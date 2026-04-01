# Step 06 — Customer Billing Profile

## References to Original Requirements

- `Docs/structured_breakdown/01-domain-model.md` → Section: "Customer / BillingProfile" — CustomerID (6–9 digits), delivery method enum, billing address with two-language variants, business ID, language code, invoicingMode GROSS/NET
- `Docs/structured_breakdown/04-api-layer.md` → Section: "Billing Profiles and Customer Data" — `GET /customers/{id}/billing-profile`, `PUT /customers/{id}/billing-profile`
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 5: "Data Locking During Billing Runs" — any write to billing address or billing groups must check the ActiveRunLock and return 423 if locked; Rule 2: Roles — `INVOICING_USER` can read and update billing profiles
- `Docs/structured_breakdown/03-business-logic.md` → Section: "BillingProfileSyncService" — every save triggers a sync push to the external invoicing system
- `Docs/structured_breakdown/02-data-layer.md` → Section: "CustomerBillingProfileRepository" — `findCustomersWithMissingBillingData`, `findByDeliveryMethod`

---

## Goal

Implement the `BillingProfile` embedded value object on the `Customer` entity, with all the mandatory fields required before any invoice can be generated for a customer. Every `Customer` record must have a complete `BillingProfile` before it appears in an invoice run — missing profile data blocks generation for that customer (see `BillingProfileValidationService` in Step 09).

This step provides:
- The `BillingProfile` embedded object and its `BillingAddress` value type
- The `DeliveryMethod` and `InvoicingMode` enums
- The `CustomerType` enum used both here and in `ClassificationRule` (Step 08)
- A `CustomerBillingProfileRepository` with the pre-flight queries required by `InvoiceRunService`
- A `BillingProfileService` that enforces the run-lock rule before every write
- REST endpoints at `/api/v1/customers/{id}/billing-profile` (GET, PUT)
- A React page for viewing and editing the billing profile

---

## Backend

### 6.1 CustomerType Enum

**File:** `invoicing/src/main/java/com/example/invoicing/customer/CustomerType.java`

> **Requirement source:** `01-domain-model.md` — ClassificationRule conditions reference customer type; `03-business-logic.md` — BillingSurchargeService differentiates surcharge amounts by customer type

```java
public enum CustomerType {
    PRIVATE,       // private household
    BUSINESS,      // business/company
    MUNICIPALITY,  // municipal organisation
    AUTHORITY      // government authority
}
```

---

### 6.2 DeliveryMethod Enum

**File:** `invoicing/src/main/java/com/example/invoicing/customer/DeliveryMethod.java`

> **Requirement source:** `01-domain-model.md` — "delivery method (E_INVOICE / EMAIL / PAPER / DIRECT_PAYMENT)"
> `04-api-layer.md` — billing surcharge auto-assigned based on this value; `04-api-layer.md` — `findByDeliveryMethod` used for surcharge application during runs

```java
public enum DeliveryMethod {
    E_INVOICE,       // electronic invoice routed through operator
    EMAIL,           // invoice sent as PDF email attachment
    PAPER,           // printed and posted invoice
    DIRECT_PAYMENT   // direct debit / auto-payment, no invoice delivery
}
```

---

### 6.3 InvoicingMode Enum

**File:** `invoicing/src/main/java/com/example/invoicing/customer/InvoicingMode.java`

> **Requirement source:** `01-domain-model.md` — "invoicingMode: GROSS or NET"; `04-api-layer.md` PD-301: gross shows total-with-VAT only; net shows net + VAT breakdown

```java
public enum InvoicingMode {
    GROSS,  // invoice shows total including VAT; VAT % displayed but not broken out as a line
    NET     // invoice shows net amount, then VAT amount, then gross total
}
```

---

### 6.4 BillingAddress Embeddable

**File:** `invoicing/src/main/java/com/example/invoicing/customer/BillingAddress.java`

> **Requirement source:** `01-domain-model.md` — "billing address stored in two language variants" (PD-308: Finnish, Swedish, English)

