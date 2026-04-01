# Step 46 — Billed Event Correction

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BilledEventCorrectionService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices — POST /invoices/{id}/correct
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT"

---

## Goal
Implement `BilledEventCorrectionService`, which handles corrections to already-invoiced events. The flow is always two steps: (1) issue a credit note for the original invoice, (2) copy all `BillingEvent` records from the original invoice to a new IN_PROGRESS state so they can be edited and re-invoiced. The correction can target the same customer or a different customer. Events are copied — not moved — so the original audit trail is preserved.

---

## Backend

### 46.1 BilledEventCorrectionService

**File:** `invoicing/src/main/java/com/example/invoicing/correction/BilledEventCorrectionService.java`

> **Requirement source:** `03-business-logic.md` — BilledEventCorrectionService

```java
@Service
@Transactional
public class BilledEventCorrectionService {

    /**
     * Correct a sent/completed invoice.
     * Step 1: Issue a full (or partial) credit note for the original invoice.
     * Step 2: Copy the selected BillingEvents to a new IN_PROGRESS state for the target customer.
     *
     * @param originalInvoiceId  The SENT or COMPLETED invoice to correct
     * @param request            targetCustomerId (null = same customer), lineItemIds (null = all),
     *                           internalComment (mandatory)
     */
    public CorrectionResult correct(Long originalInvoiceId, CorrectionRequest request) {
        // 1. Issue credit note for the original invoice
        CreditNoteRequest creditRequest = new CreditNoteRequest();
        creditRequest.setCreditType(request.getLineItemIds() != null
            ? CreditType.PARTIAL : CreditType.FULL);
        creditRequest.setLineItemIds(request.getLineItemIds());
        creditRequest.setCustomText(request.getCustomText());
        creditRequest.setInternalComment(request.getInternalComment());

        CreditNoteResponse creditNote = creditNoteService.credit(originalInvoiceId, creditRequest);

        // 2. Load the original billing events from the invoice's line items
        Invoice original = invoiceRepository.findById(originalInvoiceId).orElseThrow();
        Long targetCustomerId = request.getTargetCustomerId() != null
            ? request.getTargetCustomerId()
            : original.getCustomer().getId();

        List<BillingEvent> originalEvents = resolveEvents(original, request.getLineItemIds());

        // 3. Copy each event to a new IN_PROGRESS state for the target customer
        List<BillingEvent> copiedEvents = originalEvents.stream()
            .map(event -> copyEventForCorrection(event, targetCustomerId))
            .toList();
        billingEventRepository.saveAll(copiedEvents);

        // 4. Return the correction result with credit note ID and copied event IDs
        return new CorrectionResult(
            originalInvoiceId,
            creditNote.getCreditNoteId(),
            copiedEvents.stream().map(BillingEvent::getId).toList(),
            targetCustomerId
        );
    }

    private BillingEvent copyEventForCorrection(BillingEvent original, Long targetCustomerId) {
        BillingEvent copy = new BillingEvent();
        // Copy all fields
        copy.setEventDate(original.getEventDate());
        copy.setProduct(original.getProduct());
        copy.setQuantity(original.getQuantity());
        copy.setWeight(original.getWeight());
        copy.setWastePrice(original.getWastePrice());
        copy.setTransportPrice(original.getTransportPrice());
        copy.setUnitPrice(original.getUnitPrice());
        copy.setVatRate(original.getVatRate());
        copy.setLegalClassification(original.getLegalClassification());
        copy.setAccountingAccount(original.getAccountingAccount());
        copy.setCostCenter(original.getCostCenter());
        copy.setMunicipality(original.getMunicipality());
        copy.setLocation(original.getLocation());
        copy.setComment("Correction copy of event " + original.getId());
        // Target customer (may differ from original)
        copy.setCustomer(customerRepository.getReferenceById(targetCustomerId));
        // New event starts fresh in IN_PROGRESS
        copy.setStatus(BillingEventStatus.IN_PROGRESS);
        copy.setExcluded(false);
        copy.setRequiresOfficeReview(false);
        // Link to correction origin for audit trail
        copy.setCorrectedFromEventId(original.getId());
        return copy;
    }
}
```

---

### 46.2 CorrectionRequest / CorrectionResult

**File:** `invoicing/src/main/java/com/example/invoicing/correction/CorrectionRequest.java`

```java
public class CorrectionRequest {
    private Long targetCustomerId;      // null = same customer as original invoice
    private List<Long> lineItemIds;     // null = all line items (full correction)
    private String customText;          // customer-visible text on credit note
    private String internalComment;     // mandatory — reason for correction
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/correction/CorrectionResult.java`

