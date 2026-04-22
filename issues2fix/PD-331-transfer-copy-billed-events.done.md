# PD-331: Transfer and Copy of Billed Events (Req 3.4.38)
**Jira:** https://ioteelab.atlassian.net/browse/PD-331
**Linear:** https://linear.app/wastehero/issue/REQ-162/3438-transfer-and-copy-of-billed-events
**Status:** Partially implemented — 7 gaps to close

The backend correction service and credit note flow are solid. The main missing pieces are: (1) a pure Copy operation with no credit note, (2) creating a new Draft invoice after correction instead of loose billing events, (3) billing events not surfaced or selectable in the correction form UI, and (4) credit notes not pushed to the external invoicing system.

---

## What is already working

- `POST /api/v1/invoices/{id}/correct` → `BilledEventCorrectionService.correct()` — issues credit note + copies billing events to a new customer
- `POST /api/v1/invoices/{id}/credit` → `CreditNoteService.credit()` — full/partial credit note creation, FINVOICE XML generated, status set to READY
- `POST /api/v1/invoices/batch-credit` — batch credit notes
- `GET /api/v1/customers/{customerId}/credit-history` — lists credit notes for a customer
- Credit note correctly stays on the original customer after a cross-customer correction (`creditNote.setCustomer(original.getCustomer())` in `CreditNoteService.java` line 66)
- Partial credit by line item selection works in `CreditNoteService.java` (lines 54–60) and `CreditNoteForm.jsx`
- `BilledEventCorrectionService.copyEventForCorrection()` copies all business fields, sets `correctedFromEventId`, status `IN_PROGRESS`, correct customer number
- Same/different customer toggle in `CorrectInvoiceForm.jsx` (lines 88–100)
- Internal comment enforced as mandatory on both credit note and correction (lines 38–39 of `CreditNoteService.java`, line 31 of `CorrectInvoiceForm.jsx`)
- `BillingEventDetailPage.jsx` shows coloured banners (blue/yellow/green) for event-level credit chains via `BillingEventCreditTransferService`

---

## Gap 1 — No "Copy" operation (no credit note) (HIGH)

**Requirement FR-8, FR-9:** Copy duplicates events to a new invoice without crediting the original. The current implementation only has a combined credit+copy path. There is no way to copy events without issuing a credit note.

**Current state:** `BilledEventCorrectionService.correct()` always calls `creditNoteService.credit()` first (line 43). `CorrectInvoiceForm.jsx` only has one action button: "Issue Credit & Copy Events" (line 143). No Copy-only endpoint or service method exists.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/BilledEventCorrectionService.java` | Add a new `copy(Long originalInvoiceId, CopyRequest request)` method that skips the credit note step — only executes the event-copy and new-draft-invoice creation logic (see Gap 2). Reuse `copyEventForCorrection()` unchanged. |
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/invoice/CorrectionController.java` | Add `POST /api/v1/invoices/{id}/copy` endpoint that calls the new `copy()` method. Keep the existing `/correct` endpoint for Transfer. |
| DTO (new) | `invoicing/src/main/java/com/example/invoicing/entity/invoice/dto/CopyRequest.java` | New DTO mirroring `CorrectionRequest` but without `lineItemIds` (no credit = no line selection needed): `targetCustomerId` (Long, optional), `targetPropertyId` (String, optional), `billingEventIds` (List<Long>), `internalComment` (String, required). |
| Frontend | `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx` | Add a mode toggle at the top: "Transfer (credit + re-invoice)" vs "Copy (re-invoice only, no credit)". In Copy mode: hide the "Lines to Correct" section (no line item selection needed), change the submit button label to "Copy Events to New Invoice", and call `copyInvoice(id, data)` instead of `correctInvoice(id, data)`. |
| Frontend API | `invoicing-fe/src/api/invoices.js` | Add `copyInvoice(id, data)` → `POST /api/v1/invoices/{id}/copy`. |

---

## Gap 2 — No new Draft invoice created after Transfer or Copy (HIGH)

**Requirement FR-6, FR-10, FR-11, FR-13, FR-16:** After transferring or copying events, a new Draft invoice must be created pre-populated with those events. Currently `BilledEventCorrectionService.correct()` creates loose `IN_PROGRESS` billing events but never creates an Invoice entity. The frontend navigates to the billing events list, not a new invoice.

