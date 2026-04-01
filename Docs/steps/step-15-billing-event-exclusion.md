# Step 15 — BillingEvent Exclusion and Reinstatement

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingEvent — `excluded` and `exclusionReason` fields (PD-318)
- `Docs/structured_breakdown/02-data-layer.md` → Section: BillingEventRepository — "Excluded events never in billing queries", "Find excluded events" query
- `Docs/structured_breakdown/04-api-layer.md` → `POST /billing-events/{id}/exclude`, `POST /billing-events/{id}/reinstate`, `POST /billing-events/bulk-exclude`

---

## Goal

Implement event exclusion — the ability to remove individual or multiple events from invoicing without deleting them. Excluded events remain in the database, remain visible to billing staff, and continue to appear in the audit trail. They simply never enter a billing run.

Three operations are needed: single exclude, single reinstate, and bulk exclude. Every operation writes an audit log entry. The immutability gate applies: SENT events cannot be excluded.

---

## Backend

### 15.1 ExcludeEventRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/ExcludeEventRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /billing-events/{id}/exclude: exclusionReason in the request body."

```java
@Data
public class ExcludeEventRequest {

    @NotBlank(message = "Exclusion reason is required")
    private String exclusionReason;
}
```

---

### 15.2 BulkExcludeRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/BulkExcludeRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /billing-events/bulk-exclude: list of event IDs and a shared exclusion reason."

```java
@Data
public class BulkExcludeRequest {

    @NotEmpty(message = "At least one event ID is required")
    private List<Long> eventIds;

    @NotBlank(message = "Exclusion reason is required for bulk exclusion")
    private String exclusionReason;
}
```

---

### 15.3 ReinstateEventRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/ReinstateEventRequest.java`

```java
@Data
public class ReinstateEventRequest {

    @NotBlank(message = "A reason is required for reinstatement")
    private String reason;
}
```

---

### 15.4 BillingEventService — exclusion methods

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventService.java` (add to existing)

> **Requirement source:** `03-business-logic.md` — "Sets excluded = true with reason, excludedBy, excludedAt. Does not delete. Both single and bulk operations. The service also supports reinstating a previously excluded event."

```java
// -----------------------------------------------------------------------
// SINGLE EXCLUDE
// POST /api/v1/billing-events/{id}/exclude
// -----------------------------------------------------------------------
@Transactional
public BillingEventResponse exclude(Long id, String exclusionReason, String currentUser) {
    BillingEvent event = billingEventRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));

    // Immutability gate — cannot exclude a SENT or COMPLETED event
    billingEventStatusService.assertMutable(event);

    if (event.isExcluded()) {
        throw new IllegalStateException("BillingEvent " + id + " is already excluded.");
    }

    event.setExcluded(true);
    event.setExclusionReason(exclusionReason);
    event.setExcludedBy(currentUser);
    event.setExcludedAt(Instant.now());

    billingEventRepository.save(event);

    // Audit log — record the exclusion action
    billingEventAuditLogRepository.save(BillingEventAuditLog.builder()
        .billingEventId(id)
        .field("excluded")
        .oldValue("false")
        .newValue("true")
        .changedBy(currentUser)
        .changedAt(Instant.now())
        .reason(exclusionReason)
        .build());

    return toResponse(event);
}

// -----------------------------------------------------------------------
// REINSTATE
// POST /api/v1/billing-events/{id}/reinstate
// -----------------------------------------------------------------------
@Transactional
public BillingEventResponse reinstate(Long id, String reason, String currentUser) {
    BillingEvent event = billingEventRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));

    if (!event.isExcluded()) {
        throw new IllegalStateException("BillingEvent " + id + " is not excluded and cannot be reinstated.");
    }

    String previousReason = event.getExclusionReason();

    event.setExcluded(false);
    event.setExclusionReason(null);
    event.setExcludedBy(null);
    event.setExcludedAt(null);

    billingEventRepository.save(event);

    // Audit log — record the reinstatement
    billingEventAuditLogRepository.save(BillingEventAuditLog.builder()
        .billingEventId(id)
        .field("excluded")
        .oldValue("true (reason: " + previousReason + ")")
        .newValue("false")
        .changedBy(currentUser)
        .changedAt(Instant.now())
        .reason(reason)
        .build());

    return toResponse(event);
}

