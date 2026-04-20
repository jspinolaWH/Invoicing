# PD-283: Manual Creation of Billing Events
**Jira:** https://ioteelab.atlassian.net/browse/PD-283
**Linear:** https://linear.app/wastehero/issue/REQ-170/3430-manual-creation-of-billing-events
**Status:** 9/9 gaps closed — fully complete

---

## What is already working

- `POST /api/v1/billing-events/manual` endpoint exists — `BillingEventController.java`
- `CreateBillingEventPage.jsx` with product dropdown, date, fee fields, quantity, weight, customer number, comments
- Product auto-fills `wasteFeePrice`, `transportFeePrice`, `ecoFeePrice` from product defaults on selection — `CreateBillingEventPage.jsx` lines 41–54
- VAT rate preview resolved from product + date via `useResolvedVatRate` hook — line 30
- `origin` field stored as `"MANUAL"` on all manually created events — `BillingEvent.java` line 134
- Manual badge visible in list and detail views
- Comments / free-text description field (2000 chars) — `BillingEvent.java` line 93
- Event date field — supports any past date (retroactive)
- Full audit trail via `BaseAuditEntity` — creator and timestamp stored on every event
- `BillingEventAuditLog` table tracks all field-level changes with `changedBy`, `changedAt`, `reason`

---

## Gap 1 — Customer search instead of raw number entry (HIGH)

**Requirement FR-2:** The user selects a customer via search — not by typing a raw 6-9 digit number. The current form requires knowing the customer number in advance, which is error-prone and unintuitive.

**Current state:** `CreateBillingEventPage.jsx` line 195 renders a plain text input for `customerNumber`. There is no lookup against the customer list.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx` line 193–198 | Replace the raw text input with a searchable customer selector. On selection, store `customerNumber` in form state. Display customer name alongside the stored number. |
| Frontend (new) | `invoicing-fe/src/components/billing/CustomerSearchInput.jsx` | New reusable component: type-ahead search that calls `GET /api/v1/customers?search=<query>`, renders results as a dropdown (name + number), and returns `customerNumber` on select. |
| Frontend | `invoicing-fe/src/api/customers.js` | Add `searchCustomers(query)` function calling `GET /api/v1/customers?search=<query>&limit=20` |
| Backend | `invoicing/src/main/java/com/example/invoicing/controller/customer/CustomerController.java` | Confirm or add `GET /api/v1/customers?search=<query>` that filters by name or customer number and returns `[{ customerNumber, name }]` |

---

## Gap 2 — Contract selection (HIGH)

**Requirement FR-3:** After selecting a customer, the user selects a contract — the binding of that customer to a set of products. The contract scopes which products are available and ensures correct financial allocations.

**Current state:** No contract entity, repository, service, controller, or UI exists anywhere in the codebase. This is a new model that must be built.

A contract binds a customer to one or more products (e.g., "Land Rent agreement for customer 123456"). When a user creates a manual billing event, the contract is the authorisation that ties the event to pre-agreed products and their financial config.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity (new) | `invoicing/src/main/java/com/example/invoicing/entity/contract/Contract.java` | New entity: `id`, `customerNumber` (String), `name` (String), `active` (boolean), `products` (ManyToMany → Product). Extend `BaseAuditEntity`. |
| Repository (new) | `invoicing/src/main/java/com/example/invoicing/repository/ContractRepository.java` | New JPA repository. Add `findByCustomerNumberAndActiveTrue(String customerNumber)`. |
| Controller (new) | `invoicing/src/main/java/com/example/invoicing/controller/contract/ContractController.java` | `GET /api/v1/contracts?customerNumber=<n>` — returns active contracts for a customer. `GET /api/v1/contracts/{id}/products` — returns products on that contract. |
| Service (new) | `invoicing/src/main/java/com/example/invoicing/service/ContractService.java` | `getContractsForCustomer(customerNumber)`, `getProductsForContract(contractId)` |
| DB migration (new) | `invoicing/src/main/resources/db/migration/V4__contracts.sql` | Create `contracts` table and `contract_products` join table |
| Frontend | `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx` | After customer is selected, fetch contracts via `GET /api/v1/contracts?customerNumber=<n>` and render a contract selector dropdown. Clear product selection when contract changes. |
| Frontend | `invoicing-fe/src/api/contracts.js` (new) | `getContractsForCustomer(customerNumber)`, `getProductsForContract(contractId)` |

---

## Gap 3 — Product list filtered by selected contract (HIGH)

**Requirement FR-4:** Once a contract is selected, only products belonging to that contract appear in the product dropdown — not all products in the system.

**Current state:** `CreateBillingEventPage.jsx` line 33 calls `getInvoicingProducts()` which fetches the global product list. There is no contract-scoped filtering.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx` lines 32–34 | Replace the `getInvoicingProducts()` call with `getProductsForContract(contractId)` called after contract selection. Products state should be empty until a contract is selected. Show "Select a contract first" placeholder in the product dropdown until a contract is chosen. |
| Frontend | `invoicing-fe/src/api/contracts.js` | `getProductsForContract(contractId)` — calls `GET /api/v1/contracts/{id}/products` |

