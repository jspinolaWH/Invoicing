# PD-297: Billing Event Status Lifecycle
**Jira:** https://ioteelab.atlassian.net/browse/PD-297
**Status:** Partially implemented — 5 gaps to close

---

## What is already working

- All 4 status enum values exist: `IN_PROGRESS`, `SENT`, `COMPLETED`, `ERROR`
  — `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventStatus.java`
- State machine with allowed transitions in `BillingEventStatusService.java` (lines 21–26)
- Edit lock enforced on `SENT` and `COMPLETED` — `BillingEventStatusService.assertMutable()` (lines 36–42), called from `BillingEventService.update()`
- `ERROR` events are editable (not blocked by `assertMutable`) — confirmed in both backend and frontend
- `ERROR → SENT` retry path exists in state machine and frontend `StatusTransitionPanel.jsx`
- Correction flow (credit + new event) for `SENT`/`COMPLETED` events via `POST /api/v1/billing-events/{id}/credit-transfer` → `BillingEventCreditTransferService.java`
- Status visible in list view (`BillingEventsPage.jsx` line 268), detail view (`BillingEventDetailPage.jsx` line 75), and both response DTOs
- List view filterable by status — frontend line 168, backend `BillingEventController.java` line 72
- `StatusBadge.jsx` renders colour-coded badges for all 4 statuses

---

## Gap 1 — No automatic status transitions on transmission (HIGH)

**Requirement FR-1.1 / FR-1.2:** When a billing file is transmitted, associated billing events must automatically move to `SENT` (success) or `ERROR` (failure). Currently `InvoiceTransmissionService.java` only updates the `Invoice` entity — it never touches `BillingEvent` status.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/InvoiceTransmissionService.java` | After successful transmission (lines 38–42), find all BillingEvents linked to the invoice and call `billingEventStatusService.transition(event, SENT)` for each |
| Service | `invoicing/src/main/java/com/example/invoicing/service/InvoiceTransmissionService.java` | In the failure path (add a `catch` block or error branch), call `billingEventStatusService.transition(event, ERROR)` for each linked event, passing the error reason (see Gap 3) |
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/invoice/Invoice.java` | Confirm or add a relationship back to its originating BillingEvents so `InvoiceTransmissionService` can resolve them |

---

## Gap 2 — No automatic COMPLETED transition on external confirmation (HIGH)

**Requirement FR-1.3 / FR-1.4:** `COMPLETED` must be set automatically when the external system confirms receipt. A manual fallback must exist when the callback does not arrive — behind an explicit confirmation dialog.

**Current state:** No webhook or callback controller exists anywhere in the codebase. `InvoiceTransmissionService.fetchStatus()` (lines 49–64) reads delivery status from the external system but only updates the `Invoice` entity. The only path to `COMPLETED` is the unguarded manual `/transition` endpoint.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Controller (new) | `invoicing/src/main/java/com/example/invoicing/controller/invoice/InvoiceCallbackController.java` | New controller: `POST /api/v1/invoices/callback` — receives external delivery confirmation, finds linked BillingEvents, transitions them to `COMPLETED` |
| Service | `invoicing/src/main/java/com/example/invoicing/service/InvoiceTransmissionService.java` | In `fetchStatus()`, when the external system returns a confirmed/delivered status, transition linked BillingEvents to `COMPLETED` |
| Frontend | `invoicing-fe/src/components/billing/StatusTransitionPanel.jsx` | Add a confirmation dialog before the `SENT → COMPLETED` manual transition (line 18–29 currently fires directly). Show: *"Mark as confirmed by external system? This cannot be undone."* |

---

## Gap 3 — No error reason field on BillingEvent (MEDIUM)

**Requirement FR-3.3:** When a billing event enters `ERROR`, the user must be able to see why — so they know what to correct before retrying.

**Current state:** `BillingEvent.java` has no `errorReason` or `errorMessage` field. The entity does have `rejectionReason` (line 127–128) but that is for office review rejection, not transmission failures. Neither response DTO exposes an error reason.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEvent.java` | Add `transmissionErrorReason` field (String, nullable) |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventStatusService.java` | Update `transition()` to accept an optional `reason` parameter; set `transmissionErrorReason` when transitioning to `ERROR`, clear it when transitioning away |
| Detail Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventDetailResponse.java` | Expose `transmissionErrorReason` in the response |
| Frontend | `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` | Display `transmissionErrorReason` prominently when `status === 'ERROR'` — above the edit form so the user sees it before making changes |
| DB migration | `invoicing/src/main/resources/db/migration/` (new file) | `ALTER TABLE billing_event ADD COLUMN transmission_error_reason TEXT` |

---

## Gap 4 — `SENT → ERROR` transition must be blocked (MEDIUM)

**Requirement FR-5:** The spec defines that `SENT → ERROR` must be rejected. `SENT` means the external system received the file — it cannot be "un-sent". ERROR must only be reached via automatic transmission failure (Gap 1), never manually.

**Current state:** The state machine at `BillingEventStatusService.java` line 23 allows `SENT → ERROR`. The frontend `StatusTransitionPanel.jsx` line 6 surfaces it as a user-clickable action. Any user can exploit this to bypass the edit lock on a `SENT` event.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventStatusService.java` line 23 | Remove `BillingEventStatus.ERROR` from the allowed transitions set for `SENT`. Change to: `BillingEventStatus.SENT, Set.of(BillingEventStatus.COMPLETED)` |
| Frontend | `invoicing-fe/src/components/billing/StatusTransitionPanel.jsx` line 6 | Remove `'ERROR'` from the `SENT` transitions array. Change to: `SENT: ['COMPLETED']` |

---

## Gap 5 — No confirmation dialog on manual SENT → COMPLETED (LOW)

**Requirement FR-1.4:** The manual COMPLETED fallback must require explicit confirmation — not a single click.

**Current state:** `StatusTransitionPanel.jsx` `handleTransition` (lines 18–29) calls the API directly with no confirmation prompt. A mis-click permanently completes an event.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/components/billing/StatusTransitionPanel.jsx` lines 18–29 | Before calling the transition API for `SENT → COMPLETED`, show a confirmation dialog: *"Mark as confirmed by external system? This action cannot be undone."* Only proceed if confirmed |

---

## Checklist

- [ ] Gap 1: Auto-transition BillingEvents to `SENT`/`ERROR` in `InvoiceTransmissionService.java` on transmission outcome
- [ ] Gap 2a: Create `InvoiceCallbackController` for external delivery confirmation webhook
- [ ] Gap 2b: Add confirmation dialog before manual `SENT → COMPLETED` in `StatusTransitionPanel.jsx`
- [ ] Gap 3a: Add `transmissionErrorReason` field to `BillingEvent` entity + migration
- [ ] Gap 3b: Expose `transmissionErrorReason` in `BillingEventDetailResponse` and display it in the detail page UI
- [ ] Gap 4: Remove `SENT → ERROR` from state machine in `BillingEventStatusService.java` and `StatusTransitionPanel.jsx`
- [ ] Gap 5: Confirmation dialog on `SENT → COMPLETED` in `StatusTransitionPanel.jsx`
