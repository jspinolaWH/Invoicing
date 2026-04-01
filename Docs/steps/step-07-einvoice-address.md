# Step 07 — EInvoice Address

## References to Original Requirements

- `Docs/structured_breakdown/01-domain-model.md` → Section: "Customer / BillingProfile" — "The BillingProfile has an EInvoiceAddress sub-object with a manuallyLocked boolean. When locked, the integration (PD-107) will not overwrite it."
- `Docs/structured_breakdown/04-api-layer.md` → Section: "Billing Profiles and Customer Data" — `GET /customers/{id}/einvoice-address` (implied by the `PUT` endpoint and the billing profile GET), `PUT /customers/{id}/einvoice-address` with `lock` flag
- `Docs/structured_breakdown/05-integration-layer.md` → Section: "Invoice Operator (Ropo, Maventa, or similar)" — "Before updating, the lock flag is checked… If `manuallyLocked = true`, the integration message is logged but the address is NOT updated."

---

## Goal

Implement the `EInvoiceAddress` entity as a separate child record linked to a `Customer`. An e-invoice address identifies where Finnish electronic invoices are routed — it is the OVT identifier or IBAN+BIC string that the invoice operator uses to deliver FINVOICE files to the customer's bank.

There are two sources of truth for this address:
1. **The invoice operator** (Ropo, Maventa): pushes address changes daily via integration (PD-107)
2. **A manual edit by an office user** (PD-282): the user sets the address and can simultaneously lock it to prevent operator overwrites

The `manuallyLocked` flag is the arbitration mechanism. When true, the `EInvoiceIntegrationService` skips the update and logs the skipped message for manual review. When false, the operator's daily batch update wins.

This step builds:
- The `EInvoiceAddress` entity
- The `EInvoiceAddressRepository` and `EInvoiceAddressService`
- REST endpoints at `/api/v1/customers/{id}/einvoice-address` (GET, PUT with `lock` flag)
- A React section inside `BillingProfilePage` that shows and edits the e-invoice address with a lock toggle, visible only when `deliveryMethod = E_INVOICE`

---

## Backend

### 7.1 EInvoiceAddress Entity

**File:** `invoicing/src/main/java/com/example/invoicing/customer/EInvoiceAddress.java`

> **Requirement source:** `01-domain-model.md` — "e-invoice address string, manuallyLocked boolean flag"

```java
@Entity
@Table(name = "einvoice_addresses",
       indexes = @Index(name = "idx_einvoice_customer", columnList = "customer_id", unique = true))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceAddress extends BaseAuditEntity {

    /**
     * FK to the owning Customer. One-to-one: each customer has at most one e-invoice address.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    /**
     * The routable e-invoice address string.
     * Finnish format examples:
     *   OVT format : "003712345678"  (12 chars: "0037" + 8-digit Y-tunnus base)
     *   IBAN format : "FI2112345600000785"
     *   Network operator address as agreed with operator
     */
    @Column(name = "address", nullable = false, length = 35)
    private String address;

    /**
     * Operator code — the BIC or routing code of the invoice operator/bank routing
     * the e-invoice (e.g. "NDEAFIHH" for Nordea, "POPFFI22" for POP Pankki).
     * Required alongside the address for FINVOICE routing.
     */
    @Column(name = "operator_code", length = 20)
    private String operatorCode;

    /**
     * When true, the EInvoiceIntegrationService will NOT overwrite this address
     * with data received from the operator (PD-282).
     * Set to true only by explicit user action — never by the integration.
     */
    @Column(name = "manually_locked", nullable = false)
    private boolean manuallyLocked = false;

    /**
     * Free-text note explaining why the address was manually locked.
     * Optional but recommended for audit clarity.
     */
    @Column(name = "lock_reason", length = 500)
    private String lockReason;
}
```

---

### 7.2 EInvoiceAddressRepository

**File:** `invoicing/src/main/java/com/example/invoicing/customer/EInvoiceAddressRepository.java`

> **Requirement source:** `05-integration-layer.md` — integration must check manuallyLocked before updating; `01-domain-model.md` — each customer has one EInvoiceAddress