// -----------------------------------------------------------------------
// BULK EXCLUDE
// POST /api/v1/billing-events/bulk-exclude
// -----------------------------------------------------------------------
@Transactional
public BulkExcludeResult bulkExclude(List<Long> eventIds, String exclusionReason, String currentUser) {
    List<BillingEvent> events = billingEventRepository.findAllById(eventIds);
    List<Long> succeeded = new ArrayList<>();
    List<BulkExcludeFailure> failed = new ArrayList<>();

    for (BillingEvent event : events) {
        try {
            billingEventStatusService.assertMutable(event);   // skip SENT/COMPLETED
            if (event.isExcluded()) {
                failed.add(new BulkExcludeFailure(event.getId(), "Already excluded"));
                continue;
            }
            event.setExcluded(true);
            event.setExclusionReason(exclusionReason);
            event.setExcludedBy(currentUser);
            event.setExcludedAt(Instant.now());
            succeeded.add(event.getId());
        } catch (IllegalStateException ex) {
            failed.add(new BulkExcludeFailure(event.getId(), ex.getMessage()));
        }
    }

    billingEventRepository.saveAll(events.stream()
        .filter(e -> succeeded.contains(e.getId()))
        .toList());

    // Write one audit entry per successfully excluded event
    List<BillingEventAuditLog> auditEntries = succeeded.stream()
        .map(eventId -> BillingEventAuditLog.builder()
            .billingEventId(eventId)
            .field("excluded")
            .oldValue("false")
            .newValue("true")
            .changedBy(currentUser)
            .changedAt(Instant.now())
            .reason(exclusionReason)
            .build())
        .toList();
    billingEventAuditLogRepository.saveAll(auditEntries);

    return new BulkExcludeResult(succeeded, failed);
}
```

---

### 15.5 Exclusion endpoints in BillingEventController

Add to `BillingEventController.java`:

```java
/**
 * POST /api/v1/billing-events/{id}/exclude
 * Exclude a single event from invoicing.
 * Role: INVOICING_USER
 */
@PostMapping("/{id}/exclude")
public BillingEventResponse exclude(
    @PathVariable Long id,
    @Valid @RequestBody ExcludeEventRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventService.exclude(id, request.getExclusionReason(), currentUser);
}

/**
 * POST /api/v1/billing-events/{id}/reinstate
 * Reinstate a previously excluded event.
 * Role: INVOICING_USER
 */
@PostMapping("/{id}/reinstate")
public BillingEventResponse reinstate(
    @PathVariable Long id,
    @Valid @RequestBody ReinstateEventRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventService.reinstate(id, request.getReason(), currentUser);
}

/**
 * POST /api/v1/billing-events/bulk-exclude
 * Exclude multiple events in one operation.
 * Role: INVOICING_USER
 */
