# Step 16 — BillingEvent Transfer

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingEventTransferService
- `Docs/structured_breakdown/01-domain-model.md` → Audit entities: BillingEventTransferLog
- `Docs/structured_breakdown/04-api-layer.md` → `POST /billing-events/{id}/transfer`, `POST /billing-events/bulk-transfer`
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT"

---

## Goal

Implement `BillingEventTransferService` — the ability to move billing events from one customer to another. This handles real-world scenarios like a resident who has moved out (events should belong to the new resident) or a driver who acknowledged a service to the wrong destination.

Every transfer writes a `BillingEventTransferLog` audit record. The immutability gate applies: events that are SENT or COMPLETED cannot be transferred.

---

## Backend

### 16.1 BillingEventTransferLog Entity

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/transfer/BillingEventTransferLog.java`

> **Requirement source:** `01-domain-model.md` — "BillingEventTransferLog — records every customer transfer of an event (PD-276, PD-344). These are append-only. Nothing in the system deletes from them."

```java
@Entity
@Table(name = "billing_event_transfer_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventTransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long billingEventId;           // Transferred event (plain Long — see audit note in Step 10)

    @Column(nullable = false, length = 9)
    private String sourceCustomerNumber;   // Customer the event was moved FROM

    @Column(nullable = false, length = 9)
    private String targetCustomerNumber;   // Customer the event was moved TO

    @Column(length = 100)
    private String sourcePropertyId;       // Property transferred FROM (if location transfer)

    @Column(length = 100)
    private String targetPropertyId;       // Property transferred TO (if location transfer)

    @Column(nullable = false, length = 100)
    private String transferredBy;          // User who performed the transfer

    @Column(nullable = false)
    private Instant transferredAt;         // UTC timestamp

    @Column(nullable = false, length = 2000)
    private String reason;                 // Required — no silent transfers
}
```

---

### 16.2 BillingEventTransferLogRepository

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/transfer/BillingEventTransferLogRepository.java`

```java
public interface BillingEventTransferLogRepository
        extends JpaRepository<BillingEventTransferLog, Long> {

    List<BillingEventTransferLog> findByBillingEventIdOrderByTransferredAtDesc(Long billingEventId);

    List<BillingEventTransferLog> findBySourceCustomerNumberOrderByTransferredAtDesc(String customerNumber);

    List<BillingEventTransferLog> findByTargetCustomerNumberOrderByTransferredAtDesc(String customerNumber);
}
```

---

### 16.3 TransferEventRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/transfer/dto/TransferEventRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /billing-events/{id}/transfer: body: targetCustomerId."

```java
@Data
public class TransferEventRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Target customer number must be 6–9 digits")
    private String targetCustomerNumber;

    // Optional — use when the transfer is a location/property correction
    private String targetPropertyId;

    @NotBlank(message = "A reason is required for every transfer")
    private String reason;
}
```

---

### 16.4 BulkTransferRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/transfer/dto/BulkTransferRequest.java`

```java
@Data
public class BulkTransferRequest {

    @NotEmpty
    private List<Long> eventIds;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Target customer number must be 6–9 digits")
    private String targetCustomerNumber;

    private String targetPropertyId;

    @NotBlank(message = "A reason is required for bulk transfer")
    private String reason;
}
```

---