```java
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingAddress {

    // Primary language variant (typically Finnish)
    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "city")
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;     // ISO 3166-1 alpha-2, e.g. "FI"

    // Second language variant (typically Swedish)
    @Column(name = "street_address_alt")
    private String streetAddressAlt;

    @Column(name = "city_alt")
    private String cityAlt;

    @Column(name = "country_code_alt", length = 2)
    private String countryCodeAlt;
}
```

---

### 6.5 BillingProfile Embeddable

**File:** `invoicing/src/main/java/com/example/invoicing/customer/BillingProfile.java`

> **Requirement source:** `01-domain-model.md` — "CustomerID (6–9 digits), delivery method, billing address, business ID, languageCode, invoicingMode"

```java
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingProfile {

    /**
     * 6–9 digit numeric sequence identifying the customer in the invoicing system.
     * Validated by BillingProfileValidationService before any invoice run (PD-298).
     */
    @Column(name = "customer_id_number", length = 9)
    private String customerIdNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", length = 20)
    private DeliveryMethod deliveryMethod;

    @Embedded
    private BillingAddress billingAddress;

    /**
     * Finnish Business ID (Y-tunnus) or equivalent. Mandatory for e-invoicing.
     * Format example: "1234567-8"
     */
    @Column(name = "business_id", length = 20)
    private String businessId;

    /**
     * BCP 47 language tag: "fi", "sv", or "en".
     * Drives product name language selection in FINVOICE (PD-308).
     */
    @Column(name = "language_code", length = 5)
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoicing_mode", length = 10)
    private InvoicingMode invoicingMode;
}
```

---

### 6.6 Customer Entity

**File:** `invoicing/src/main/java/com/example/invoicing/customer/Customer.java`

> **Requirement source:** `01-domain-model.md` — BillingProfile is an embedded value object, not a separate table

```java
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseAuditEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType;

    @Embedded
    private BillingProfile billingProfile;

    // EInvoiceAddress is a separate child entity — see Step 07
}
```

---

### 6.7 CustomerBillingProfileRepository

**File:** `invoicing/src/main/java/com/example/invoicing/customer/CustomerBillingProfileRepository.java`

> **Requirement source:** `02-data-layer.md` — "Find customers with missing billing data" (PD-298 pre-flight); "Find customers by delivery method" (PD-294 surcharge assignment)

```java
public interface CustomerBillingProfileRepository extends JpaRepository<Customer, Long> {

    /**
     * Pre-flight check before a billing run starts.
     * Returns all customers missing any of the three mandatory billing fields.
     * Source: 02-data-layer.md — CustomerBillingProfileRepository / findCustomersWithMissingBillingData
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.billingProfile.businessId IS NULL
           OR c.billingProfile.billingAddress.streetAddress IS NULL
           OR c.billingProfile.deliveryMethod IS NULL
        """)
    List<Customer> findCustomersWithMissingBillingData();

    /**
     * Used by BillingSurchargeService to determine which customers receive
     * a surcharge for a given delivery method (e.g. PAPER invoice surcharge).
     * Source: 02-data-layer.md — CustomerBillingProfileRepository / findByDeliveryMethod
     */
    List<Customer> findByBillingProfile_DeliveryMethod(DeliveryMethod deliveryMethod);
}
```

---

### 6.8 ActiveRunLockRepository

**File:** `invoicing/src/main/java/com/example/invoicing/invoicerun/ActiveRunLockRepository.java`

> **Requirement source:** `02-data-layer.md` — "Check if a customer is locked" — must be fast, index on customerId essential; `06-cross-cutting.md` — Rule 5: Data Locking During Billing Runs

```java
@Entity
@Table(name = "active_run_locks",
       indexes = @Index(name = "idx_run_lock_customer", columnList = "customer_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRunLock extends BaseAuditEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "run_id", nullable = false)
    private Long runId;
}
```

```java
public interface ActiveRunLockRepository extends JpaRepository<ActiveRunLock, Long> {

    boolean existsByCustomerId(Long customerId);

    void deleteByRunId(Long runId);
}
```

---

### 6.9 BillingProfileService

**File:** `invoicing/src/main/java/com/example/invoicing/customer/BillingProfileService.java`

> **Requirement source:** `03-business-logic.md` — BillingProfileSyncService triggered on every save; `06-cross-cutting.md` — Rule 5: 423 Locked if customer is in an active run