@PostMapping("/bulk-exclude")
public BulkExcludeResult bulkExclude(
    @Valid @RequestBody BulkExcludeRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventService.bulkExclude(
        request.getEventIds(), request.getExclusionReason(), currentUser);
}
```

**Request/Response example — single exclude:**

`POST /api/v1/billing-events/1001/exclude`
```json
{
  "exclusionReason": "Customer dispute — do not bill until resolved"
}
```

Response `200 OK`:
```json
{
  "id": 1001,
  "excluded": true,
  "exclusionReason": "Customer dispute — do not bill until resolved",
  "excludedBy": "anna.virtanen",
  "excludedAt": "2024-01-16T14:00:00Z",
  "status": "IN_PROGRESS"
}
```

**Request/Response example — reinstate:**

`POST /api/v1/billing-events/1001/reinstate`
```json
{
  "reason": "Dispute resolved — safe to invoice"
}
```

Response `200 OK`:
```json
{
  "id": 1001,
  "excluded": false,
  "exclusionReason": null,
  "status": "IN_PROGRESS"
}
```

**Request/Response example — bulk exclude:**

`POST /api/v1/billing-events/bulk-exclude`
```json
{
  "eventIds": [1002, 1003, 1004],
  "exclusionReason": "Annual review — holding these events pending audit clearance"
}
```

Response `200 OK`:
```json
{
  "succeeded": [1002, 1003],
  "failed": [
    {
      "eventId": 1004,
      "reason": "BillingEvent 1004 is SENT and cannot be modified."
    }
  ]
}
```

**Attempt to exclude a SENT event:**

`POST /api/v1/billing-events/1005/exclude`
```json
{
  "exclusionReason": "Error detected"
}
```

Response `409 Conflict`:
```json
{
  "error": "STATUS_TRANSITION_ERROR",
  "message": "BillingEvent 1005 is SENT and cannot be modified. Use the credit-and-re-invoice flow for corrections."
}
```

---

### 15.6 Repository filter guarantee

> **Requirement source:** `02-data-layer.md` — "Excluded events must NEVER appear in billing queries. This filter must be in every relevant query definition, not left to calling code to add."

All eight billing queries in `BillingEventRepository` (Step 10) already include `AND e.excluded = false`. This must be verified during this step. Run the following test to confirm the guarantee holds:

```java
// Integration test: verify excluded events never appear in billing queries
@Test
void excludedEventNeverAppearsInBillingQueries() {
    BillingEvent event = createAndSaveEvent();
    billingEventService.exclude(event.getId(), "test exclusion", "test-user");

    // findByRunFilter must not return the excluded event
    List<BillingEvent> runResults = billingEventRepository
        .findByRunFilter(null, null, null, null, null, null);
    assertThat(runResults).noneMatch(e -> e.getId().equals(event.getId()));

    // findUnbilledByCustomerAndDateRange must not return it
    List<BillingEvent> unbilledResults = billingEventRepository
        .findUnbilledByCustomerAndDateRange(event.getCustomerNumber(),
            event.getEventDate().minusDays(1), event.getEventDate().plusDays(1));
    assertThat(unbilledResults).noneMatch(e -> e.getId().equals(event.getId()));
}
```

---

## Frontend

### 15.7 Exclude Button on Event Detail Page

**File:** `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` (extend existing from Step 13)

Add to the action bar on the detail page:
- "Exclude Event" button — visible only when `excluded === false` AND `status !== 'SENT'` AND `status !== 'COMPLETED'`
- "Reinstate Event" button — visible only when `excluded === true`

Both open a modal that requires the user to type a reason before confirming.

---

### 15.8 ExclusionModal Component

**File:** `invoicing-fe/src/components/billing/ExclusionModal.jsx`

```jsx
// Used for both exclude and reinstate — the `mode` prop ('exclude' | 'reinstate')
// controls the title and submit label.

export default function ExclusionModal({ eventId, mode, onSuccess, onClose }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!reason.trim()) return
    setLoading(true)
    try {
      if (mode === 'exclude') {
        await excludeBillingEvent(eventId, reason)
      } else {
        await reinstateBillingEvent(eventId, reason)
      }
      onSuccess()
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title={mode === 'exclude' ? 'Exclude Event' : 'Reinstate Event'} onClose={onClose}>
      <label>
        {mode === 'exclude' ? 'Exclusion Reason' : 'Reason for Reinstatement'}
        <span className="text-red-500 ml-1">*</span>
      </label>
      <textarea
        value={reason}
        onChange={e => setReason(e.target.value)}
        rows={3}
        className="w-full border rounded p-2 mt-1"
        placeholder="Required"
      />
      <div className="flex justify-end gap-2 mt-4">
        <button onClick={onClose}>Cancel</button>
        <button
          onClick={handleSubmit}
          disabled={!reason.trim() || loading}
          className={mode === 'exclude' ? 'btn-danger' : 'btn-primary'}
        >
          {loading ? 'Saving…' : (mode === 'exclude' ? 'Exclude' : 'Reinstate')}
        </button>
      </div>
    </Modal>
  )
}
```

---

### 15.9 Visual indicator for excluded events

On `BillingEventsPage.jsx` (list page), excluded events are visually differentiated:
- Row text is rendered with `line-through` text decoration
- An "Excluded" badge (grey background) replaces or accompanies the status badge
- The row background is a pale grey to draw attention

```jsx
<tr className={event.excluded ? 'opacity-60 line-through bg-gray-50' : ''}>
  <td>{event.eventDate}</td>
  <td>{event.customerNumber}</td>
  ...
  <td>
    {event.excluded
      ? <span className="badge-grey">Excluded</span>
      : <StatusBadge status={event.status} />}
  </td>
