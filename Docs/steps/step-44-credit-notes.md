# Step 44 — Credit Notes

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: CreditNoteService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Credit Notes — POST /invoices/{id}/credit
- `Docs/structured_breakdown/01-domain-model.md` → Invoice (originalInvoice FK, invoiceType, internalComment)
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 1: "Audit Trail" — credit note creation must be logged

---

## Goal
Implement `CreditNoteService`, which creates a new `Invoice` entity with `invoiceType = CREDIT_NOTE` that references the original invoice and carries all line items with negated amounts. For full credit: all lines are negated. For partial credit: only the selected line items are negated. `customText` (customer-visible) and `internalComment` (mandatory, not shown to customer) are required inputs. Generates credit FINVOICE. Status lifecycle is identical to a regular invoice.

---

## Backend

### 44.1 CreditNoteService

**File:** `invoicing/src/main/java/com/example/invoicing/credit/CreditNoteService.java`

> **Requirement source:** `03-business-logic.md` — CreditNoteService

```java
@Service
@Transactional
public class CreditNoteService {

    /**
     * Issue a full or partial credit note against a sent/completed invoice.
     *
     * @param originalInvoiceId  The invoice being credited
     * @param request            Contains: creditType (FULL|PARTIAL), lineItemIds (for partial),
     *                           customText, internalComment (mandatory)
     * @throws CreditNoteValidationException if internalComment is blank or invoice is not creditable
     */
    public CreditNoteResponse credit(Long originalInvoiceId, CreditNoteRequest request) {
        // 1. Validate inputs
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new CreditNoteValidationException("internalComment is mandatory for credit notes");
        }

        Invoice original = invoiceRepository.findById(originalInvoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + originalInvoiceId));

        if (original.getStatus() != InvoiceStatus.SENT &&
            original.getStatus() != InvoiceStatus.COMPLETED) {
            throw new CreditNoteValidationException(
                "Credit notes can only be issued for SENT or COMPLETED invoices. " +
                "Current status: " + original.getStatus());
        }

        // 2. Determine which line items to credit
        List<InvoiceLineItem> linesToCredit;
        if (request.getCreditType() == CreditType.FULL) {
            linesToCredit = original.getLineItems();
        } else {
            Set<Long> ids = new HashSet<>(request.getLineItemIds());
            linesToCredit = original.getLineItems().stream()
                .filter(l -> ids.contains(l.getId()))
                .toList();
            if (linesToCredit.isEmpty()) {
                throw new CreditNoteValidationException("No valid line items selected for partial credit");
            }
        }

        // 3. Build credit note invoice
        Invoice creditNote = new Invoice();
        creditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);
        creditNote.setOriginalInvoice(original);
        creditNote.setCustomer(original.getCustomer());
        creditNote.setLanguage(original.getLanguage());
        creditNote.setInvoicingMode(original.getInvoicingMode());
        creditNote.setReverseChargeVat(original.isReverseChargeVat());
        creditNote.setCustomText(request.getCustomText());
        creditNote.setInternalComment(request.getInternalComment());  // NOT sent to customer
        creditNote.setTemplateCode(original.getTemplateCode());
        creditNote.setStatus(InvoiceStatus.DRAFT);
        creditNote.setInvoiceDate(LocalDate.now());

        // 4. Negate line items
        for (InvoiceLineItem originalLine : linesToCredit) {
            InvoiceLineItem creditLine = new InvoiceLineItem();
            creditLine.setInvoice(creditNote);
            creditLine.setProduct(originalLine.getProduct());
            creditLine.setDescription(originalLine.getDescription());
            creditLine.setQuantity(originalLine.getQuantity().negate());
            creditLine.setUnitPrice(originalLine.getUnitPrice());
            creditLine.setVatRate(originalLine.getVatRate());
            creditLine.setNetAmount(originalLine.getNetAmount().negate());
            creditLine.setGrossAmount(originalLine.getGrossAmount().negate());
            creditLine.setLegalClassification(originalLine.getLegalClassification());
            creditLine.setAccountingAccount(originalLine.getAccountingAccount());
            creditLine.setCostCenter(originalLine.getCostCenter());
            creditNote.getLineItems().add(creditLine);
        }

        // 5. Calculate totals (negative)
        creditNote.setNetAmount(creditNote.getLineItems().stream()
            .map(InvoiceLineItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        creditNote.setGrossAmount(creditNote.getLineItems().stream()
            .map(InvoiceLineItem::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        creditNote.setVatAmount(creditNote.getGrossAmount().subtract(creditNote.getNetAmount()));

        // 6. Assign invoice number from the credit note series
        String creditNumber = invoiceNumberingService.assignNextForCreditNote(
            original.getCustomer().getId());
        creditNote.setInvoiceNumber(creditNumber);

        // 7. Build credit FINVOICE
        String finvoiceXml = finvoiceBuilderService.build(creditNote, sellerDetails);
        creditNote.setStatus(InvoiceStatus.READY);

        Invoice saved = invoiceRepository.save(creditNote);

        // 8. Audit log
        auditLogService.logCreditNoteCreation(originalInvoiceId, saved.getId(),
            request.getCreditType(), request.getInternalComment());

        return CreditNoteResponse.from(saved, original);
    }
}
```