### 16.5 BillingEventTransferService

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/transfer/BillingEventTransferService.java`

> **Requirement source:** `03-business-logic.md` — "Checks that the event is IN_PROGRESS. Moves the event to the new customer. Writes a BillingEventTransferLog with from/to customer, user, timestamp, and reason. The original customer record no longer has this event associated."

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventTransferService {

    private final BillingEventRepository            billingEventRepository;
    private final BillingEventTransferLogRepository transferLogRepository;
    private final BillingEventStatusService         statusService;

    // -----------------------------------------------------------------------
    // SINGLE TRANSFER
    // POST /api/v1/billing-events/{id}/transfer
    // -----------------------------------------------------------------------
    public TransferResult transfer(Long eventId, TransferEventRequest request, String currentUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        // Immutability gate — cannot transfer SENT or COMPLETED events
        statusService.assertMutable(event);

        String previousCustomer = event.getCustomerNumber();
        String previousProperty = event.getLocationId();

        // Perform the transfer
        event.setCustomerNumber(request.getTargetCustomerNumber());
        if (request.getTargetPropertyId() != null) {
            event.setLocationId(request.getTargetPropertyId());
        }

        billingEventRepository.save(event);

        // Write the transfer audit log
        transferLogRepository.save(BillingEventTransferLog.builder()
            .billingEventId(eventId)
            .sourceCustomerNumber(previousCustomer)
            .targetCustomerNumber(request.getTargetCustomerNumber())
            .sourcePropertyId(previousProperty)
            .targetPropertyId(request.getTargetPropertyId())
            .transferredBy(currentUser)
            .transferredAt(Instant.now())
            .reason(request.getReason())
            .build());

        return new TransferResult(eventId, previousCustomer,
            request.getTargetCustomerNumber(), true, null);
    }

    // -----------------------------------------------------------------------
    // BULK TRANSFER
    // POST /api/v1/billing-events/bulk-transfer
    // -----------------------------------------------------------------------
    public BulkTransferResult bulkTransfer(BulkTransferRequest request, String currentUser) {
        List<BillingEvent> events = billingEventRepository.findAllById(request.getEventIds());
        List<Long> succeeded = new ArrayList<>();
        List<BulkTransferFailure> failed = new ArrayList<>();

        for (BillingEvent event : events) {
            try {
                statusService.assertMutable(event);

                String previousCustomer = event.getCustomerNumber();
                String previousProperty = event.getLocationId();

                event.setCustomerNumber(request.getTargetCustomerNumber());
                if (request.getTargetPropertyId() != null) {
                    event.setLocationId(request.getTargetPropertyId());
                }

                succeeded.add(event.getId());

                // Build transfer log for this event
                transferLogRepository.save(BillingEventTransferLog.builder()
                    .billingEventId(event.getId())
                    .sourceCustomerNumber(previousCustomer)
                    .targetCustomerNumber(request.getTargetCustomerNumber())
                    .sourcePropertyId(previousProperty)
                    .targetPropertyId(request.getTargetPropertyId())
                    .transferredBy(currentUser)
                    .transferredAt(Instant.now())
                    .reason(request.getReason())
                    .build());

            } catch (IllegalStateException ex) {
                failed.add(new BulkTransferFailure(event.getId(), ex.getMessage()));
            }
        }

        billingEventRepository.saveAll(events.stream()
            .filter(e -> succeeded.contains(e.getId()))
            .toList());

        return new BulkTransferResult(succeeded, failed);
    }
}
```

---

### 16.6 Transfer endpoints in BillingEventController

Add to `BillingEventController.java`:

```java
/**
 * POST /api/v1/billing-events/{id}/transfer
 * Transfer a single unbilled event to a different customer.
 * Role: INVOICING_USER
 */
@PostMapping("/{id}/transfer")
public TransferResult transfer(
    @PathVariable Long id,
    @Valid @RequestBody TransferEventRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventTransferService.transfer(id, request, currentUser);
}

/**
 * POST /api/v1/billing-events/bulk-transfer
 * Transfer multiple unbilled events to a different customer.
 * Role: INVOICING_USER
 */
@PostMapping("/bulk-transfer")
public BulkTransferResult bulkTransfer(
    @Valid @RequestBody BulkTransferRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventTransferService.bulkTransfer(request, currentUser);
}
```

**Request/Response example — single transfer:**

`POST /api/v1/billing-events/1001/transfer`
```json
{
  "targetCustomerNumber": "654321",
  "reason": "Customer moved out on 2024-01-10. Transferring unbilled events to new resident."
}
```

Response `200 OK`:
```json
{
  "eventId": 1001,
  "sourceCustomerNumber": "123456",
  "targetCustomerNumber": "654321",
  "success": true,
  "failureReason": null
}
```

**Request/Response example — transfer blocked (SENT event):**

`POST /api/v1/billing-events/1005/transfer`
```json
{
  "targetCustomerNumber": "654321",
  "reason": "Wrong customer"
}
```

