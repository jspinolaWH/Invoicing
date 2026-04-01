# Step 52 — Billing Address Sync to External System

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingProfileSyncService
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: "Billing address updates" and "Integration failure handling"
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 10: "WasteHero is the Master for Billing Data"

---

## Goal
Implement `BillingProfileSyncService`, an event-driven service triggered whenever a `BillingProfile` is saved in WasteHero. It pushes the updated address to the external invoicing system and then updates all open invoices for that customer in the external system. WasteHero is always the master — if the sync call fails, the address is still saved in WasteHero; the failure is logged and scheduled for retry. No new user-facing write endpoints. FE shows a sync status indicator (synced / pending / failed) on the billing profile.

---

## Backend

### 52.1 BillingAddressSyncStatus Enum

```java
public enum BillingAddressSyncStatus {
    SYNCED,    // external system is up to date
    PENDING,   // sync not yet attempted or retry queued
    FAILED     // last sync attempt failed (retry pending)
}
```

Add to `BillingProfile` (or `Customer`) entity:
```java
@Enumerated(EnumType.STRING)
@Column(name = "address_sync_status")
private BillingAddressSyncStatus addressSyncStatus = BillingAddressSyncStatus.PENDING;

@Column(name = "address_sync_error", length = 500)
private String addressSyncError;

@Column(name = "address_sync_last_attempted_at")
private Instant addressSyncLastAttemptedAt;
```

---

### 52.2 BillingProfileUpdatedEvent

**File:** `invoicing/src/main/java/com/example/invoicing/billing/BillingProfileUpdatedEvent.java`

> **Requirement source:** `03-business-logic.md` — "Triggered as a Spring Application Event whenever a billing address is updated in WasteHero"

```java
public class BillingProfileUpdatedEvent extends ApplicationEvent {
    private final Long customerId;
    private final String customerNumber;

    public BillingProfileUpdatedEvent(Object source, Long customerId, String customerNumber) {
        super(source);
        this.customerId = customerId;
        this.customerNumber = customerNumber;
    }
}
```

Published from `BillingProfileService.update()`:
```java
applicationEventPublisher.publishEvent(
    new BillingProfileUpdatedEvent(this, customerId, customerNumber));
```

---

### 52.3 BillingProfileSyncService

**File:** `invoicing/src/main/java/com/example/invoicing/billing/BillingProfileSyncService.java`

> **Requirement source:** `05-integration-layer.md` — "If the sync call to the external system fails, the new address is still saved in WasteHero. The failed sync is logged and retried."

```java
@Service
public class BillingProfileSyncService {

    @Async
    @EventListener
    @Transactional
    public void onBillingProfileUpdated(BillingProfileUpdatedEvent event) {
        Customer customer = customerRepository.findById(event.getCustomerId()).orElseThrow();
        BillingProfile profile = customer.getBillingProfile();

        try {
            // 1. Push updated billing address to external invoicing system
            externalInvoicingClient.updateCustomerAddress(profile);

            // 2. Fetch all open invoices for this customer and update their delivery address
            List<Invoice> openInvoices = invoiceRepository.findOpenByCustomerId(event.getCustomerId());
            for (Invoice invoice : openInvoices) {
                externalInvoicingClient.updateInvoiceAddress(
                    invoice.getInvoiceNumber(), profile.getBillingAddress());
            }

            profile.setAddressSyncStatus(BillingAddressSyncStatus.SYNCED);
            profile.setAddressSyncError(null);

        } catch (ExternalSystemUnavailableException ex) {
            // WasteHero is master — save address regardless; schedule retry
            profile.setAddressSyncStatus(BillingAddressSyncStatus.FAILED);
            profile.setAddressSyncError(ex.getMessage());
            log.error("Billing address sync failed for customer {}: {}",
                event.getCustomerId(), ex.getMessage());
            // Queue retry (simple approach: scheduler will re-process FAILED profiles)
        } finally {
            profile.setAddressSyncLastAttemptedAt(Instant.now());
            customerRepository.save(customer);
        }
    }

    /**
     * Retry all FAILED sync profiles — called by scheduled job.
     */
    @Scheduled(fixedDelay = 300_000)  // every 5 minutes
    public void retryFailedSyncs() {
        List<Customer> failedCustomers = customerRepository
            .findByBillingProfileAddressSyncStatus(BillingAddressSyncStatus.FAILED);
        for (Customer customer : failedCustomers) {
            applicationEventPublisher.publishEvent(
                new BillingProfileUpdatedEvent(this, customer.getId(),
                    customer.getBillingProfile().getCustomerNumber()));
        }
    }
}
```

