# Step 12 — BillingEvent Create (External + Manual)

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingEventService — "Creating events from operational sources" and "Creating manual events"
- `Docs/structured_breakdown/04-api-layer.md` → Billing Events: `POST /billing-events` and `POST /billing-events/manual`
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 9: "Event Date Drives VAT Rate — Not Today's Date"

---

## Goal

Implement the two creation variants for `BillingEvent`:

1. **External source creation** — `POST /api/v1/billing-events` — full payload from a route management system, weighbridge, or POS integration. The caller provides most fields; the service auto-resolves any missing derived fields.
2. **Manual creation** — `POST /api/v1/billing-events/manual` — a billing clerk fills in a minimal form in the UI; the service auto-resolves accounting account, VAT rate, and legal classification from the selected product and event date.

Both variants share the same auto-resolution logic and always start with `status = IN_PROGRESS`.

---

## Backend

### 12.1 BillingEventCreateRequest (external source)

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/BillingEventCreateRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /billing-events: Request body contains the full event schema."

```java
@Data
public class BillingEventCreateRequest {

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal wasteFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal transportFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal ecoFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal weight;

    // VAT rates: optional on create — if null, service resolves by eventDate
    private BigDecimal vatRate0;
    private BigDecimal vatRate24;

    private String vehicleId;
    private String driverId;

    private Long costCenterId;           // optional — resolved from allocation rules if null
    private Long accountingAccountId;    // optional — resolved from allocation rules if null

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Customer number must be 6–9 digits")
    private String customerNumber;

    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private String comments;
    private String projectId;
    private LegalClassification legalClassification; // optional — resolved from rules if null
}
```

---

### 12.2 BillingEventManualCreateRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/BillingEventManualCreateRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /billing-events/manual: The user provides customer ID, product ID, quantity/hours, date, and optional comment. The product ID drives all financial metadata — the user cannot set accounts or VAT manually."

```java
@Data
public class BillingEventManualCreateRequest {

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal wasteFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal transportFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal ecoFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal weight;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Customer number must be 6–9 digits")
    private String customerNumber;

    private String vehicleId;
    private String driverId;
    private String locationId;
    private String municipalityId;
    private String comments;

    // Note: accountingAccountId, costCenterId, vatRate0, vatRate24, legalClassification
    // are intentionally absent — the service derives them from the product and eventDate.
}
```

---