---

### 44.2 CreditType Enum

```java
public enum CreditType {
    FULL,     // All line items negated
    PARTIAL   // Only selected line items negated
}
```

---

### 44.3 CreditNoteController

**File:** `invoicing/src/main/java/com/example/invoicing/credit/CreditNoteController.java`

Base path: `/api/v1/invoices`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/{id}/credit` | Issue full or partial credit note | INVOICING_USER |

**POST /api/v1/invoices/{id}/credit request:**
```json
{
  "creditType": "PARTIAL",
  "lineItemIds": [5001, 5002],
  "customText": "Credit issued for billing error on container service.",
  "internalComment": "Customer disputed charges. Confirmed overcounting by driver. Approved by supervisor."
}
```

For full credit, `lineItemIds` is omitted:
```json
{
  "creditType": "FULL",
  "customText": "Full credit issued. Please disregard original invoice.",
  "internalComment": "Invoice generated for wrong customer. Transferred via step-46."
}
```

**POST response (201 Created):**
```json
{
  "creditNoteId": 2001,
  "creditNoteNumber": "KH2024000001",
  "originalInvoiceId": 1001,
  "originalInvoiceNumber": "PL2024000042",
  "creditType": "PARTIAL",
  "netAmount": -80.00,
  "grossAmount": -99.20,
  "status": "READY",
  "lineItems": [
    {
      "description": "Waste container emptying",
      "quantity": -4,
      "unitPrice": 20.00,
      "vatRate": 24.00,
      "netAmount": -80.00,
      "grossAmount": -99.20
    }
  ]
}
```

**Error responses:**
- `400 Bad Request` — `internalComment` is blank.
- `409 Conflict` — invoice is not in SENT or COMPLETED status.
- `404 Not Found` — invoice ID does not exist.

---

## Frontend

### 44.4 Credit Note Form

**File:** `invoicing-fe/src/pages/invoices/CreditNoteForm.jsx`

**Trigger:** "Issue Credit Note" button on `InvoiceDetailPage`, visible only when invoice status is SENT or COMPLETED.

Components:
- **CreditTypeSelector** — radio buttons: "Full Credit" / "Partial Credit".
- **LineItemCheckboxList** — visible only when `creditType === 'PARTIAL'`. Shows all line items with checkboxes. At least one must be checked.
- **CustomTextField** — text area labelled "Customer-facing message (visible on credit note)". Optional.
- **InternalCommentField** — text area labelled "Internal reason (not sent to customer)". **Required** — red asterisk, validation prevents submit if blank.
- **SubmitCreditButton** — submits `POST /api/v1/invoices/{id}/credit`. On success, navigates to the new credit note's detail page.

**Warning notice:** "The internal comment will NOT be sent to the customer. It is for internal records only."

**API calls (additions to `src/api/invoices.js`):**
```js
export const issueCreditNote = (invoiceId, data) =>
  axios.post(`/api/v1/invoices/${invoiceId}/credit`, data)
```

---

## Verification Checklist

1. `POST /api/v1/invoices/{id}/credit` with `creditType: FULL` → credit note invoice created with all line items negated; original invoice's `creditNotes` list updated.
2. Partial credit with `lineItemIds: [5001]` → only line 5001 is negated; other lines not included.
3. Attempt credit with blank `internalComment` → HTTP 400 "internalComment is mandatory."
4. Attempt credit on DRAFT invoice → HTTP 409 "Credit notes can only be issued for SENT or COMPLETED invoices."
5. Credit note `netAmount` = negative sum of credited line `netAmount` values.
6. Credit note FINVOICE XML is generated and passes XSD validation with negative amounts.
7. Credit note `invoiceType = CREDIT_NOTE` and `originalInvoice.id = originalInvoiceId`.
8. `internalComment` is stored in the database but is NOT present in any API response JSON (verify the DTO excludes it from the customer-facing response — it should be in admin-only views only).
9. Audit log entry created: `logCreditNoteCreation(originalId, creditNoteId, creditType, reason)`.
10. Open `InvoiceDetailPage` in FE for a SENT invoice — "Issue Credit Note" button visible; form prevents submit with blank internal comment; on success navigates to new credit note page.

---

## File Checklist

### Backend
- [ ] `credit/CreditNoteService.java`
- [ ] `credit/CreditNoteController.java`
- [ ] `credit/CreditType.java` (enum)
- [ ] `credit/CreditNoteValidationException.java`
- [ ] `credit/dto/CreditNoteRequest.java`
- [ ] `credit/dto/CreditNoteResponse.java`

### Frontend
- [ ] `src/pages/invoices/CreditNoteForm.jsx`
- [ ] `src/pages/invoices/components/CreditTypeSelector.jsx`
- [ ] `src/pages/invoices/components/LineItemCheckboxList.jsx`
- [ ] `src/api/invoices.js` — add `issueCreditNote()`
