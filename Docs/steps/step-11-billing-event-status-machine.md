# Step 11 — BillingEvent Status Machine

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingEvent — status lifecycle (PD-297)
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingEventStatusService
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT"

---

## Goal

Implement `BillingEventStatusService` — the single owner of all status transitions for `BillingEvent`. No other class in the system may write the `status` field directly. The service enforces the four-state lifecycle (IN_PROGRESS → SENT → COMPLETED/ERROR) and acts as a gate that blocks all mutation attempts on events that have already been sent.

This step also adds the `StatusBadge` component to the frontend, which is reused in every subsequent step that displays events.

---

## Backend

### 11.1 Valid Transition Map

> **Requirement source:** `03-business-logic.md` — BillingEventStatusService: "Allowed transitions: IN_PROGRESS → SENT, SENT → ERROR, SENT → COMPLETED, ERROR → SENT"

The full allowed transition table:

| From | To | Trigger |
|---|---|---|
| IN_PROGRESS | SENT | FINVOICE transmitted successfully |
| SENT | COMPLETED | External system confirms receipt |
| SENT | ERROR | External system reports failure |
| ERROR | SENT | User corrects data and re-sends |

Any other combination (e.g. IN_PROGRESS → COMPLETED, COMPLETED → IN_PROGRESS) must throw `IllegalStateException`.

---

### 11.2 BillingEventStatusService

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventStatusService.java`

> **Requirement source:** `03-business-logic.md` — "Any attempt to transition to a state not in the allowed map throws an exception. Any mutation of an event in SENT or COMPLETED status is rejected here before the edit even reaches the database."

```java
@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventStatusService {

    private final BillingEventRepository billingEventRepository;

    // Immutable transition map — package-visible for testing
    static final Map<BillingEventStatus, Set<BillingEventStatus>> ALLOWED_TRANSITIONS = Map.of(
        BillingEventStatus.IN_PROGRESS, Set.of(BillingEventStatus.SENT),
        BillingEventStatus.SENT,        Set.of(BillingEventStatus.COMPLETED, BillingEventStatus.ERROR),
        BillingEventStatus.ERROR,       Set.of(BillingEventStatus.SENT),
        BillingEventStatus.COMPLETED,   Set.of()   // terminal state
    );

    /**
     * Transitions a billing event to the requested status.
     * Throws IllegalStateException if the transition is not allowed.
     *
     * Called by: InvoiceGenerationService (→ SENT), FinvoiceTransmissionCallback (→ COMPLETED/ERROR)
     */
    public BillingEvent transitionTo(Long eventId, BillingEventStatus targetStatus) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        assertValidTransition(event.getStatus(), targetStatus, eventId);
        event.setStatus(targetStatus);
        return billingEventRepository.save(event);
    }

    /**
     * Immutability gate — called by BillingEventService before any field edit.
     * If the event is SENT or COMPLETED, editing is forbidden.
     *
     * Called by: BillingEventService.update(), BillingEventService.exclude(),
     *            BillingEventTransferService.transfer()
     */
    public void assertMutable(BillingEvent event) {
        if (event.getStatus() == BillingEventStatus.SENT
                || event.getStatus() == BillingEventStatus.COMPLETED) {
            throw new IllegalStateException(
                "BillingEvent " + event.getId() + " is " + event.getStatus()
                + " and cannot be modified. Use the credit-and-re-invoice flow for corrections."
            );
        }
    }

    // -----------------------------------------------------------------------
    private void assertValidTransition(BillingEventStatus from, BillingEventStatus to, Long id) {
        Set<BillingEventStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "Invalid status transition for BillingEvent " + id
                + ": " + from + " → " + to
                + ". Allowed: " + allowed
            );
        }
    }
}
```

---

### 11.3 Integration with PATCH /api/v1/billing-events/{id}

> **Requirement source:** `04-api-layer.md` — "PATCH /billing-events/{id}: Blocked if event status is SENT or COMPLETED."

No new controller endpoint is created here — the PATCH endpoint belongs to `BillingEventController` (introduced fully in Step 13). However, the status gate must be called at the top of any edit handler. The pattern is:

```java
// Inside BillingEventService.update(Long id, BillingEventUpdateRequest request, String currentUser):
BillingEvent event = billingEventRepository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));

billingEventStatusService.assertMutable(event);  // <-- gate here, before any field is touched