### 12.3 BillingEventService — create methods

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventService.java`

> **Requirement source:** `03-business-logic.md` — "Receives event data and persists a BillingEvent with status IN_PROGRESS. Applies all defaults from the product and customer configuration at creation time — account code, cost centre, VAT rate (resolved by event date), legal classification."

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventService {

    private final BillingEventRepository        billingEventRepository;
    private final BillingEventStatusService      statusService;
    private final ProductRepository              productRepository;
    private final VatRateRepository              vatRateRepository;
    private final AccountingAllocationRuleRepository allocationRuleRepository;
    private final LegalClassificationService     classificationService;
    private final CostCenterRepository           costCenterRepository;
    private final AccountingAccountRepository    accountingAccountRepository;

    // -----------------------------------------------------------------------
    // VARIANT 1 — External source (full payload, integration or weighbridge)
    // POST /api/v1/billing-events
    // -----------------------------------------------------------------------
    public BillingEventResponse createFromExternalSource(BillingEventCreateRequest req) {
        Product product = loadProduct(req.getProductId());
        BillingEvent event = new BillingEvent();

        mapCommonFields(event, req.getEventDate(), product, req);

        // If caller did not supply accounting data, resolve from allocation rules
        if (req.getAccountingAccountId() == null || req.getCostCenterId() == null) {
            resolveAccountingDefaults(event, product, req.getLocationId());
        } else {
            event.setAccountingAccount(accountingAccountRepository.getReferenceById(req.getAccountingAccountId()));
            event.setCostCenter(costCenterRepository.getReferenceById(req.getCostCenterId()));
        }

        // If caller did not supply VAT, resolve by event date (NEVER by today)
        if (req.getVatRate0() == null || req.getVatRate24() == null) {
            resolveVatRates(event, req.getEventDate(), product);
        } else {
            event.setVatRate0(req.getVatRate0());
            event.setVatRate24(req.getVatRate24());
        }

        // If caller did not supply classification, derive from rules
        LegalClassification classification = req.getLegalClassification() != null
            ? req.getLegalClassification()
            : classificationService.classify(req.getCustomerNumber(), product, req.getMunicipalityId());
        event.setLegalClassification(classification);

        event.setStatus(BillingEventStatus.IN_PROGRESS);
        event.setOrigin("INTEGRATION");

        return toResponse(billingEventRepository.save(event));
    }

    // -----------------------------------------------------------------------
    // VARIANT 2 — Manual creation by billing clerk
    // POST /api/v1/billing-events/manual
    // -----------------------------------------------------------------------
    public BillingEventResponse createManual(BillingEventManualCreateRequest req, String currentUser) {
        Product product = loadProduct(req.getProductId());
        BillingEvent event = new BillingEvent();

        // Prices come from the user's form input
        event.setEventDate(req.getEventDate());
        event.setProduct(product);
        event.setWasteFeePrice(req.getWasteFeePrice());
        event.setTransportFeePrice(req.getTransportFeePrice());
        event.setEcoFeePrice(req.getEcoFeePrice());
        event.setQuantity(req.getQuantity());
        event.setWeight(req.getWeight());
        event.setCustomerNumber(req.getCustomerNumber());
        event.setVehicleId(req.getVehicleId());
        event.setDriverId(req.getDriverId());
        event.setLocationId(req.getLocationId());
        event.setMunicipalityId(req.getMunicipalityId());
        event.setComments(req.getComments());

        // All financial metadata is derived from the product — user cannot override (PD-283)
        resolveAccountingDefaults(event, product, req.getLocationId());
        resolveVatRates(event, req.getEventDate(), product);

        LegalClassification classification =
            classificationService.classify(req.getCustomerNumber(), product, req.getMunicipalityId());
        event.setLegalClassification(classification);

        event.setStatus(BillingEventStatus.IN_PROGRESS);
        event.setOrigin("MANUAL");

        return toResponse(billingEventRepository.save(event));
    }

    // -----------------------------------------------------------------------
    // SHARED HELPERS
    // -----------------------------------------------------------------------

    /**
     * Resolves VAT rates by event date.
     * CRITICAL: must use eventDate, NOT LocalDate.now().
     * See 06-cross-cutting.md Rule 9.
     */
    private void resolveVatRates(BillingEvent event, LocalDate eventDate, Product product) {
        List<VatRate> rates = vatRateRepository.findByEventDate(eventDate);
        // Rates are stored on the event as percentages (e.g. 24.00, 0.00)
        rates.stream()
            .filter(r -> r.getRate().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .ifPresent(r -> event.setVatRate0(r.getRate()));
        rates.stream()
            .filter(r -> r.getRate().compareTo(BigDecimal.ZERO) > 0)
            .findFirst()
            .ifPresent(r -> event.setVatRate24(r.getRate()));
    }

    /**
     * Resolves accounting account and cost center from allocation rules.
     * Most specific rule wins (product + region beats product-only).
     * See 02-data-layer.md — AccountingAllocationRuleRepository.
     */
    private void resolveAccountingDefaults(BillingEvent event, Product product, String locationId) {
        allocationRuleRepository
            .findBestMatchForProduct(product.getId(), locationId)
            .ifPresent(rule -> {
                event.setAccountingAccount(rule.getAccountingAccount());
                event.setCostCenter(rule.getCostCenter());
            });
    }

    private void mapCommonFields(BillingEvent event, LocalDate eventDate, Product product,
                                  BillingEventCreateRequest req) {
        event.setEventDate(eventDate);
        event.setProduct(product);
        event.setWasteFeePrice(req.getWasteFeePrice());
        event.setTransportFeePrice(req.getTransportFeePrice());
        event.setEcoFeePrice(req.getEcoFeePrice());
        event.setQuantity(req.getQuantity());
        event.setWeight(req.getWeight());
        event.setCustomerNumber(req.getCustomerNumber());
        event.setVehicleId(req.getVehicleId());
        event.setDriverId(req.getDriverId());
        event.setContractor(req.getContractor());
        event.setLocationId(req.getLocationId());
        event.setMunicipalityId(req.getMunicipalityId());
        event.setSharedServiceGroupId(req.getSharedServiceGroupId());
        event.setSharedServiceGroupPercentage(req.getSharedServiceGroupPercentage());
        event.setComments(req.getComments());
        event.setProjectId(req.getProjectId());
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
    }

    private BillingEventResponse toResponse(BillingEvent event) {
        // ... map entity to DTO
    }
}
```

---

### 12.4 BillingEventController — create endpoints

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventController.java`

```java
@RestController
@RequestMapping("/api/v1/billing-events")
@RequiredArgsConstructor
public class BillingEventController {

    private final BillingEventService billingEventService;