Methods:

| Method signature | Description |
|---|---|
| `getBillingProfile(Long customerId)` | Loads customer, returns profile DTO |
| `updateBillingProfile(Long customerId, BillingProfileRequest)` | Checks run lock, applies update, publishes BillingAddressChangedEvent |
| `isLockedByActiveRun(Long customerId)` | Delegates to ActiveRunLockRepository |

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BillingProfileService {

    private final CustomerBillingProfileRepository customerRepo;
    private final ActiveRunLockRepository runLockRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public BillingProfileResponse getBillingProfile(Long customerId) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        return BillingProfileResponse.from(customer);
    }

    public BillingProfileResponse updateBillingProfile(Long customerId,
                                                        BillingProfileRequest request) {
        // 06-cross-cutting.md Rule 5: check run lock before any billing profile write
        if (runLockRepo.existsByCustomerId(customerId)) {
            throw new BillingRunLockException(
                "Invoice processing in progress. Address changes cannot be made during this time.");
        }

        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        customer.setBillingProfile(request.toBillingProfile());
        Customer saved = customerRepo.save(customer);

        // 03-business-logic.md BillingProfileSyncService: trigger external sync
        eventPublisher.publishEvent(new BillingAddressChangedEvent(this, customerId));

        return BillingProfileResponse.from(saved);
    }
}
```

**BillingRunLockException** returns HTTP 423:

**File:** `invoicing/src/main/java/com/example/invoicing/common/exception/BillingRunLockException.java`

```java
@ResponseStatus(HttpStatus.LOCKED)
public class BillingRunLockException extends RuntimeException {
    public BillingRunLockException(String message) {
        super(message);
    }
}
```

---

### 6.10 BillingProfileController

**File:** `invoicing/src/main/java/com/example/invoicing/customer/BillingProfileController.java`

> **Requirement source:** `04-api-layer.md` — `GET /customers/{id}/billing-profile`, `PUT /customers/{id}/billing-profile`

Base path: `/api/v1/customers/{customerId}/billing-profile`

| Method | Path | Role required | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/customers/{id}/billing-profile` | INVOICING_USER | Return current billing profile |
| PUT | `/api/v1/customers/{id}/billing-profile` | INVOICING_USER | Replace entire billing profile; triggers sync |

**GET response body:**
```json
{
  "customerId": 42,
  "customerName": "Jari Virtanen",
  "customerType": "PRIVATE",
  "billingProfile": {
    "customerIdNumber": "123456",
    "deliveryMethod": "E_INVOICE",
    "billingAddress": {
      "streetAddress": "Mannerheimintie 1",
      "postalCode": "00100",
      "city": "Helsinki",
      "countryCode": "FI",
      "streetAddressAlt": null,
      "cityAlt": null,
      "countryCodeAlt": null
    },
    "businessId": "1234567-8",
    "languageCode": "fi",
    "invoicingMode": "NET"
  },
  "lastModifiedBy": "office.user@example.fi",
  "lastModifiedAt": "2025-03-15T09:22:00Z"
}
```

**PUT request body:**
```json
{
  "customerIdNumber": "123456",
  "deliveryMethod": "E_INVOICE",
  "billingAddress": {
    "streetAddress": "Mannerheimintie 1",
    "postalCode": "00100",
    "city": "Helsinki",
    "countryCode": "FI",
    "streetAddressAlt": "Mannerheimsgatan 1",
    "cityAlt": "Helsingfors",
    "countryCodeAlt": "FI"
  },
  "businessId": "1234567-8",
  "languageCode": "fi",
  "invoicingMode": "NET"
}
```

**PUT success response (200):** same shape as GET response, updated values.

**PUT failure — customer in active billing run (423):**
```json
{
  "status": 423,
  "error": "Locked",
  "message": "Invoice processing in progress. Address changes cannot be made during this time."
}
```