---

## Gap 4 — Unit price override flag (MEDIUM)

**Requirement FR-9:** When a user changes a fee price from the product default, the event must be flagged as price-overridden. The original product default values, override values, and the user who made the change must all be recorded. The override flag must be visible in list and detail views.

**Current state:** `CreateBillingEventPage.jsx` lines 41–53 pre-fill fee fields from product defaults. The user can freely change the values, but nothing tracks whether they differ from the product defaults. No `priceOverridden` flag exists on `BillingEvent.java`.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEvent.java` | Add `priceOverridden` (boolean, default false), `originalWasteFeePrice`, `originalTransportFeePrice`, `originalEcoFeePrice` (BigDecimal, nullable) |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | In `createManual()`: after loading the product defaults, compare submitted fees against defaults. If any differ, set `priceOverridden = true` and populate the three `original*` fields with the product's default values |
| DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/dto/BillingEventDetailResponse.java` | Expose `priceOverridden`, `originalWasteFeePrice`, `originalTransportFeePrice`, `originalEcoFeePrice` |
| DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/dto/BillingEventResponse.java` | Expose `priceOverridden` (boolean) for list view badge |
| DB migration | `invoicing/src/main/resources/db/migration/V4__contracts.sql` (or new V5) | `ALTER TABLE billing_events ADD COLUMN price_overridden BOOLEAN NOT NULL DEFAULT FALSE`, add three `original_*_fee_price` columns (NUMERIC 19,4, nullable) |
| Frontend — list | `invoicing-fe/src/pages/billing/BillingEventsPage.jsx` | Show an "Override" badge (e.g. amber) on rows where `priceOverridden === true` |
| Frontend — detail | `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` | In the pricing section, when `priceOverridden === true`: show original values alongside current values with a clear label (e.g. "Product default: €X → Overridden: €Y") |

---

## Gap 5 — Product pricing unit label in quantity field (LOW)

**Requirement FR-6:** The quantity field must display the product's pricing unit (e.g. "KG", "TON", "HOUR") so the user knows what unit they are entering.

**Current state:** `CreateBillingEventPage.jsx` line 175–181 renders a bare quantity input with no unit label. The selected product's `pricingUnit` is available in the `products` array but not displayed.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx` lines 175–181 | After product selection, read `selectedProduct.pricingUnit` and render it as a unit suffix on the quantity input (e.g. an inline label: `Quantity (HOUR) *`). No backend change needed. |

---

## Gap 6 — Draft saving (MEDIUM)

**Requirement FR-10:** Users can save a billing event as a draft before submitting, allowing them to return and complete it later. Draft events are not processed by the billing run.