    /**
     * POST /api/v1/billing-events
     * External source creation — integration, weighbridge, POS
     * Role: SYSTEM (or INVOICING_USER for manual API calls)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse create(
        @Valid @RequestBody BillingEventCreateRequest request
    ) {
        return billingEventService.createFromExternalSource(request);
    }

    /**
     * POST /api/v1/billing-events/manual
     * Manual creation by billing clerk
     * Role: INVOICING_USER
     */
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse createManual(
        @Valid @RequestBody BillingEventManualCreateRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        return billingEventService.createManual(request, currentUser);
    }

    /**
     * GET /api/v1/billing-events
     * Filtered list — see BillingEventsPage.jsx for query params
     * Role: INVOICING_USER
     */
    @GetMapping
    public Page<BillingEventResponse> list(
        @RequestParam(required = false) String customerNumber,
        @RequestParam(required = false) BillingEventStatus status,
        @RequestParam(required = false) String municipalityId,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Boolean excluded,
        @RequestParam(required = false) Boolean requiresReview,
        Pageable pageable
    ) {
        return billingEventService.findFiltered(
            customerNumber, status, municipalityId, dateFrom, dateTo,
            productId, excluded, requiresReview, pageable
        );
    }

    /**
     * GET /api/v1/billing-events/{id}
     * Full event details
     * Role: INVOICING_USER
     */
    @GetMapping("/{id}")
    public BillingEventDetailResponse getById(@PathVariable Long id) {
        return billingEventService.findById(id);
    }
}
```

**Request/Response example — external source:**

`POST /api/v1/billing-events`
```json
{
  "eventDate": "2024-01-15",
  "productId": 7,
  "wasteFeePrice": 12.50,
  "transportFeePrice": 4.80,
  "ecoFeePrice": 0.50,
  "quantity": 1.0,
  "weight": 0.240,
  "customerNumber": "123456",
  "vehicleId": "ABC-123",
  "driverId": "driver-88",
  "locationId": "LOC-042",
  "municipalityId": "MUN-01",
  "comments": "Container at gate 3"
}
```

Response `201 Created`:
```json
{
  "id": 1001,
  "eventDate": "2024-01-15",
  "product": { "id": 7, "name": "Bio-waste emptying" },
  "wasteFeePrice": 12.50,
  "transportFeePrice": 4.80,
  "ecoFeePrice": 0.50,
  "quantity": 1.0,
  "weight": 0.240,
  "vatRate0": 0.00,
  "vatRate24": 24.00,
  "customerNumber": "123456",
  "vehicleId": "ABC-123",
  "driverId": "driver-88",
  "locationId": "LOC-042",
  "municipalityId": "MUN-01",
  "legalClassification": "PUBLIC_LAW",
  "accountingAccount": { "id": 3, "code": "4100" },
  "costCenter": { "id": 5, "code": "CC-WASTE-01" },
  "status": "IN_PROGRESS",
  "excluded": false,
  "origin": "INTEGRATION",
  "createdAt": "2024-01-15T08:00:00Z"
}
```

**Request/Response example — manual creation:**

`POST /api/v1/billing-events/manual`
```json
{
  "eventDate": "2024-01-15",
  "productId": 12,
  "wasteFeePrice": 85.00,
  "transportFeePrice": 0.00,
  "ecoFeePrice": 0.00,
  "quantity": 1.0,
  "weight": 0.0,
  "customerNumber": "987654",
  "comments": "Expert consultation, 2 hours"
}
```

Response `201 Created`:
```json
{
  "id": 1002,
  "eventDate": "2024-01-15",
  "product": { "id": 12, "name": "Expert work" },
  "wasteFeePrice": 85.00,
  "transportFeePrice": 0.00,
  "ecoFeePrice": 0.00,
  "quantity": 1.0,
  "weight": 0.0,
  "vatRate0": 0.00,
  "vatRate24": 24.00,
  "customerNumber": "987654",
  "legalClassification": "PRIVATE_LAW",
  "accountingAccount": { "id": 8, "code": "4500" },
  "costCenter": { "id": 11, "code": "CC-EXPERT" },
  "status": "IN_PROGRESS",
  "excluded": false,
  "origin": "MANUAL",
  "createdAt": "2024-01-15T09:30:00Z"
}
```

---

### 12.5 BillingEventResponse DTO

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/BillingEventResponse.java`

```java
@Data
@Builder
public class BillingEventResponse {
    private Long id;
    private LocalDate eventDate;
    private ProductSummaryDto product;
    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal vatRate0;
    private BigDecimal vatRate24;
    private String vehicleId;
    private String driverId;
    private String customerNumber;
    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private String comments;
    private BillingEventStatus status;
    private boolean excluded;
    private String exclusionReason;
    private boolean nonBillable;
    private LegalClassification legalClassification;
    private boolean officeReviewRequired;
    private String projectId;
    private String origin;
    private AccountSummaryDto accountingAccount;
    private CostCenterSummaryDto costCenter;
    private Instant createdAt;
    private String createdBy;
}
```

