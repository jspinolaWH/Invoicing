# Step 54 — Invoice Recall

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: "Invoice recall" — only if supported by external system
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices (recall implied by cancel after transmission)
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: BILLING_MANAGER required for recall

---

## Goal
Implement invoice recall: `POST /api/v1/invoices/{id}/recall` calls `ExternalInvoicingClient.recall()` to retract a transmitted invoice from the external invoicing system. On success: invoice status → CANCELLED. On failure (unsupported by the external system): return HTTP 422 Unprocessable Entity with a clear message. BILLING_MANAGER role required. FE shows a "Recall" button on SENT/COMPLETED invoices.

---

## Backend

### 54.1 InvoiceRecallService

**File:** `invoicing/src/main/java/com/example/invoicing/cancellation/InvoiceRecallService.java`

> **Requirement source:** `05-integration-layer.md` — "Recall is only possible if the external system's API supports it. WasteHero calls the recall endpoint with the invoice number."

```java
@Service
@Transactional
public class InvoiceRecallService {

    /**
     * Recall a transmitted invoice from the external invoicing system.
     * BILLING_MANAGER role required (enforced at controller level).
     *
     * @throws RecallNotSupportedException  if the external system does not support recall (HTTP 422)
     * @throws RecallFailedException        if the recall attempt failed for a technical reason
     */
    public RecallResult recall(Long invoiceId, String recalledBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice " + invoiceId + " not found"));

        // Only SENT and COMPLETED invoices can be recalled
        if (invoice.getStatus() != InvoiceStatus.SENT &&
            invoice.getStatus() != InvoiceStatus.COMPLETED) {
            throw new InvalidInvoiceStateException(
                "Only SENT or COMPLETED invoices can be recalled. Current status: "
                + invoice.getStatus());
        }

        if (invoice.getInvoiceNumber() == null) {
            throw new InvalidInvoiceStateException("Invoice has no invoice number — cannot recall");
        }

        // Call external system
        com.example.invoicing.integration.RecallResult externalResult =
            externalInvoicingClient.recall(invoice.getInvoiceNumber());

        if (externalResult.isUnsupported()) {
            throw new RecallNotSupportedException(
                "The external invoicing system does not support invoice recall. " +
                "Please contact the operator directly to cancel invoice "
                + invoice.getInvoiceNumber() + ".");
        }

        if (!externalResult.isSuccess()) {
            throw new RecallFailedException(
                "Recall failed: " + externalResult.getErrorMessage());
        }

        // Successful recall → CANCELLED
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        // Write audit log
        auditLogService.logRecall(invoiceId, invoice.getInvoiceNumber(), recalledBy);

        return new RecallResult(invoiceId, invoice.getInvoiceNumber(), InvoiceStatus.CANCELLED);
    }
}
```

---

### 54.2 Exception Types

```java
public class RecallNotSupportedException extends RuntimeException { }
    // → HTTP 422 Unprocessable Entity

public class RecallFailedException extends RuntimeException { }
    // → HTTP 502 Bad Gateway (external system error)
```

In `GlobalExceptionHandler`:
```java
@ExceptionHandler(RecallNotSupportedException.class)
public ResponseEntity<ErrorResponse> handleRecallNotSupported(RecallNotSupportedException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse("RECALL_NOT_SUPPORTED", ex.getMessage()));
}

@ExceptionHandler(RecallFailedException.class)
public ResponseEntity<ErrorResponse> handleRecallFailed(RecallFailedException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("RECALL_FAILED", ex.getMessage()));
}
```

---

### 54.3 InvoiceRecallController

**File:** `invoicing/src/main/java/com/example/invoicing/cancellation/InvoiceRecallController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/{id}/recall` | Recall a transmitted invoice | BILLING_MANAGER |

**POST /api/v1/invoices/{id}/recall request body:** empty (no body required)

**POST response (200 OK — success):**
```json
{
  "invoiceId": 1001,
  "invoiceNumber": "PL2024000042",
  "status": "CANCELLED",
  "message": "Invoice recalled successfully from external invoicing system."
}
```

**POST response (422 — not supported):**
```json
{
  "error": "RECALL_NOT_SUPPORTED",
  "message": "The external invoicing system does not support invoice recall. Please contact the operator directly to cancel invoice PL2024000042."
}
```

**POST response (502 — external failure):**
```json
{
  "error": "RECALL_FAILED",
  "message": "Recall failed: External system returned 409 — invoice already paid."
}
```

---

## Frontend

### 54.4 Recall Button on Invoice Detail

**Extension to `InvoiceDetailPage.jsx`** (step 29):

**"Recall Invoice" button:**
- Visible only when `status === 'SENT' || status === 'COMPLETED'`.
- Visible only to users with BILLING_MANAGER role.
- Clicking opens a confirmation dialog: "Recall Invoice — This will attempt to retract invoice [number] from the external invoicing system. Are you sure?"
- On confirm: calls `POST /api/v1/invoices/{id}/recall`.

**Outcome handling:**
- On 200 success: status badge updates to CANCELLED; success toast "Invoice recalled successfully."
- On 422 (not supported): amber warning dialog: "Recall not supported. Please cancel this invoice directly with the invoicing operator. Invoice number: [number]."
- On 502 (failure): red error dialog with the specific error message.

**API calls (additions to `src/api/invoices.js`):**
```js
export const recallInvoice = (invoiceId) =>
  axios.post(`/api/v1/invoices/${invoiceId}/recall`)
```

---

## Verification Checklist

1. Mock external system to support recall → `POST /api/v1/invoices/{id}/recall` returns 200; invoice status = CANCELLED.
2. Mock external system to return 405 Method Not Allowed → WasteHero returns HTTP 422 with "RECALL_NOT_SUPPORTED" message.
3. Mock external system to return 500 → WasteHero returns HTTP 502 with "RECALL_FAILED" message.
4. Attempt recall by INVOICING_USER → HTTP 403 Forbidden.
5. Attempt recall on DRAFT invoice → HTTP 400 "Only SENT or COMPLETED invoices can be recalled."
6. Attempt recall on already CANCELLED invoice → HTTP 400.
7. Successful recall: audit log entry created with invoice ID, invoice number, and recalledBy user.
8. After recall, the `InvoiceNumberSeries` released pool: the recalled invoice's number is added (cannot be re-issued).
9. Open `InvoiceDetailPage` for a SENT invoice as BILLING_MANAGER — Recall button visible. As INVOICING_USER — Recall button not shown.
10. Recall failure (422): FE shows amber warning with invoice number for manual action; does NOT show generic error toast.

---

## File Checklist

### Backend
- [ ] `cancellation/InvoiceRecallService.java`
- [ ] `cancellation/InvoiceRecallController.java`
- [ ] `cancellation/RecallNotSupportedException.java`
- [ ] `cancellation/RecallFailedException.java`
- [ ] `cancellation/RecallResult.java`
- [ ] `common/exception/GlobalExceptionHandler.java` — add 422 and 502 handlers for recall exceptions

### Frontend
- [ ] `src/pages/invoices/components/RecallButton.jsx`
- [ ] `src/api/invoices.js` — add `recallInvoice()` (extends step 29)