**Current state:** The form submits directly as an `IN_PROGRESS` event. There is no `DRAFT` state in `BillingEventStatus.java` and no save-draft path in the service or UI.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Enum | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventStatus.java` | Add `DRAFT` as the first status value |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | Add `saveDraft()` method: same as `createManual()` but sets `status = DRAFT`. Ensure `assertMutable()` does not block edits on `DRAFT` events. `DRAFT` events must be excluded from the billing run query. |
| Status service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventStatusService.java` | Add `DRAFT → IN_PROGRESS` as a valid transition (the "submit" action). Ensure `DRAFT` is not a terminal state. |
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/billingevent/BillingEventController.java` | Add `POST /api/v1/billing-events/draft` endpoint for saving drafts |
| Frontend | `invoicing-fe/src/pages/billing/CreateBillingEventPage.jsx` lines 273–278 | Add a "Save as Draft" button alongside "Create Event". Draft button calls the `/draft` endpoint and navigates to the detail page. |
| Frontend — list | `invoicing-fe/src/pages/billing/BillingEventsPage.jsx` | Ensure `DRAFT` status is shown in the status filter and rendered with a distinct badge colour |
| Frontend — detail | `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` | On `DRAFT` events, show a "Submit" button that transitions `DRAFT → IN_PROGRESS` |

---

## Gap 7 — File attachments on billing events (MEDIUM)

**Requirement FR-11:** Users can attach files to a billing event (e.g. a supporting document for the land rent or hours worked).

**Current state:** `InvoiceAttachmentController` and `InvoiceAttachment` entity exist for invoices but there is no equivalent for billing events. No attachment upload UI exists on `CreateBillingEventPage.jsx` or `BillingEventDetailPage.jsx`.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity (new) | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventAttachment.java` | New entity: `id`, `billingEvent` (ManyToOne), `fileName`, `fileSize`, `contentType`, `storagePath`. Extend `BaseAuditEntity`. |
| Repository (new) | `invoicing/src/main/java/com/example/invoicing/repository/BillingEventAttachmentRepository.java` | `findByBillingEventId(Long id)`, `countByBillingEventId(Long id)` |
| Controller (new) | `invoicing/src/main/java/com/example/invoicing/controller/billingevent/BillingEventAttachmentController.java` | `POST /api/v1/billing-events/{id}/attachments` (multipart upload), `GET /api/v1/billing-events/{id}/attachments`, `DELETE /api/v1/billing-events/{id}/attachments/{attachmentId}` |
| Service (new) | `invoicing/src/main/java/com/example/invoicing/service/BillingEventAttachmentService.java` | Upload validation (max 10 files, 10 MB each), storage, deletion |
| DB migration | new migration file | Create `billing_event_attachments` table |
| Frontend | `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` | Add an Attachments tab or section: file list, upload button, delete button |
| Frontend | `invoicing-fe/src/api/billingEvents.js` | Add `uploadBillingEventAttachment(id, file)`, `getBillingEventAttachments(id)`, `deleteBillingEventAttachment(id, attachmentId)` |

---

## Checklist

- [x] Gap 1a: `CustomerSearchInput.jsx` component with type-ahead lookup
- [x] Gap 1b: Replace raw customer number text input in `CreateBillingEventPage.jsx` with `CustomerSearchInput`
- [x] Gap 1c: `GET /api/v1/customers/search` confirmed in `CustomerController.java`
- [x] Gap 2a: `Contract` entity, repository, service, and controller created
- [x] Gap 2b: `V4__contracts.sql` migration for `contracts` and `contract_products` tables
- [x] Gap 2c: Contract selector in `CreateBillingEventPage.jsx` (after customer selection)
- [x] Gap 3: Product dropdown filtered by selected contract via `GET /api/v1/contracts/{id}/products`
- [x] Gap 4a: `priceOverridden` + `original*Fee` fields added to `BillingEvent.java` + `V5` migration
- [x] Gap 4b: Override flag detection in `BillingEventService.createManual()`
- [x] Gap 4c: Override fields exposed in `BillingEventDetailResponse` and `BillingEventResponse`
- [x] Gap 4d: Override badge in `BillingEventsPage.jsx`; original vs overridden display in `BillingEventDetailPage.jsx`
- [x] Gap 5: Product `pricingUnit` label shown next to quantity field in `CreateBillingEventPage.jsx`
- [x] Gap 6a: `DRAFT` added to `BillingEventStatus` enum + `DRAFT → IN_PROGRESS` transition allowed
- [x] Gap 6b: `saveDraft()` in `BillingEventService` + `POST /api/v1/billing-events/draft` endpoint
- [x] Gap 6c: "Save as Draft" button in `CreateBillingEventPage.jsx`; Submit button on draft detail view
- [x] Gap 7a: `BillingEventAttachment` entity, repository, service, controller + `V5` migration
- [x] Gap 7b: Attachments tab with upload/list/delete in `BillingEventDetailPage.jsx`
- [x] Gap 8: `BillingEventTemplate` entity + CRUD endpoints + save/load UI in `CreateBillingEventPage.jsx` — `V6__billing_event_templates.sql`
- [x] Gap 9: Undo/redo history stack in `CreateBillingEventPage.jsx` — Ctrl+Z / Ctrl+Y shortcuts + Undo/Redo buttons