**Current state:** `BilledEventCorrectionService.correct()` lines 61–65 save copied events and return their IDs. `CorrectInvoiceForm.jsx` line 63 navigates to `/billing-events` on success. No `Invoice` is created anywhere in the correction flow.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/BilledEventCorrectionService.java` | After saving copied events (line 64), call `invoiceService.createDraftFromEvents(copiedEventIds, targetCustomerId)` and include the returned `draftInvoiceId` in `CorrectionResult`. |
| Service | `invoicing/src/main/java/com/example/invoicing/service/InvoiceService.java` | Add `createDraftFromEvents(List<Long> billingEventIds, Long customerId)`: creates a new `Invoice` entity with `status = DRAFT`, `invoiceType = STANDARD`, sets `customer` from customerId, links the provided billing events, and saves. Returns the new invoice ID. |
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/invoice/Invoice.java` | Confirm (or add) a `@OneToMany` relationship to `BillingEvent` so that `createDraftFromEvents` can associate events to the new invoice. |
| DTO | `invoicing/src/main/java/com/example/invoicing/entity/invoice/dto/CorrectionResult.java` | Add `Long draftInvoiceId` field to the response. |
| Frontend | `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx` lines 59–68 | Change the success state: replace `navigate('/billing-events')` with `navigate('/invoices/${result.draftInvoiceId}')` so the user lands on the new Draft invoice. Update the "View Copied Events" button to "View New Draft Invoice". |

---

## Gap 3 — Billing events not shown or selectable in CorrectInvoiceForm (HIGH)

**Requirement FR-1, FR-3:** The user must be able to see the billing events on an invoice and select which ones to copy/transfer. Currently `CorrectInvoiceForm.jsx` has no billing events table. The `billingEventIds` field in `CorrectionRequest` is **never populated from the frontend** — so billing events are never copied regardless of what the user does.

