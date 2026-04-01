# Step 43 — Invoice Cancellation and Schedule Send

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceCancellationService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoice Runs — POST cancel, POST schedule-send
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: BILLING_MANAGER for cancellation after transmission

---

## Goal
Implement `InvoiceCancellationService` covering two scenarios: (1) cancel a run/invoice before transmission — reverting statuses, releasing invoice numbers, removing locks; (2) cancel after transmission by recalling FINVOICE from the external system (only if the integration supports it). Also implement the scheduled send: `POST /api/v1/invoice-runs/{id}/schedule-send` stores a `sendAt` datetime and a scheduler triggers transmission. BILLING_MANAGER role required for post-transmission cancellation.

---

## Backend

### 43.1 InvoiceCancellationService

**File:** `invoicing/src/main/java/com/example/invoicing/cancellation/InvoiceCancellationService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceCancellationService

```java
@Service
@Transactional
public class InvoiceCancellationService {

    /**
     * Cancel an invoice run before any invoice has been transmitted to the external system.
     * Reverts all invoice statuses to CANCELLED, releases invoice numbers, removes all run locks.
     * BILLING_MANAGER role required (enforced at controller level).
     */
    public CancellationResult cancelRun(Long runId, String reason, String cancelledBy) {
        InvoiceRun run = runRepository.findById(runId).orElseThrow();

        if (run.getStatus() == InvoiceRunStatus.SENT) {
            throw new CannotCancelException(
                "Run has already been fully transmitted. Use recall for individual invoices.");
        }

        List<Invoice> invoices = invoiceRepository.findByInvoiceRunId(runId);
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.SENT ||
                invoice.getStatus() == InvoiceStatus.COMPLETED) {
                // Individual invoice already transmitted — skip; log for manual action
                log.warn("Invoice {} already SENT — cannot auto-cancel; manual recall required",
                    invoice.getId());
                continue;
            }
            // Release invoice number back to the released pool (never re-issued)
            if (invoice.getInvoiceNumber() != null) {
                invoiceNumberingService.releaseNumber(invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumberSeries().getId());
            }
            // Revert BillingEvents to IN_PROGRESS
            for (BillingEvent event : invoice.getBillingEvents()) {
                billingEventStatusService.revertToInProgress(event.getId());
            }
            invoice.setStatus(InvoiceStatus.CANCELLED);
        }

        // Release all run locks
        lockService.releaseLocksForRun(runId);

        run.setStatus(InvoiceRunStatus.CANCELLED);
        run.setCancellationReason(reason);
        run.setCancelledBy(cancelledBy);
        run.setCancelledAt(Instant.now());
        runRepository.save(run);

        return new CancellationResult(runId, invoices.size(), "Run cancelled successfully");
    }

    /**
     * Cancel a single invoice (called from the invoice detail page if BILLING_MANAGER).
     */
    public void cancelInvoice(Long invoiceId, String reason, String cancelledBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();

        if (invoice.getStatus() == InvoiceStatus.SENT ||
            invoice.getStatus() == InvoiceStatus.COMPLETED) {
            throw new CannotCancelException(
                "Invoice has been transmitted. Use /recall endpoint to retract from external system.");
        }

        if (invoice.getInvoiceNumber() != null) {
            invoiceNumberingService.releaseNumber(invoice.getInvoiceNumber(),
                invoice.getInvoiceNumberSeries().getId());
        }
        for (BillingEvent event : invoice.getBillingEvents()) {
            billingEventStatusService.revertToInProgress(event.getId());
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }
}
```

---

### 43.2 Schedule Send

> **Requirement source:** `03-business-logic.md` — "Supports a `scheduledSendAt` timestamp on the InvoiceRun."

**In `InvoiceRunService`:**
```java
public InvoiceRunResponse scheduleSend(Long runId, Instant sendAt) {
    InvoiceRun run = runRepository.findById(runId).orElseThrow();
    if (run.getStatus() != InvoiceRunStatus.COMPLETED) {
        throw new InvalidRunStateException("Can only schedule send for COMPLETED runs");
    }
    run.setScheduledSendAt(sendAt);
    return InvoiceRunResponse.from(runRepository.save(run));
}
```

**Scheduler (from step 41):**
```java
@Scheduled(fixedDelay = 60_000)
public void triggerScheduledSends() {
    runRepository.findScheduledForSend(Instant.now())
        .forEach(run -> transmissionService.transmitRun(run.getId()));
}
```

---

### 43.3 Exception Types

**File:** `invoicing/src/main/java/com/example/invoicing/cancellation/CannotCancelException.java`
```java
public class CannotCancelException extends RuntimeException {
    // Maps to HTTP 409 Conflict in GlobalExceptionHandler
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvalidRunStateException.java`
```java
public class InvalidRunStateException extends RuntimeException {
    // Maps to HTTP 400 Bad Request
}
```

---

### 43.4 Controller Endpoints

**Additions to `InvoiceRunController`** (step 38):

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoice-runs/{id}/cancel` | Cancel a run and revert invoices | BILLING_MANAGER |
| POST | `/api/v1/invoice-runs/{id}/schedule-send` | Set a future transmission time | INVOICING_USER |
| POST | `/api/v1/invoice-runs/{id}/send` | Manually trigger immediate transmission | INVOICING_USER |

**POST cancel request:**
```json
{
  "reason": "Incorrect period selected. Will re-run with correct dates."
}
```

**POST cancel response:**
```json
{
  "runId": 42,
  "invoicesCancelled": 248,
  "message": "Run cancelled successfully"
}
```

**POST schedule-send request:**
```json
{
  "sendAt": "2024-02-05T09:00:00Z"
}
```

**POST schedule-send response:**
```json
{
  "runId": 42,
  "status": "COMPLETED",
  "scheduledSendAt": "2024-02-05T09:00:00Z"
}
```

---

## Frontend

### 43.5 Cancel Button and Schedule Send Date Picker

**Extension to `RunActionBar.jsx`** (step 38):

**Cancel Button:**
- Visible only when run status is not SENT, CANCELLED, or ERROR.
- Visible only to users with BILLING_MANAGER role.
- Clicking opens a confirmation modal: "Cancel Run — This will revert X invoices to Draft status and release all invoice numbers. Enter reason:"
- Input field: reason (required).
- Confirm button calls `POST /api/v1/invoice-runs/{id}/cancel`.

**Schedule Send:**
- DateTime picker (date + time) enabled when run status = COMPLETED.
- "Schedule Send" button calls `POST /api/v1/invoice-runs/{id}/schedule-send`.
- After scheduling, shows: "Scheduled for [datetime]" with a "Cancel Schedule" link (clears `scheduledSendAt` via `PUT` — or another cancel endpoint).

**Send Now:**
- Button enabled when run status = COMPLETED.
- Confirms with "Send all invoices now?" dialog.
- Calls `POST /api/v1/invoice-runs/{id}/send`.

**API calls (additions to `src/api/invoiceRuns.js`):**
```js
export const cancelRun = (id, reason) =>
  axios.post(`/api/v1/invoice-runs/${id}/cancel`, { reason })

export const scheduleSend = (id, sendAt) =>
  axios.post(`/api/v1/invoice-runs/${id}/schedule-send`, { sendAt })

export const triggerSend = (id) =>
  axios.post(`/api/v1/invoice-runs/${id}/send`)
```

---

## Verification Checklist

1. `POST /api/v1/invoice-runs/{id}/cancel` by BILLING_MANAGER → run status = CANCELLED; all DRAFT/READY invoices in the run have status = CANCELLED; all `BillingEvent` statuses reverted to IN_PROGRESS.
2. `POST cancel` by INVOICING_USER → HTTP 403 Forbidden.
3. Invoice number release: after cancellation, the released invoice numbers appear in the `InvoiceNumberSeries.releasedNumbers` pool (not re-used).
4. Run already SENT: attempt cancel → HTTP 409 "Run has already been fully transmitted."
5. Mixed run (some invoices SENT, some DRAFT): cancel → DRAFT invoices cancelled; SENT invoices logged for manual action; no silent failure.
6. `POST schedule-send` with a past datetime → HTTP 400 "sendAt must be in the future."
7. `findScheduledForSend(Instant.now())` at the scheduled time returns the run → `triggerScheduledSends()` triggers transmission.
8. `POST send` when status ≠ COMPLETED → HTTP 400 "Run must be COMPLETED before sending."
9. Open `RunActionBar` in FE as BILLING_MANAGER — Cancel button visible; clicking opens confirmation modal.
10. Schedule send: date picker visible when status = COMPLETED; after scheduling, "Scheduled for [datetime]" message displayed.

---

## File Checklist

### Backend
- [ ] `cancellation/InvoiceCancellationService.java`
- [ ] `cancellation/CancellationResult.java`
- [ ] `cancellation/CannotCancelException.java`
- [ ] `run/InvalidRunStateException.java`
- [ ] `run/InvoiceRunController.java` — add cancel, schedule-send, send endpoints (extends step 38)
- [ ] `common/exception/GlobalExceptionHandler.java` — add 409 handler for CannotCancelException

### Frontend
- [ ] `src/pages/runs/components/RunActionBar.jsx` — cancel modal, schedule picker, send-now button (extends step 38)
- [ ] `src/api/invoiceRuns.js` — add cancelRun, scheduleSend, triggerSend (extends step 38)