```java
@RestController
@RequestMapping("/api/v1/customers/{customerId}/billing-profile")
@RequiredArgsConstructor
public class BillingProfileController {

    private final BillingProfileService billingProfileService;

    @GetMapping
    public ResponseEntity<BillingProfileResponse> getProfile(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(billingProfileService.getBillingProfile(customerId));
    }

    @PutMapping
    public ResponseEntity<BillingProfileResponse> updateProfile(
            @PathVariable Long customerId,
            @RequestBody @Valid BillingProfileRequest request) {
        return ResponseEntity.ok(
            billingProfileService.updateBillingProfile(customerId, request));
    }
}
```

---

### 6.11 DTOs

**File:** `invoicing/src/main/java/com/example/invoicing/customer/dto/BillingProfileRequest.java`

```java
@Data
public class BillingProfileRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "customerIdNumber must be 6–9 digits")
    private String customerIdNumber;

    @NotNull
    private DeliveryMethod deliveryMethod;

    @NotNull
    @Valid
    private BillingAddressRequest billingAddress;

    private String businessId;

    @NotBlank
    private String languageCode;

    @NotNull
    private InvoicingMode invoicingMode;

    public BillingProfile toBillingProfile() {
        BillingAddress addr = new BillingAddress(
            billingAddress.getStreetAddress(),
            billingAddress.getPostalCode(),
            billingAddress.getCity(),
            billingAddress.getCountryCode(),
            billingAddress.getStreetAddressAlt(),
            billingAddress.getCityAlt(),
            billingAddress.getCountryCodeAlt()
        );
        return new BillingProfile(customerIdNumber, deliveryMethod,
                                   addr, businessId, languageCode, invoicingMode);
    }
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/customer/dto/BillingAddressRequest.java`

```java
@Data
public class BillingAddressRequest {
    @NotBlank private String streetAddress;
    @NotBlank private String postalCode;
    @NotBlank private String city;
    @NotBlank @Size(min = 2, max = 2) private String countryCode;
    private String streetAddressAlt;
    private String cityAlt;
    private String countryCodeAlt;
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/customer/dto/BillingProfileResponse.java`

```java
@Data
@Builder
public class BillingProfileResponse {
    private Long customerId;
    private String customerName;
    private CustomerType customerType;
    private BillingProfileDto billingProfile;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static BillingProfileResponse from(Customer c) {
        return BillingProfileResponse.builder()
            .customerId(c.getId())
            .customerName(c.getName())
            .customerType(c.getCustomerType())
            .billingProfile(BillingProfileDto.from(c.getBillingProfile()))
            .lastModifiedBy(c.getLastModifiedBy())
            .lastModifiedAt(c.getLastModifiedAt())
            .build();
    }
}
```

---

## Frontend

### 6.12 billingProfile.js — API Client

**File:** `invoicing-fe/src/api/billingProfile.js`

```js
import axios from './axios'

export const getBillingProfile = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/billing-profile`)

export const updateBillingProfile = (customerId, data) =>
  axios.put(`/api/v1/customers/${customerId}/billing-profile`, data)
```

---

### 6.13 BillingProfilePage.jsx

**File:** `invoicing-fe/src/pages/customers/BillingProfilePage.jsx`

**Purpose:** View and edit the billing profile for a single customer. Reached via the route `/customers/:customerId/billing-profile`.

**Component structure:**

```
BillingProfilePage
├── CustomerHeader          — name, type badge, customerId
├── BillingProfileForm      — all profile fields (see below)
│   ├── DeliveryMethodSelect
│   ├── InvoicingModeSelect
│   ├── LanguageCodeSelect  — fi / sv / en
│   ├── BillingAddressSection
│   │   ├── Primary address fields (streetAddress, postalCode, city, countryCode)
│   │   └── Alt language address fields (streetAddressAlt, cityAlt, countryCodeAlt)
│   └── BusinessIdInput
└── ActionBar
    ├── SaveButton
    └── ValidateButton      — triggers Step 09 validation (wired in Step 09)
```

**Key behaviours:**

- On mount: calls `getBillingProfile(customerId)`, populates form state
- On save: calls `updateBillingProfile(customerId, formData)`
- If the PUT returns **423**: display an alert banner: _"Invoice processing in progress. Address changes cannot be made during this time."_ The save button is disabled while the alert is visible
- `customerIdNumber` field validates 6–9 digits on blur before submission
- `deliveryMethod` = `E_INVOICE` → the EInvoice Address section (Step 07) is shown below the form; for other delivery methods it is hidden
- All fields show their `lastModifiedBy` / `lastModifiedAt` timestamp at the bottom of the form

**API calls:**
```js
// Load on mount
const { data } = await getBillingProfile(customerId)

