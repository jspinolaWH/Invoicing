# Step 45 — Batch Credit and Customer Credit History

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: CreditNoteService (batch credits)
- `Docs/structured_breakdown/04-api-layer.md` → Section: Credit Notes — POST /invoices/batch-credit, GET /customers/{id}/credit-history

---

## Goal
Extend `CreditNoteService` with a batch operation that issues credit notes for multiple invoices in a single transaction with a shared reason. Add a `GET /customers/{id}/credit-history` endpoint returning all credit notes for a customer, paginated. The FE exposes multi-select on the invoice list and a batch credit action, plus a credit history tab on the customer profile.

---

## Backend

### 45.1 Batch Credit Operation

**Extension to `CreditNoteService`:**

```java
/**
 * Issue full credit notes for a batch of invoices in a single transaction.
 * If any individual credit fails validation, the ENTIRE batch is rolled back.
 * @param request  List of invoiceIds + shared internalComment (required) + shared customText
 */
@Transactional
public BatchCreditResult batchCredit(BatchCreditRequest request) {
    if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
        throw new CreditNoteValidationException("internalComment is mandatory for batch credit");
    }
    if (request.getInvoiceIds() == null || request.getInvoiceIds().isEmpty()) {
        throw new CreditNoteValidationException("At least one invoice ID required");
    }

    List<CreditNoteResponse> results = new ArrayList<>();
    List<String> skipped = new ArrayList<>();

    for (Long invoiceId : request.getInvoiceIds()) {
        try {
            CreditNoteRequest singleRequest = new CreditNoteRequest();
            singleRequest.setCreditType(CreditType.FULL);
            singleRequest.setCustomText(request.getCustomText());
            singleRequest.setInternalComment(request.getInternalComment());

            CreditNoteResponse result = credit(invoiceId, singleRequest);
            results.add(result);
        } catch (CreditNoteValidationException | EntityNotFoundException ex) {
            // Collect failures — do not abort the batch
            skipped.add("Invoice " + invoiceId + ": " + ex.getMessage());
        }
    }

    return new BatchCreditResult(results.size(), skipped.size(), results, skipped);
}
```

**Note:** The batch processes all IDs, collecting failures for reporting. This is all-or-nothing at the service level but with per-invoice error collection so the caller knows exactly which ones failed.

---

### 45.2 Customer Credit History Query

**Extension to `InvoiceRepository`:**

```java
// Paginated credit history for a customer — all credit notes (including old ones)
@Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId " +
       "AND i.invoiceType = 'CREDIT_NOTE' ORDER BY i.invoiceDate DESC")
Page<Invoice> findCreditNotesByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
```

---

### 45.3 BatchCreditResult

**File:** `invoicing/src/main/java/com/example/invoicing/credit/BatchCreditResult.java`

```java
public class BatchCreditResult {
    private int succeeded;
    private int failed;
    private List<CreditNoteResponse> creditedInvoices;
    private List<String> failureReasons;
}
```

---

### 45.4 Controller Endpoints