```java
public class CorrectionResult {
    private Long originalInvoiceId;
    private Long creditNoteId;
    private List<Long> copiedEventIds;  // new IN_PROGRESS events ready for editing
    private Long targetCustomerId;
}
```

---

### 46.3 BillingEvent — correctedFromEventId field

Add to `BillingEvent` entity:
```java
@Column(name = "corrected_from_event_id")
private Long correctedFromEventId;     // non-null when this is a correction copy
```

This maintains the audit trail from the copied event back to the original.

---

### 46.4 CorrectionController

**File:** `invoicing/src/main/java/com/example/invoicing/correction/CorrectionController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/{id}/correct` | Issue credit note and copy events for re-invoicing | INVOICING_USER |

**POST /api/v1/invoices/{id}/correct request:**
```json
{
  "targetCustomerId": null,
  "lineItemIds": null,
  "customText": "Corrected invoice will follow.",
  "internalComment": "Driver recorded container emptying to wrong address. Correcting to customer 123456."
}
```

To correct to a different customer:
```json
{
  "targetCustomerId": 2002,
  "lineItemIds": null,
  "customText": "Corrected invoice issued to correct customer.",
  "internalComment": "Events were assigned to previous tenant. New tenant takes over from 2024-02-01."
}
```

**POST response (201 Created):**
```json
{
  "originalInvoiceId": 1001,
  "creditNoteId": 2001,
  "copiedEventIds": [9001, 9002, 9003],
  "targetCustomerId": 1001,
  "message": "Credit note issued and 3 events copied for re-invoicing. Edit the copied events then generate a new invoice."
}
```

---

## Frontend

### 46.5 Correct Invoice Button and Form

**Extension to `InvoiceDetailPage.jsx`** (step 29):

**"Correct Invoice" button** — visible only when `status === 'SENT' || status === 'COMPLETED'`.

**File:** `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx`

Components:
- **TargetCustomerSelector** — "Same customer" (radio) or "Different customer" (radio + customer search field). Defaults to "Same customer".
- **LineSelectionToggle** — "Correct all lines" / "Select specific lines". When "Select specific lines": renders line checkboxes.
- **CreditNoteFields** — `customText` (optional) and `internalComment` (required).
- **SubmitButton** — labelled "Issue Credit & Copy Events". On success:
  1. Shows a success banner: "Credit note issued. 3 events are now ready for editing."
  2. Provides a link "View Copied Events" → navigates to the billing events list filtered by the copied event IDs.

**Copied Events editing flow:**
- User navigates to billing events list; the copied events appear in IN_PROGRESS status with a note "Correction copy of event #XXXX".
- User edits the events (quantity, price, customer, etc.) as needed.
- User then generates a new invoice from these events (standard flow via step 34).

**API calls (additions to `src/api/invoices.js`):**
```js
export const correctInvoice = (invoiceId, data) =>
  axios.post(`/api/v1/invoices/${invoiceId}/correct`, data)
```

---

## Verification Checklist

1. `POST /api/v1/invoices/{id}/correct` on a SENT invoice → credit note created (step 44 logic); N new BillingEvent copies created with status IN_PROGRESS.
2. Copied events have `correctedFromEventId` pointing to the original event IDs.
3. Original BillingEvent records retain status SENT/COMPLETED — they are NOT reverted.
4. Correction to a different customer: copied events have `customer.id = targetCustomerId`.
5. Partial correction with `lineItemIds`: only the specified lines are credited; only the corresponding events are copied.
6. `internalComment` blank → HTTP 400 (via CreditNoteService validation).
7. Correction on a DRAFT invoice → HTTP 409 (must be SENT or COMPLETED).
8. Original invoice and the credit note both remain visible on the original customer's account.
9. Open `InvoiceDetailPage` for a SENT invoice in FE — "Correct Invoice" button visible; form allows customer selection; submit creates credit note and copied events.
10. "View Copied Events" link navigates to billing events list showing the 3 new IN_PROGRESS events with correction note in description.

---

## File Checklist

### Backend
- [ ] `correction/BilledEventCorrectionService.java`
- [ ] `correction/CorrectionController.java`
- [ ] `correction/CorrectionRequest.java`
- [ ] `correction/CorrectionResult.java`
- [ ] `event/BillingEvent.java` — add `correctedFromEventId` field (extends step 10)

### Frontend
- [ ] `src/pages/invoices/CorrectInvoiceForm.jsx`
- [ ] `src/pages/invoices/components/TargetCustomerSelector.jsx`
- [ ] `src/api/invoices.js` — add `correctInvoice()` (extends step 44/45)