Response `409 Conflict`:
```json
{
  "error": "STATUS_TRANSITION_ERROR",
  "message": "BillingEvent 1005 is SENT and cannot be modified. Use the credit-and-re-invoice flow for corrections."
}
```

**Request/Response example — bulk transfer:**

`POST /api/v1/billing-events/bulk-transfer`
```json
{
  "eventIds": [1001, 1002, 1003],
  "targetCustomerNumber": "654321",
  "reason": "Customer 123456 moved out. All unbilled events transferred to new resident 654321."
}
```

Response `200 OK`:
```json
{
  "succeeded": [1001, 1002],
  "failed": [
    {
      "eventId": 1003,
      "reason": "BillingEvent 1003 is SENT and cannot be modified."
    }
  ]
}
```

**Request/Response example — location/property transfer:**

`POST /api/v1/billing-events/1007/transfer`
```json
{
  "targetCustomerNumber": "123456",
  "targetPropertyId": "PROP-0099",
  "reason": "Driver acknowledged emptying to wrong destination. Correct property is PROP-0099."
}
```

Response `200 OK`:
```json
{
  "eventId": 1007,
  "sourceCustomerNumber": "123456",
  "targetCustomerNumber": "123456",
  "success": true,
  "failureReason": null
}
```

---

### 16.7 Flyway migration for transfer log

**File:** `invoicing/src/main/resources/db/migration/V16__billing_event_transfer_log.sql`

```sql
CREATE TABLE billing_event_transfer_log (
    id                      BIGSERIAL    PRIMARY KEY,
    billing_event_id        BIGINT       NOT NULL,
    source_customer_number  VARCHAR(9)   NOT NULL,
    target_customer_number  VARCHAR(9)   NOT NULL,
    source_property_id      VARCHAR(100),
    target_property_id      VARCHAR(100),
    transferred_by          VARCHAR(100) NOT NULL,
    transferred_at          TIMESTAMPTZ  NOT NULL,
    reason                  VARCHAR(2000) NOT NULL
);

CREATE INDEX idx_transfer_log_event      ON billing_event_transfer_log(billing_event_id);
CREATE INDEX idx_transfer_log_source_cust ON billing_event_transfer_log(source_customer_number);
CREATE INDEX idx_transfer_log_target_cust ON billing_event_transfer_log(target_customer_number);

-- Audit table — no mutation permissions for the app user
REVOKE UPDATE, DELETE ON billing_event_transfer_log FROM invoicing_app_user;
```

---

## Frontend

### 16.8 Transfer Button on Event Detail Page

**File:** `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` (extend existing)

Add a "Transfer Event" button to the action bar. Visible only when:
- `status === 'IN_PROGRESS'` (not SENT or COMPLETED)
- `excluded === false`

---

### 16.9 TransferEventModal Component

**File:** `invoicing-fe/src/components/billing/TransferEventModal.jsx`

```jsx
export default function TransferEventModal({ eventId, currentCustomerNumber, onSuccess, onClose }) {
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')
  const [targetPropertyId, setTargetPropertyId]     = useState('')
  const [reason, setReason]                         = useState('')
  const [loading, setLoading]                       = useState(false)
  const [error, setError]                           = useState(null)

  const handleSubmit = async () => {
    if (!targetCustomerNumber.trim() || !reason.trim()) return
    setLoading(true)
    setError(null)
    try {
      await transferBillingEvent(eventId, {
        targetCustomerNumber,
        targetPropertyId: targetPropertyId || undefined,
        reason
      })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Transfer failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="Transfer Billing Event" onClose={onClose}>
      <p className="text-sm text-gray-500 mb-4">
        Currently assigned to customer <strong>{currentCustomerNumber}</strong>.
      </p>

      {error && <div className="alert-error mb-4">{error}</div>}

      <label>Target Customer Number <span className="text-red-500">*</span></label>
      <input
        type="text"
        pattern="\d{6,9}"
        value={targetCustomerNumber}
        onChange={e => setTargetCustomerNumber(e.target.value)}
        placeholder="6–9 digit customer number"
        className="input-field"
      />

      <label className="mt-3">Target Property ID (optional)</label>
      <input
        type="text"
        value={targetPropertyId}
        onChange={e => setTargetPropertyId(e.target.value)}
        placeholder="Leave blank if not a location transfer"
        className="input-field"
      />

      <label className="mt-3">Reason <span className="text-red-500">*</span></label>
      <textarea
        value={reason}
        onChange={e => setReason(e.target.value)}
        rows={3}
        className="input-field"
        placeholder="Required"
      />

      <div className="flex justify-end gap-2 mt-4">
        <button onClick={onClose}>Cancel</button>
        <button
          onClick={handleSubmit}
          disabled={!targetCustomerNumber.trim() || !reason.trim() || loading}
          className="btn-primary"
        >
          {loading ? 'Transferring…' : 'Transfer Event'}
        </button>
      </div>
    </Modal>
  )
}
```