---

## Frontend

### 12.6 Create BillingEvent Form (Manual)

**File:** `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx`

This page is for manual creation only (`POST /manual`). Integration events arrive via the API and do not have a UI form.

**Form sections:**

**Section 1 — Event Details**
- `eventDate` — date picker (required)
- `productId` — dropdown, loads from `GET /api/v1/products` (required)

**Section 2 — Auto-resolved fields (read-only, shown after product + date are selected)**

After the user selects a product and date, the UI calls `GET /api/v1/vat-rates?eventDate=YYYY-MM-DD` and `GET /api/v1/products/{id}` to pre-fetch and display:
- VAT Rate (read-only badge showing the resolved rate for the selected date)
- Accounting Account (read-only label)
- Cost Center (read-only label)
- Legal Classification (read-only badge)

This preview gives the user confidence the system is using the correct values before submitting.

**Section 3 — Pricing**
- `wasteFeePrice` — number input (required, min 0.00)
- `transportFeePrice` — number input (required, min 0.00)
- `ecoFeePrice` — number input (required, min 0.00)

**Section 4 — Quantity**
- `quantity` — number input (required, min 0.00)
- `weight` — number input (required, min 0.00)

**Section 5 — Customer & Location**
- `customerNumber` — text input, 6-9 digits (required)
- `vehicleId` — text input (optional)
- `driverId` — text input (optional)
- `locationId` — text input (optional)
- `municipalityId` — text input (optional)

**Section 6 — Notes**
- `comments` — textarea (optional)

**Submit behavior:** POST to `/api/v1/billing-events/manual`. On success, redirect to the event detail page for the new event ID.

---

### 12.7 Auto-resolve VAT preview hook

**File:** `invoicing-fe/src/hooks/useResolvedVatRate.js`

```js
import { useState, useEffect } from 'react'
import axios from '../api/axios'

export function useResolvedVatRate(productId, eventDate) {
  const [vatRates, setVatRates] = useState(null)

  useEffect(() => {
    if (!productId || !eventDate) return
    axios.get('/api/v1/vat-rates', { params: { eventDate } })
      .then(res => setVatRates(res.data))
      .catch(() => setVatRates(null))
  }, [productId, eventDate])

  return vatRates
}
```

This hook is used in `CreateBillingEventPage.jsx` to display the auto-resolved VAT rate inline on the form as the user fills in product and date.

---

## Verification Checklist

1. `POST /api/v1/billing-events` with full payload — returns 201, event has `status: "IN_PROGRESS"`, `origin: "INTEGRATION"`.
2. `POST /api/v1/billing-events` without `vatRate0`/`vatRate24` — verify the service resolves them from the VatRate table using `eventDate` (not today's date).
3. `POST /api/v1/billing-events` with an `eventDate` from before the current VAT rate was valid — verify the older rate is applied.
4. `POST /api/v1/billing-events/manual` without `accountingAccountId` — verify the service auto-resolves from the product's allocation rule.
5. `POST /api/v1/billing-events/manual` — verify the response does NOT include a field for the user to have set `legalClassification` — it must be auto-resolved.
6. `POST /api/v1/billing-events` with `customerNumber: "12345"` (5 digits) — verify 400 validation error with "Customer number must be 6–9 digits".
7. `POST /api/v1/billing-events` with `productId: 99999` (non-existent) — verify 404 response.
8. Frontend: open Create BillingEvent form, select a product and date — auto-resolved VAT rate badge appears in the form.
9. Frontend: submit the manual form with all required fields — redirects to the detail page for the new event.
10. Frontend: submit with an empty `wasteFeePrice` — client-side validation shows an error before the request is sent.

---

## File Checklist

### Backend
- [ ] `billingevent/dto/BillingEventCreateRequest.java`
- [ ] `billingevent/dto/BillingEventManualCreateRequest.java`
- [ ] `billingevent/dto/BillingEventResponse.java`
- [ ] `billingevent/BillingEventService.java` — `createFromExternalSource()` and `createManual()` methods
- [ ] `billingevent/BillingEventController.java` — `POST /`, `POST /manual`, `GET /`, `GET /{id}` endpoints

### Frontend
- [ ] `src/pages/billing/CreateBillingEventPage.jsx`
- [ ] `src/hooks/useResolvedVatRate.js`
- [ ] `src/api/billingEvents.js` — add `createManualBillingEvent`
- [ ] `src/App.jsx` — add route for `/billing-events/new`