// Save
try {
  const { data: updated } = await updateBillingProfile(customerId, formState)
  setProfile(updated)
  showSuccess('Billing profile saved')
} catch (err) {
  if (err.response?.status === 423) {
    showLockBanner(err.response.data.message)
  } else {
    showError('Failed to save billing profile')
  }
}
```

---

### 6.14 Route Registration

**File:** `invoicing-fe/src/App.jsx` — add:

```jsx
<Route path="/customers/:customerId/billing-profile" element={<BillingProfilePage />} />
```

---

## Verification Checklist

1. `POST /api/v1/customers` (or seed data) — create a customer record so there is an ID to work with
2. `GET /api/v1/customers/1/billing-profile` — returns 200 with `billingProfile` object (all fields may be null initially)
3. `PUT /api/v1/customers/1/billing-profile` with a valid body — returns 200 with updated values; verify `lastModifiedAt` is updated in DB
4. `PUT /api/v1/customers/1/billing-profile` with `customerIdNumber: "12345"` (5 digits) — returns 400 Bad Request; `customerIdNumber` must be 6–9 digits
5. `PUT /api/v1/customers/1/billing-profile` with `customerIdNumber: "1234567890"` (10 digits) — returns 400 Bad Request
6. Manually insert a row into `active_run_locks` for customer ID 1; then call `PUT /api/v1/customers/1/billing-profile` — returns 423 with the message "Invoice processing in progress. Address changes cannot be made during this time."
7. Delete the lock row; retry the PUT — returns 200 again
8. `GET /api/v1/customers/1/billing-profile` after the PUT — returns the values written in step 3
9. Open the FE at `/customers/1/billing-profile` — form is pre-populated from the GET response
10. Change `deliveryMethod` to `E_INVOICE` in the dropdown — EInvoice Address section appears below the form (Step 07 component renders)
11. Change `deliveryMethod` to `PAPER` — EInvoice Address section disappears
12. Submit the form with a valid payload — success toast appears and form fields reflect the saved values
13. Simulate a 423 response (mock or inject a lock) — lock banner displays with the exact message from PD-270; save button is disabled
14. `GET /api/v1/customers/1/billing-profile?deliveryMethod=PAPER` (if supported) — or call `CustomerBillingProfileRepository.findByBillingProfile_DeliveryMethod(PAPER)` from a test — returns expected customers
15. `CustomerBillingProfileRepository.findCustomersWithMissingBillingData()` — customers with null `businessId`, `deliveryMethod`, or `streetAddress` are included in results

---

## File Checklist

### Backend
- [ ] `customer/CustomerType.java`
- [ ] `customer/DeliveryMethod.java`
- [ ] `customer/InvoicingMode.java`
- [ ] `customer/BillingAddress.java`
- [ ] `customer/BillingProfile.java`
- [ ] `customer/Customer.java`
- [ ] `customer/CustomerBillingProfileRepository.java`
- [ ] `customer/BillingProfileService.java`
- [ ] `customer/BillingProfileController.java`
- [ ] `customer/dto/BillingProfileRequest.java`
- [ ] `customer/dto/BillingAddressRequest.java`
- [ ] `customer/dto/BillingProfileResponse.java`
- [ ] `customer/dto/BillingProfileDto.java`
- [ ] `customer/event/BillingAddressChangedEvent.java`
- [ ] `invoicerun/ActiveRunLock.java`
- [ ] `invoicerun/ActiveRunLockRepository.java`
- [ ] `common/exception/BillingRunLockException.java`

### Frontend
- [ ] `src/api/billingProfile.js`
- [ ] `src/pages/customers/BillingProfilePage.jsx`
- [ ] `src/pages/customers/components/BillingProfileForm.jsx`
- [ ] `src/pages/customers/components/BillingAddressSection.jsx`
- [ ] `src/App.jsx` — add route `/customers/:customerId/billing-profile`