</tr>
```

---

### 15.10 Bulk exclude on list page

On `BillingEventsPage.jsx`, the "Validate Selected" toolbar (Step 14) is extended:
- "Exclude Selected" button — enabled when at least one event is selected
- Opens a shared-reason modal (same `ExclusionModal` with `mode='exclude'`, applied to all selected IDs)
- After bulk exclude, refreshes the list and shows a summary toast: "2 excluded, 1 skipped (already sent)"

---

### 15.11 API additions

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const excludeBillingEvent = (id, exclusionReason) =>
  axios.post(`/api/v1/billing-events/${id}/exclude`, { exclusionReason })

export const reinstateBillingEvent = (id, reason) =>
  axios.post(`/api/v1/billing-events/${id}/reinstate`, { reason })

export const bulkExcludeBillingEvents = (eventIds, exclusionReason) =>
  axios.post('/api/v1/billing-events/bulk-exclude', { eventIds, exclusionReason })
```

---

## Verification Checklist

1. `POST /api/v1/billing-events/{id}/exclude` without a reason body — verify 400 with "Exclusion reason is required".
2. `POST /api/v1/billing-events/{id}/exclude` on a valid IN_PROGRESS event — verify 200, `excluded: true`, audit log entry written.
3. `GET /api/v1/billing-events?excluded=false` after step 2 — excluded event does NOT appear.
4. `GET /api/v1/billing-events?excluded=true` — excluded event DOES appear (it must remain visible).
5. `POST /api/v1/billing-events/{id}/exclude` on an already-excluded event — verify 409.
6. `POST /api/v1/billing-events/{id}/reinstate` — verify 200, `excluded: false`, audit log entry written.
7. `POST /api/v1/billing-events/{id}/exclude` on a SENT event — verify 409 with status gate message.
8. `POST /api/v1/billing-events/bulk-exclude` with 3 IDs (1 SENT, 2 IN_PROGRESS) — verify 200, `succeeded` has 2 IDs, `failed` has 1 with a reason.
9. `findByRunFilter` does not return excluded events — confirmed by the integration test in 15.6.
10. Frontend: excluded events show grey/strikethrough styling on the list page.
11. Frontend: "Exclude Event" button is hidden on a SENT event detail page.
12. Frontend: excluding an event from the detail page shows it as "Excluded" immediately (optimistic update or refetch).

---

## File Checklist

### Backend
- [ ] `billingevent/dto/ExcludeEventRequest.java`
- [ ] `billingevent/dto/ReinstateEventRequest.java`
- [ ] `billingevent/dto/BulkExcludeRequest.java`
- [ ] `billingevent/dto/BulkExcludeResult.java`
- [ ] `billingevent/dto/BulkExcludeFailure.java`
- [ ] `billingevent/BillingEventService.java` — add `exclude()`, `reinstate()`, `bulkExclude()`
- [ ] `billingevent/BillingEventController.java` — add `POST /{id}/exclude`, `POST /{id}/reinstate`, `POST /bulk-exclude`

### Frontend
- [ ] `src/components/billing/ExclusionModal.jsx`
- [ ] `src/pages/billing/BillingEventDetailPage.jsx` — add exclude/reinstate buttons
- [ ] `src/pages/billing/BillingEventsPage.jsx` — add bulk exclude button, excluded row styling
- [ ] `src/api/billingEvents.js` — add `excludeBillingEvent`, `reinstateBillingEvent`, `bulkExcludeBillingEvents`