---

### 52.4 ExternalInvoicingClient — Address Update Methods

**Extension to `ExternalInvoicingClient`** (from step 50):

```java
public void updateCustomerAddress(BillingProfile profile) {
    CustomerAddressUpdateRequest body = CustomerAddressUpdateRequest.from(profile);
    restTemplate.put(config.getCustomerUpdateUrl() + "/" + profile.getCustomerNumber(),
        new HttpEntity<>(body, buildAuthHeaders()));
}

public void updateInvoiceAddress(String invoiceNumber, BillingAddress address) {
    if (invoiceNumber == null) return;  // invoice not yet transmitted — skip
    InvoiceAddressUpdateRequest body = InvoiceAddressUpdateRequest.from(invoiceNumber, address);
    restTemplate.put(config.getInvoiceAddressUpdateUrl() + "/" + invoiceNumber,
        new HttpEntity<>(body, buildAuthHeaders()));
}
```

---

## Frontend

### 52.5 Sync Status Indicator on Billing Profile

**Extension to customer billing profile page** (referenced in step 6):

**`AddressSyncStatusBadge` component:**

```jsx
const AddressSyncStatusBadge = ({ syncStatus, syncError, lastAttemptedAt }) => {
  const config = {
    SYNCED:  { color: 'green',  label: 'Synced',       icon: '✓' },
    PENDING: { color: 'grey',   label: 'Pending sync', icon: '⏳' },
    FAILED:  { color: 'red',    label: 'Sync failed',  icon: '✗' }
  }
  const { color, label, icon } = config[syncStatus] || config.PENDING

  return (
    <span style={{ color }} title={syncError || `Last attempted: ${lastAttemptedAt}`}>
      {icon} {label}
    </span>
  )
}
```

- **SYNCED**: green tick — "Synced with external invoicing system".
- **PENDING**: grey hourglass — "Sync pending".
- **FAILED**: red cross — tooltip shows `syncError` message and last attempt time.

**No new API endpoint** — `syncStatus`, `syncError`, and `addressSyncLastAttemptedAt` are included in the existing `GET /api/v1/customers/{id}/billing-profile` response.

**Billing profile response extension:**
```json
{
  "customerId": 1001,
  "customerNumber": "123456",
  "billingAddress": { "...": "..." },
  "deliveryMethod": "E_INVOICE",
  "addressSyncStatus": "SYNCED",
  "addressSyncError": null,
  "addressSyncLastAttemptedAt": "2024-03-01T08:05:00Z"
}
```

---

## Verification Checklist

1. Update a customer billing address via `PUT /api/v1/customers/{id}/billing-profile` — `BillingProfileUpdatedEvent` is published and `BillingProfileSyncService.onBillingProfileUpdated()` is called.
2. External sync succeeds → `addressSyncStatus = SYNCED`, `addressSyncError = null`.
3. Mock external system to return 503 → billing profile saved with new address; `addressSyncStatus = FAILED`; `addressSyncError` contains error message.
4. Scheduled `retryFailedSyncs()` — picks up FAILED profiles and re-publishes events; after successful retry, `addressSyncStatus = SYNCED`.
5. Open invoices (DRAFT/READY/SENT status) for the customer have their delivery address updated in the external system after sync.
6. WasteHero is master: after a failed sync, the address in WasteHero DB is the updated one — not reverted to the old value.
7. `@Async` — address sync runs in a separate thread; `PUT billing-profile` endpoint returns immediately without waiting for the sync call.
8. `BillingAddressSyncStatus.PENDING` is the initial state for a newly created billing profile (before first sync).
9. Open billing profile page in FE — `AddressSyncStatusBadge` shows "Synced" (green) after successful sync, "Sync failed" (red) with tooltip after failure.
10. Customer locked during a run (step 39): `assertNotLocked()` prevents billing profile update; sync never fires during an active run for that customer.

---

## File Checklist

### Backend
- [ ] `billing/BillingProfileUpdatedEvent.java`
- [ ] `billing/BillingProfileSyncService.java`
- [ ] `billing/BillingAddressSyncStatus.java` (enum)
- [ ] `integration/ExternalInvoicingClient.java` — add `updateCustomerAddress()` and `updateInvoiceAddress()` (extends step 50)
- [ ] `billing/BillingProfile.java` or `customer/Customer.java` — add sync status fields

### Frontend
- [ ] `src/components/AddressSyncStatusBadge.jsx`
- [ ] `src/pages/customers/BillingProfilePage.jsx` — add sync status badge (extends step 6)