```java
public interface EInvoiceAddressRepository extends JpaRepository<EInvoiceAddress, Long> {

    /**
     * Main lookup: find the e-invoice address for a customer.
     * Returns empty Optional if the customer has no e-invoice address on file.
     */
    Optional<EInvoiceAddress> findByCustomer_Id(Long customerId);

    /**
     * Used by EInvoiceIntegrationService when processing the daily operator batch.
     * Returns only addresses where the lock flag is false — i.e. addresses the
     * integration is allowed to overwrite (05-integration-layer.md).
     */
    Optional<EInvoiceAddress> findByCustomer_IdAndManuallyLockedFalse(Long customerId);

    /**
     * Diagnostic / audit query: find all manually locked addresses for a company.
     * Useful for reviewing which customers' addresses are protected from integration updates.
     */
    @Query("SELECT e FROM EInvoiceAddress e WHERE e.manuallyLocked = true")
    List<EInvoiceAddress> findAllManuallyLocked();
}
```

---

### 7.3 EInvoiceAddressService

**File:** `invoicing/src/main/java/com/example/invoicing/customer/EInvoiceAddressService.java`

> **Requirement source:** `04-api-layer.md` — manual set/update (PD-282), lock flag; `05-integration-layer.md` — integration update path that respects the lock

Methods:

| Method signature | Description |
|---|---|
| `getAddress(Long customerId)` | Returns the current EInvoiceAddress DTO or empty response if not yet set |
| `setAddress(Long customerId, EInvoiceAddressRequest)` | Manual set — respects lock flag from request |
| `updateFromOperator(Long customerId, String address, String operatorCode)` | Integration path — skips update if `manuallyLocked = true`, logs the skip |

```java
@Service
@RequiredArgsConstructor
@Transactional
public class EInvoiceAddressService {

    private final EInvoiceAddressRepository addressRepo;
    private final CustomerBillingProfileRepository customerRepo;

    @Transactional(readOnly = true)
    public EInvoiceAddressResponse getAddress(Long customerId) {
        return addressRepo.findByCustomer_Id(customerId)
            .map(EInvoiceAddressResponse::from)
            .orElse(EInvoiceAddressResponse.empty(customerId));
    }

    /**
     * Manual update path — used by the REST controller (PD-282).
     * When request.isLock() is true, sets manuallyLocked = true and records lockReason.
     * When request.isLock() is false, clears the lock (operator updates allowed again).
     */
    public EInvoiceAddressResponse setAddress(Long customerId,
                                               EInvoiceAddressRequest request) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        EInvoiceAddress entity = addressRepo.findByCustomer_Id(customerId)
            .orElse(new EInvoiceAddress());

        entity.setCustomer(customer);
        entity.setAddress(request.getAddress());
        entity.setOperatorCode(request.getOperatorCode());
        entity.setManuallyLocked(request.isLock());
        entity.setLockReason(request.isLock() ? request.getLockReason() : null);

        return EInvoiceAddressResponse.from(addressRepo.save(entity));
    }

    /**
     * Integration update path — called by EInvoiceIntegrationService daily (PD-107).
     * If manuallyLocked = true, skips the update entirely and logs the skipped attempt.
     * Source: 05-integration-layer.md — "If manuallyLocked = true, the integration
     * message is logged but the address is NOT updated."
     */
    public void updateFromOperator(Long customerId, String address, String operatorCode) {
        // findByCustomer_IdAndManuallyLockedFalse returns empty if locked — no update
        addressRepo.findByCustomer_IdAndManuallyLockedFalse(customerId).ifPresentOrElse(
            existing -> {
                existing.setAddress(address);
                existing.setOperatorCode(operatorCode);
                addressRepo.save(existing);
            },
            () -> {
                // Address is either manually locked or doesn't exist yet.
                // Log and skip — do NOT create or overwrite a locked address.
                addressRepo.findByCustomer_Id(customerId).ifPresent(locked ->
                    // In a real implementation this writes to an operator message log
                    log.warn("Skipped operator update for customerId={}: address is manually locked",
                             customerId)
                );
            }
        );
    }
}
```

---

### 7.4 EInvoiceAddressController

**File:** `invoicing/src/main/java/com/example/invoicing/customer/EInvoiceAddressController.java`

> **Requirement source:** `04-api-layer.md` — `PUT /customers/{id}/einvoice-address` with `lock` flag; GET implied by the bilateral flow

Base path: `/api/v1/customers/{customerId}/einvoice-address`