**Current state:** `CorrectInvoiceForm.jsx` lines 34–39 build the request body with `lineItemIds` only — `billingEventIds` is omitted entirely. `BilledEventCorrectionService.correct()` line 57 checks `if (request.getBillingEventIds() != null && !request.getBillingEventIds().isEmpty())` — this condition is always false from the UI.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Backend | `invoicing/src/main/java/com/example/invoicing/controller/invoice/CorrectionController.java` | Add `GET /api/v1/invoices/{id}/billing-events` endpoint that returns the billing events linked to this invoice — needed so the frontend can display them for selection. |
| Frontend | `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx` | After the invoice loads, also fetch `GET /api/v1/invoices/{id}/billing-events`. Render a "Billing Events to Copy" section below "Lines to Correct" with a checkbox table (columns: Event #, Date, Product, Customer, Total fees). Pre-select all events by default. Include the selected event IDs as `billingEventIds` in the request body. |
| Frontend | `invoicing-fe/src/api/invoices.js` | Add `getInvoiceBillingEvents(id)` → `GET /api/v1/invoices/{id}/billing-events`. |

---

## Gap 4 — Credit note not pushed to external invoicing system (HIGH)

**Requirement FR-15:** Credit notes must be pushed to the external invoicing system after creation. Currently `CreditNoteService.credit()` builds FINVOICE XML (lines 100–104) and sets status to READY (line 106) but never triggers transmission.

**Current state:** `CreditNoteService.java` line 106 sets `READY` and saves — no call to `InvoiceTransmissionService` or any external push. The credit note stays in READY indefinitely.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/CreditNoteService.java` | After saving the credit note (line 107), call `invoiceTransmissionService.transmit(saved.getId())` to push to the external system. Inject `InvoiceTransmissionService` as a dependency. |
| Service | `invoicing/src/main/java/com/example/invoicing/service/CreditNoteService.java` | Wrap the transmission call in a try/catch: on failure, set the credit note status to ERROR and log — do not roll back the credit note creation itself, as the credit is a financial record that must persist regardless of transmission outcome. |

---

## Gap 5 — No property selection in CorrectInvoiceForm (MEDIUM)

**Requirement FR-7:** When re-routing to a different customer, the user should also be able to specify a target property. `CorrectInvoiceForm.jsx` has no property selector — `targetPropertyId` cannot be set from the UI.

**Current state:** `CorrectInvoiceForm.jsx` lines 95–100 only render a customer ID number input when "Different customer" is selected. No property field exists. `CorrectionRequest.java` has no `targetPropertyId` field.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| DTO | `invoicing/src/main/java/com/example/invoicing/entity/invoice/dto/CorrectionRequest.java` | Add `String targetPropertyId` (nullable). |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BilledEventCorrectionService.java` | In `copyEventForCorrection()`, if `request.getTargetPropertyId()` is non-null, call `copy.setLocationId(request.getTargetPropertyId())` instead of copying the original's `locationId` (line 93). |
| Frontend | `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx` | Below the customer selector (line 98), add a property search field (use `SearchableAutocomplete` as in `CreditTransferModal.jsx`) that only activates after a target customer is chosen. Include `targetPropertyId` in the request body. |

---

## Gap 6 — Customer input is a plain number field, not a searchable selector (MEDIUM)

**Requirement FR-7:** The "Different customer" input in `CorrectInvoiceForm.jsx` is a plain `<input type="number">` (line 98) that requires the user to know the internal customer ID. `CreditTransferModal.jsx` solves this correctly with `SearchableAutocomplete` + customer number resolution — the same pattern should be used here.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/invoices/CorrectInvoiceForm.jsx` lines 95–100 | Replace the `<input type="number" placeholder="Customer ID">` with a `SearchableAutocomplete` (same as `CreditTransferModal.jsx` lines 93–116). On selection, store both the customer's internal ID (`targetCustomerId`) and display name. The `searchCustomers` API function already exists in `src/api/customers.js`. |

---

## Gap 7 — Original invoice detail page missing credit note banner/link (MEDIUM)

**Requirement FR-12:** After a credit note is issued against an invoice, the original invoice's detail view must show which lines were credited and link to the credit note. No such banner exists.

**Current state:** The invoice detail page (check `invoicing-fe/src/pages/invoices/InvoiceDetailPage.jsx`) has no credit note indicator. The link exists in the data model (`creditNote.setOriginalInvoice(original)` in `CreditNoteService.java` line 65) but is not surfaced in the UI.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Backend | `invoicing/src/main/java/com/example/invoicing/entity/invoice/dto/InvoiceDetailResponse.java` (or equivalent) | Add `List<CreditNoteSummary> creditNotes` — each entry includes `creditNoteId`, `creditNoteNumber`, `creditType`, `issuedAt`, `netAmount`. Populate by querying invoices where `originalInvoice = this invoice` and `invoiceType = CREDIT_NOTE`. |
| Frontend | `invoicing-fe/src/pages/invoices/InvoiceDetailPage.jsx` | If `invoice.creditNotes` is non-empty, render a yellow banner at the top: "This invoice has been (partially) credited." List each credit note with its number as a clickable link to `/invoices/{creditNoteId}`. |

---

## Checklist

- [ ] Gap 1a: Add `copy()` method to `BilledEventCorrectionService.java`
- [ ] Gap 1b: Add `POST /api/v1/invoices/{id}/copy` to `CorrectionController.java`
- [ ] Gap 1c: Create `CopyRequest.java` DTO
- [ ] Gap 1d: Add Transfer/Copy mode toggle + Copy submit path to `CorrectInvoiceForm.jsx`
- [ ] Gap 1e: Add `copyInvoice(id, data)` to `invoices.js` API client
- [ ] Gap 2a: Add `createDraftFromEvents()` to `InvoiceService.java`
- [ ] Gap 2b: Call `createDraftFromEvents()` from `BilledEventCorrectionService.correct()` and `copy()`
- [ ] Gap 2c: Add `draftInvoiceId` to `CorrectionResult.java`
- [ ] Gap 2d: Navigate to new Draft invoice on success in `CorrectInvoiceForm.jsx`
- [ ] Gap 3a: Add `GET /api/v1/invoices/{id}/billing-events` endpoint to `CorrectionController.java`
- [ ] Gap 3b: Load and render billing events table with checkboxes in `CorrectInvoiceForm.jsx`
- [ ] Gap 3c: Include selected `billingEventIds` in request body from `CorrectInvoiceForm.jsx`
- [ ] Gap 4a: Inject `InvoiceTransmissionService` into `CreditNoteService.java` and call `transmit()` after save
- [ ] Gap 4b: Handle transmission failure gracefully (set status to ERROR, don't rollback the credit note)
- [ ] Gap 5a: Add `targetPropertyId` to `CorrectionRequest.java`
- [ ] Gap 5b: Apply `targetPropertyId` in `copyEventForCorrection()` in `BilledEventCorrectionService.java`
- [ ] Gap 5c: Add property `SearchableAutocomplete` to `CorrectInvoiceForm.jsx`
- [ ] Gap 6: Replace customer number input with `SearchableAutocomplete` in `CorrectInvoiceForm.jsx`
- [ ] Gap 7a: Expose `creditNotes` list in `InvoiceDetailResponse`
- [ ] Gap 7b: Render credit note banner + links in `InvoiceDetailPage.jsx`