**Additions to `CreditNoteController`:**

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/batch-credit` | Issue full credits for multiple invoices | INVOICING_USER |
| GET | `/api/v1/customers/{id}/credit-history` | Paginated credit notes for a customer | INVOICING_USER |

**POST /api/v1/invoices/batch-credit request:**
```json
{
  "invoiceIds": [1001, 1002, 1003],
  "customText": "Batch credit issued due to system pricing error in January 2024.",
  "internalComment": "Pricing bug discovered in product group BIN_EMPTYING. All Jan 2024 invoices credited. Ticket #4521."
}
```

**POST /api/v1/invoices/batch-credit response:**
```json
{
  "succeeded": 2,
  "failed": 1,
  "creditedInvoices": [
    { "creditNoteId": 2001, "creditNoteNumber": "KH2024000001", "originalInvoiceId": 1001 },
    { "creditNoteId": 2002, "creditNoteNumber": "KH2024000002", "originalInvoiceId": 1002 }
  ],
  "failureReasons": [
    "Invoice 1003: Credit notes can only be issued for SENT or COMPLETED invoices. Current status: DRAFT"
  ]
}
```

**GET /api/v1/customers/{id}/credit-history?page=0&size=20 response:**
```json
{
  "content": [
    {
      "id": 2001,
      "invoiceNumber": "KH2024000001",
      "invoiceDate": "2024-03-15",
      "originalInvoiceId": 1001,
      "originalInvoiceNumber": "PL2024000042",
      "netAmount": -120.00,
      "grossAmount": -148.80,
      "status": "SENT",
      "customText": "Batch credit issued due to system pricing error."
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## Frontend

### 45.5 Multi-Select Invoice List with Batch Credit Action

**File:** `invoicing-fe/src/pages/invoices/InvoiceListPage.jsx`

Components:
- **InvoiceListTable** — extends existing invoice list with a checkbox column. Checkboxes only enabled for invoices with status SENT or COMPLETED.
- **BulkActionsBar** — appears when ≥1 invoice selected: shows "X selected" count and "Batch Credit" button.
- **BatchCreditModal** — modal triggered by "Batch Credit" button:
  - Summary: "You are about to credit X invoices totalling €Y."
  - `customText` field (optional).
  - `internalComment` field (required — red asterisk).
  - "Issue Credits" button → calls `POST /api/v1/invoices/batch-credit`.
- **BatchCreditResultPanel** — shown after submission: succeeded count, failed count, list of failure reasons.

### 45.6 Customer Credit History Tab

**File:** `invoicing-fe/src/pages/customers/CustomerCreditHistoryTab.jsx`

A tab on the customer detail page (or a dedicated sub-route `/customers/{id}/credit-history`).

Components:
- **CreditHistoryTable** — columns: Credit Note Number, Issue Date, Original Invoice Number, Net Amount (shown in red), Gross Amount (shown in red), Status.
- **Pagination** — standard page controls.
- Clicking a row navigates to `InvoiceDetailPage` for the credit note.

**API calls (additions to `src/api/invoices.js`):**
```js
export const batchCredit = (data) => axios.post('/api/v1/invoices/batch-credit', data)

export const getCreditHistory = (customerId, page = 0, size = 20) =>
  axios.get(`/api/v1/customers/${customerId}/credit-history`, { params: { page, size } })
```

---

## Verification Checklist

1. `POST /api/v1/invoices/batch-credit` with 3 valid invoice IDs → 3 credit notes created; response `succeeded: 3`, `failed: 0`.
2. Batch with 2 valid + 1 DRAFT invoice → response `succeeded: 2`, `failed: 1`; failure reason includes invoice ID and status message.
3. Batch with blank `internalComment` → HTTP 400; zero credit notes created.
4. All credit notes in a batch share the same `customText` and `internalComment`.
5. `GET /api/v1/customers/{id}/credit-history` returns only `invoiceType = CREDIT_NOTE` invoices for this customer.
6. Pagination: 25 credit notes; request with `size=10` → 3 pages, first page has 10 items.
7. Credit history sorted by `invoiceDate DESC` — most recent credit notes first.
8. `CreditHistoryTable` shows amounts in red; clicking a row opens the credit note detail page.
9. Multi-select in `InvoiceListTable`: checkboxes only enabled for SENT/COMPLETED status; selecting 3 → "3 selected" counter in `BulkActionsBar`.
10. `BatchCreditModal` prevents submit when `internalComment` is blank; shows validation error inline.

---

## File Checklist

### Backend
- [ ] `credit/CreditNoteService.java` — add `batchCredit()` method (extends step 44)
- [ ] `credit/CreditNoteController.java` — add batch-credit and credit-history endpoints (extends step 44)
- [ ] `credit/BatchCreditRequest.java`
- [ ] `credit/BatchCreditResult.java`
- [ ] `invoice/InvoiceRepository.java` — add `findCreditNotesByCustomerId()` (extends step 29)

### Frontend
- [ ] `src/pages/invoices/InvoiceListPage.jsx` — add checkbox column and BulkActionsBar
- [ ] `src/pages/invoices/components/BulkActionsBar.jsx`
- [ ] `src/pages/invoices/components/BatchCreditModal.jsx`
- [ ] `src/pages/invoices/components/BatchCreditResultPanel.jsx`
- [ ] `src/pages/customers/CustomerCreditHistoryTab.jsx`
- [ ] `src/pages/customers/components/CreditHistoryTable.jsx`
- [ ] `src/api/invoices.js` — add `batchCredit()` and `getCreditHistory()` (extends step 44)