| Method | Path | Role required | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/customers/{id}/einvoice-address` | INVOICING_USER | Return current e-invoice address and lock status |
| PUT | `/api/v1/customers/{id}/einvoice-address` | INVOICING_USER | Set or update address; optionally lock it |

**GET response body — address on file:**
```json
{
  "customerId": 42,
  "address": "003712345678",
  "operatorCode": "NDEAFIHH",
  "manuallyLocked": true,
  "lockReason": "Agreed directly with customer 2025-01-10",
  "lastModifiedBy": "anna.korhonen@kiertokapula.fi",
  "lastModifiedAt": "2025-01-10T14:30:00Z"
}
```

**GET response body — no address set yet:**
```json
{
  "customerId": 42,
  "address": null,
  "operatorCode": null,
  "manuallyLocked": false,
  "lockReason": null,
  "lastModifiedBy": null,
  "lastModifiedAt": null
}
```

**PUT request body — set address and lock it:**
```json
{
  "address": "003712345678",
  "operatorCode": "NDEAFIHH",
  "lock": true,
  "lockReason": "Manually confirmed with customer — do not overwrite via integration"
}
```

**PUT request body — set address without locking (integration may overwrite):**
```json
{
  "address": "003712345678",
  "operatorCode": "NDEAFIHH",
  "lock": false,
  "lockReason": null
}
```

**PUT request body — remove the lock (allow operator updates again):**
```json
{
  "address": "003712345678",
  "operatorCode": "NDEAFIHH",
  "lock": false,
  "lockReason": null
}
```

**PUT success response (200):** same shape as GET response.

**PUT validation failure (400):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "address must not be blank"
}
```

```java
@RestController
@RequestMapping("/api/v1/customers/{customerId}/einvoice-address")
@RequiredArgsConstructor
public class EInvoiceAddressController {

    private final EInvoiceAddressService addressService;

    @GetMapping
    public ResponseEntity<EInvoiceAddressResponse> getAddress(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(addressService.getAddress(customerId));
    }

    @PutMapping
    public ResponseEntity<EInvoiceAddressResponse> setAddress(
            @PathVariable Long customerId,
            @RequestBody @Valid EInvoiceAddressRequest request) {
        return ResponseEntity.ok(addressService.setAddress(customerId, request));
    }
}
```

---

### 7.5 DTOs

**File:** `invoicing/src/main/java/com/example/invoicing/customer/dto/EInvoiceAddressRequest.java`

```java
@Data
public class EInvoiceAddressRequest {

    @NotBlank(message = "address must not be blank")
    @Size(max = 35)
    private String address;

    @Size(max = 20)
    private String operatorCode;

    /** When true, sets manuallyLocked = true — prevents operator overwrites (PD-282) */
    private boolean lock = false;

    @Size(max = 500)
    private String lockReason;
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/customer/dto/EInvoiceAddressResponse.java`

```java
@Data
@Builder
public class EInvoiceAddressResponse {
    private Long customerId;
    private String address;
    private String operatorCode;
    private boolean manuallyLocked;
    private String lockReason;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static EInvoiceAddressResponse from(EInvoiceAddress e) {
        return EInvoiceAddressResponse.builder()
            .customerId(e.getCustomer().getId())
            .address(e.getAddress())
            .operatorCode(e.getOperatorCode())
            .manuallyLocked(e.isManuallyLocked())
            .lockReason(e.getLockReason())
            .lastModifiedBy(e.getLastModifiedBy())
            .lastModifiedAt(e.getLastModifiedAt())
            .build();
    }

    public static EInvoiceAddressResponse empty(Long customerId) {
        return EInvoiceAddressResponse.builder()
            .customerId(customerId)
            .manuallyLocked(false)
            .build();
    }
}
```

---

## Frontend

### 7.6 einvoiceAddress.js — API Client

**File:** `invoicing-fe/src/api/einvoiceAddress.js`

```js
import axios from './axios'

export const getEInvoiceAddress = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/einvoice-address`)

export const setEInvoiceAddress = (customerId, data) =>
  axios.put(`/api/v1/customers/${customerId}/einvoice-address`, data)