---

### 16.10 Bulk Transfer on List Page

On `BillingEventsPage.jsx`, extend the toolbar:
- "Transfer Selected" button — enabled when at least one event is selected
- Opens `TransferEventModal` in bulk mode
- After bulk transfer, shows a summary toast: "2 transferred, 1 skipped (already sent)"

---

### 16.11 API additions

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const transferBillingEvent = (id, data) =>
  axios.post(`/api/v1/billing-events/${id}/transfer`, data)

export const bulkTransferBillingEvents = (data) =>
  axios.post('/api/v1/billing-events/bulk-transfer', data)
```

---

## Verification Checklist

1. `POST /api/v1/billing-events/{id}/transfer` on an IN_PROGRESS event — verify 200, event's `customerNumber` is updated to `targetCustomerNumber`.
2. Verify a `BillingEventTransferLog` row is written with correct `sourceCustomerNumber`, `targetCustomerNumber`, `transferredBy`, and `reason`.
3. `POST /api/v1/billing-events/{id}/transfer` on a SENT event — verify 409 Conflict.
4. `POST /api/v1/billing-events/{id}/transfer` without a reason — verify 400.
5. `POST /api/v1/billing-events/{id}/transfer` with `targetCustomerNumber: "12345"` (5 digits) — verify 400 with format error.
6. `POST /api/v1/billing-events/bulk-transfer` with 3 events (2 IN_PROGRESS, 1 SENT) — verify `succeeded` has 2 IDs, `failed` has 1.
7. After a location transfer (with `targetPropertyId`), verify `event.locationId` is updated.
8. After a same-customer location transfer, verify `customerNumber` is unchanged and `locationId` is updated.
9. `GET /api/v1/billing-events/{id}/audit-log` — transfer events do NOT appear here (transfer log is separate from the field-level audit log).
10. Frontend: "Transfer Event" button is hidden on a SENT or COMPLETED event.
11. Frontend: submitting the TransferModal with a 5-digit customer number shows a validation error.
12. Frontend: after a successful transfer, the detail page reloads and shows the new customer number.

---

## File Checklist

### Backend
- [ ] `billingevent/transfer/BillingEventTransferLog.java`
- [ ] `billingevent/transfer/BillingEventTransferLogRepository.java`
- [ ] `billingevent/transfer/dto/TransferEventRequest.java`
- [ ] `billingevent/transfer/dto/BulkTransferRequest.java`
- [ ] `billingevent/transfer/dto/TransferResult.java`
- [ ] `billingevent/transfer/dto/BulkTransferResult.java`
- [ ] `billingevent/transfer/dto/BulkTransferFailure.java`
- [ ] `billingevent/transfer/BillingEventTransferService.java`
- [ ] `billingevent/BillingEventController.java` — add `POST /{id}/transfer`, `POST /bulk-transfer`
- [ ] `db/migration/V16__billing_event_transfer_log.sql`

### Frontend
- [ ] `src/components/billing/TransferEventModal.jsx`
- [ ] `src/pages/billing/BillingEventDetailPage.jsx` — add Transfer button
- [ ] `src/pages/billing/BillingEventsPage.jsx` — add bulk transfer button
- [ ] `src/api/billingEvents.js` — add `transferBillingEvent`, `bulkTransferBillingEvents`