// ... apply changes, write audit log ...
```

If `assertMutable` throws, the exception propagates as a 409 Conflict via the global exception handler (see Step 6).

---

### 11.4 Global Exception Handler for Status Violations

**File:** `invoicing/src/main/java/com/example/invoicing/common/exception/GlobalExceptionHandler.java`

Add a handler for `IllegalStateException` to return a structured error body:

```java
@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("STATUS_TRANSITION_ERROR", ex.getMessage()));
}
```

**Request/Response example — attempting to edit a SENT event:**

`PATCH /api/v1/billing-events/42`
```json
{
  "quantity": 5.0,
  "reason": "Correcting driver entry"
}
```

Response `409 Conflict`:
```json
{
  "error": "STATUS_TRANSITION_ERROR",
  "message": "BillingEvent 42 is SENT and cannot be modified. Use the credit-and-re-invoice flow for corrections."
}
```

**Request/Response example — valid transition (SENT → COMPLETED):**

This transition is triggered programmatically (not by a user PATCH), but can also be triggered by an internal admin endpoint. When done internally:

`POST /api/v1/billing-events/42/transition`
```json
{
  "targetStatus": "COMPLETED"
}
```

Response `200 OK`:
```json
{
  "id": 42,
  "status": "COMPLETED",
  "lastModifiedAt": "2024-03-15T14:30:00Z"
}
```

Response `409 Conflict` (invalid transition):
```json
{
  "error": "STATUS_TRANSITION_ERROR",
  "message": "Invalid status transition for BillingEvent 42: COMPLETED → IN_PROGRESS. Allowed: []"
}
```

---

## Frontend

### 11.5 StatusBadge Component

**File:** `invoicing-fe/src/components/billing/StatusBadge.jsx`

> **Requirement source:** `01-domain-model.md` — four-state lifecycle (PD-297). Color coding makes the status immediately visible without reading text.

Color scheme:
- `IN_PROGRESS` → yellow/amber background
- `SENT` → blue background
- `COMPLETED` → green background
- `ERROR` → red background

```jsx
const STATUS_STYLES = {
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800 border border-yellow-300',
  SENT:        'bg-blue-100 text-blue-800 border border-blue-300',
  COMPLETED:   'bg-green-100 text-green-800 border border-green-300',
  ERROR:       'bg-red-100 text-red-800 border border-red-300',
}

const STATUS_LABELS = {
  IN_PROGRESS: 'In Progress',
  SENT:        'Sent',
  COMPLETED:   'Completed',
  ERROR:       'Error',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[status] ?? 'bg-gray-100 text-gray-800'}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}
```

---

### 11.6 Status Transition Buttons

**File:** `invoicing-fe/src/components/billing/StatusTransitionPanel.jsx`

Shows contextual action buttons based on current status. Only renders buttons for valid next states.

```jsx
const NEXT_ACTIONS = {
  IN_PROGRESS: [],                        // transitions from IN_PROGRESS happen automatically
  SENT:        ['COMPLETED', 'ERROR'],    // manually mark as confirmed or errored
  ERROR:       ['SENT'],                  // re-send after correction
  COMPLETED:   [],                        // terminal — no actions
}

export default function StatusTransitionPanel({ eventId, currentStatus, onTransitioned }) {
  const actions = NEXT_ACTIONS[currentStatus] ?? []

  if (actions.length === 0) return null

  const handleTransition = async (targetStatus) => {
    await axios.post(`/api/v1/billing-events/${eventId}/transition`, { targetStatus })
    onTransitioned()
  }

  return (
    <div className="flex gap-2">
      {actions.map((target) => (
        <button
          key={target}
          onClick={() => handleTransition(target)}
          className={target === 'ERROR' ? 'btn-danger' : 'btn-primary'}
        >
          Mark as {target}
        </button>
      ))}
    </div>
  )
}
```

The `StatusTransitionPanel` is rendered on the BillingEvent detail page (Step 13), below the event header.

---

### 11.7 API helper for transition

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const transitionBillingEventStatus = (id, targetStatus) =>
  axios.post(`/api/v1/billing-events/${id}/transition`, { targetStatus })
```

---

## Verification Checklist

1. Create a billing event — verify `status` is `IN_PROGRESS`.
2. Attempt to transition IN_PROGRESS → COMPLETED directly — verify `409 Conflict` with `STATUS_TRANSITION_ERROR`.
3. Transition IN_PROGRESS → SENT — verify `200 OK` and `status: "SENT"`.
4. Attempt to PATCH (edit) the now-SENT event — verify `409 Conflict` with the immutability message.
5. Transition SENT → ERROR — verify `200 OK` and `status: "ERROR"`.
6. Transition ERROR → SENT — verify `200 OK` and `status: "SENT"`.
7. Transition SENT → COMPLETED — verify `200 OK` and `status: "COMPLETED"`.
8. Attempt any further transition from COMPLETED — verify `409 Conflict`, allowed set is empty.
9. Frontend: `StatusBadge` with `status="IN_PROGRESS"` renders with yellow styling; `status="ERROR"` renders red.
10. Frontend: `StatusTransitionPanel` for a SENT event shows "Mark as COMPLETED" and "Mark as ERROR" buttons; for a COMPLETED event shows nothing.

---

## File Checklist

### Backend
- [ ] `billingevent/BillingEventStatusService.java`
- [ ] `common/exception/GlobalExceptionHandler.java` — add `IllegalStateException` handler
- [ ] `billingevent/BillingEventController.java` — add `POST /{id}/transition` endpoint (stub for now, expanded in Step 13)

### Frontend
- [ ] `src/components/billing/StatusBadge.jsx`
- [ ] `src/components/billing/StatusTransitionPanel.jsx`
- [ ] `src/api/billingEvents.js` — add `transitionBillingEventStatus`