```

---

### 7.7 EInvoiceAddressSection.jsx

**File:** `invoicing-fe/src/pages/customers/components/EInvoiceAddressSection.jsx`

**Purpose:** Sub-section rendered inside `BillingProfilePage` (Step 06) whenever the selected `deliveryMethod` is `E_INVOICE`. Provides a form to view, set, and lock/unlock the e-invoice address.

**Component structure:**

```
EInvoiceAddressSection
├── SectionHeader        — "E-Invoice Address" with lock status badge
├── AddressInput         — text input for the address string (OVT / IBAN)
├── OperatorCodeInput    — text input for the operator BIC code
├── LockToggle           — checkbox or toggle switch: "Lock this address (prevent integration overwrites)"
├── LockReasonInput      — text area, visible and required when LockToggle is on
├── LockStatusBanner     — shown when manuallyLocked = true:
│                          "This address is manually locked. The daily operator integration
│                           will not overwrite it." with an "Unlock" action button
└── SaveAddressButton
```

**Key behaviours:**

- On mount (or when `deliveryMethod` changes to `E_INVOICE`): calls `getEInvoiceAddress(customerId)`
- If `manuallyLocked = true`: show the lock status banner in amber; the address and operator code fields remain editable (the user may want to update and keep it locked)
- `LockToggle` defaults to the current `manuallyLocked` value from the API response
- When `LockToggle` is turned ON: `LockReasonInput` appears and is required before save
- When `LockToggle` is turned OFF and the address was previously locked: show a confirmation: _"Removing the lock will allow the daily operator integration to overwrite this address. Continue?"_
- On save: calls `setEInvoiceAddress(customerId, { address, operatorCode, lock, lockReason })`
- On success: refresh the section data; show success toast "E-invoice address saved"
- On 400 validation error: show field-level error messages

**State shape:**
```js
const [addressData, setAddressData] = useState({
  address: '',
  operatorCode: '',
  lock: false,
  lockReason: '',
  manuallyLocked: false,   // from API response (read-only display)
  lastModifiedBy: null,
  lastModifiedAt: null,
})
```

**API calls:**
```js
// Load
const { data } = await getEInvoiceAddress(customerId)
setAddressData(data)

// Save
const payload = {
  address: addressData.address,
  operatorCode: addressData.operatorCode,
  lock: addressData.lock,
  lockReason: addressData.lock ? addressData.lockReason : null,
}
const { data: saved } = await setEInvoiceAddress(customerId, payload)
setAddressData(saved)
```

---

### 7.8 Integration into BillingProfilePage

In `BillingProfilePage.jsx` (Step 06), import and conditionally render the section:

```jsx
import EInvoiceAddressSection from './components/EInvoiceAddressSection'

// Inside the render, after the main BillingProfileForm:
{profile?.billingProfile?.deliveryMethod === 'E_INVOICE' && (
  <EInvoiceAddressSection customerId={customerId} />
)}
```

---

## Verification Checklist

1. `GET /api/v1/customers/1/einvoice-address` on a customer with no e-invoice address on file — returns 200 with `address: null`, `manuallyLocked: false`
2. `PUT /api/v1/customers/1/einvoice-address` with `address: "003712345678"`, `lock: false` — returns 200; DB row created with `manually_locked = false`
3. `GET /api/v1/customers/1/einvoice-address` — returns the address set in step 2
4. `PUT /api/v1/customers/1/einvoice-address` with `lock: true`, `lockReason: "Confirmed with customer"` — returns 200 with `manuallyLocked: true`
5. Call `EInvoiceAddressService.updateFromOperator(1L, "NEWADDRESS", "POPFFI22")` directly (e.g. from a unit test or integration test) — because `manuallyLocked = true`, the existing address must NOT be changed; `findByCustomer_Id(1)` must still return the old address
6. `PUT /api/v1/customers/1/einvoice-address` with `lock: false` — returns 200 with `manuallyLocked: false`; call `updateFromOperator` again — address IS updated this time
7. `PUT /api/v1/customers/1/einvoice-address` with a blank `address` field — returns 400 "address must not be blank"
8. `EInvoiceAddressRepository.findAllManuallyLocked()` — returns only records where `manually_locked = true`
9. Open FE at `/customers/1/billing-profile` with `deliveryMethod = PAPER` — EInvoice Address section is not rendered
10. Switch `deliveryMethod` to `E_INVOICE` in the form — EInvoice Address section appears immediately
11. In the EInvoice Address section, enter an address and enable the lock toggle — `LockReasonInput` appears
12. Save without filling in `lockReason` while lock is on — form validation prevents submission with an error on the lock reason field
13. Fill in `lockReason`, save — success toast; the amber lock banner appears with the lock reason text
14. Click "Unlock" in the lock banner — confirmation dialog appears; confirm — lock is cleared, banner disappears
15. Verify `lastModifiedBy` and `lastModifiedAt` are updated in the API response after each save

---

## File Checklist

### Backend
- [ ] `customer/EInvoiceAddress.java`
- [ ] `customer/EInvoiceAddressRepository.java`
- [ ] `customer/EInvoiceAddressService.java`
- [ ] `customer/EInvoiceAddressController.java`
- [ ] `customer/dto/EInvoiceAddressRequest.java`
- [ ] `customer/dto/EInvoiceAddressResponse.java`

### Frontend
- [ ] `src/api/einvoiceAddress.js`
- [ ] `src/pages/customers/components/EInvoiceAddressSection.jsx`
- [ ] `src/pages/customers/BillingProfilePage.jsx` — add conditional render of `EInvoiceAddressSection`
