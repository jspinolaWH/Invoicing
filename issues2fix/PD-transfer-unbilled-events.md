# Transfer of Unbilled Billing Events (Spec 3.4.37)
**Jira:** https://ioteelab.atlassian.net/browse/PD-283 (related)
**Linear:** https://linear.app/wastehero/issue/REQ-204/3218-transfer-of-billing-events
**Status:** Partially implemented — 9 gaps to close

The codebase has a working immediate transfer mechanism (single + bulk), but the spec requires a two-step **pending/confirm** flow. The current implementation transfers atomically with no review step, no reversibility, and no pending state.

---

## What is already working

- Single transfer endpoint `POST /api/v1/billing-events/{id}/transfer` — `BillingEventController.java`
- Bulk transfer endpoint `POST /api/v1/billing-events/bulk-transfer` — `BillingEventController.java`
- `BillingEventTransferService.java` — handles both, writes to `BillingEventTransferLog` and `BillingEventAuditLog`
- Audit log captures: original `customerNumber`, new `customerNumber`, original `locationId`, new `locationId`, transferred by, reason, timestamp
- `assertMutable()` blocks transfer on `SENT`/`COMPLETED` events — `BillingEventStatusService.java`
- Checkboxes + "Transfer Selected" toolbar button in `BillingEventsPage.jsx`
- `TransferEventModal.jsx` with customer search (`SearchableAutocomplete`) + property search + reason field — used for single-event transfer
- `BillingEventTransferLog` entity and table already exist

---

## Gap 1 — PENDING_TRANSFER status missing (HIGH)

**Requirement FR-5 / FR-8 / FR-9:** Events must move to a `PENDING_TRANSFER` status on transfer initiation — locked from edits, excluded from the billing run, and reversible by cancellation. Currently transfers happen atomically with no intermediate state.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Enum | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventStatus.java` | Add `PENDING_TRANSFER` between `DRAFT` and `IN_PROGRESS` |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventStatusService.java` | Add `PENDING_TRANSFER` to `assertMutable()` block — events in this state cannot be edited. Add `IN_PROGRESS → PENDING_TRANSFER` and `PENDING_TRANSFER → IN_PROGRESS` as valid transitions |
| Billing run query | `invoicing/src/main/java/com/example/invoicing/repository/BillingEventRepository.java` | Ensure any query that fetches billable events excludes `status = PENDING_TRANSFER` (add to the existing status filter) |
| DB migration (new) | `invoicing/src/main/resources/db/migration/V7__transfer_pending.sql` | No schema change needed — `PENDING_TRANSFER` is a new enum value stored as string in existing `status` column |

---

## Gap 2 — BillingEvent missing pending transfer fields (HIGH)

**Requirement FR-5 / FR-8 / FR-11:** The entity needs to store the proposed transfer target (so it can be reviewed and cancelled) and the original values (so they can be displayed after confirmation).

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEvent.java` | Add 4 fields: `pendingTransferCustomerNumber` (String, nullable), `pendingTransferLocationId` (String, nullable), `priorCustomerNumber` (String, nullable), `priorLocationId` (String, nullable) |
| DB migration | `invoicing/src/main/resources/db/migration/V7__transfer_pending.sql` | `ALTER TABLE billing_events ADD COLUMN pending_transfer_customer_number VARCHAR(9)`, `ADD COLUMN pending_transfer_location_id VARCHAR(100)`, `ADD COLUMN prior_customer_number VARCHAR(9)`, `ADD COLUMN prior_location_id VARCHAR(100)` |
| Detail Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/dto/BillingEventDetailResponse.java` | Expose `priorCustomerNumber`, `priorLocationId`, `pendingTransferCustomerNumber`, `pendingTransferLocationId` |
| List Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/dto/BillingEventResponse.java` | Expose `pendingTransferCustomerNumber` (so list view can show pending indicator) |

---

## Gap 3 — Two-step transfer flow: initiate → confirm/cancel (HIGH)

**Requirement FR-5 / FR-6 / FR-7 / FR-8:** The current `transfer()` method applies changes immediately. It must be split into two steps: initiate (moves to `PENDING_TRANSFER`, stores proposed target) and confirm (applies the change) or cancel (reverts).

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventTransferService.java` | **Refactor `transfer()` and `bulkTransfer()`:** Instead of immediately updating `customerNumber`/`locationId`, set `status = PENDING_TRANSFER`, populate `pendingTransferCustomerNumber`, `pendingTransferLocationId`, store `priorCustomerNumber = event.getCustomerNumber()` and `priorLocationId = event.getLocationId()`. Do NOT yet update `customerNumber` or `locationId`. |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventTransferService.java` | **Add `confirmTransfer(Long eventId, String confirmedBy)`:** Copy `pendingTransfer*` values into `customerNumber`/`locationId`, set `status = IN_PROGRESS`, clear `pending*` fields. Write audit log entries with `changedBy = confirmedBy`. |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventTransferService.java` | **Add `cancelTransfer(Long eventId, String cancelledBy)`:** Restore `customerNumber` from `priorCustomerNumber`, `locationId` from `priorLocationId`, set `status = IN_PROGRESS`, clear all `pending*` and `prior*` fields. Write audit log entry. |
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/billingevent/BillingEventController.java` | Add `POST /api/v1/billing-events/{id}/transfer/confirm` and `POST /api/v1/billing-events/{id}/transfer/cancel` endpoints. Add `POST /api/v1/billing-events/bulk-transfer/confirm` and `/cancel` for bulk. |

---

## Gap 4 — BulkTransferModal lacks customer search and property selector (HIGH)

**Requirement FR-2 / FR-3:** The inline bulk transfer modal in `BillingEventsPage.jsx` (lines 418–479) uses a plain text input for customer number with no search, and has no property selector at all. The spec requires a customer search autocomplete and an optional active-property selector filtered to the target customer.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/billing/BillingEventsPage.jsx` lines 418–479 | Replace the inline bulk transfer modal with a dedicated `BulkTransferModal` component (or reuse `TransferEventModal.jsx`). Must include: `CustomerSearchInput` (already exists), active property selector filtered to target customer (see Gap 5), mandatory reason field |
| Frontend (new) | `invoicing-fe/src/components/billing/BulkTransferModal.jsx` | New component: Step 1 — customer search + optional property selector + reason. Step 2 — review table: one row per selected event showing current customer → proposed customer (and property if selected). Confirm / Cancel buttons. Calls `POST /api/v1/billing-events/bulk-transfer` on confirm (which after Gap 3 initiates pending, not immediate) |

---

## Gap 5 — Property selector not filtered to target customer's active properties (MEDIUM)

**Requirement FR-3:** `TransferEventModal.jsx` lines 29–32 call `searchProperties(q)` against the full property list — not filtered to the selected target customer's active properties.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Backend | `invoicing/src/main/java/com/example/invoicing/controller/customer/CustomerController.java` | Add `GET /api/v1/customers/{customerNumber}/properties?active=true` — returns active properties for the given customer number. Used by the transfer modal property selector. |
| Frontend | `invoicing-fe/src/components/billing/TransferEventModal.jsx` lines 29–32 | Replace `searchProperties(q)` with a call to `GET /api/v1/customers/{customerNumber}/properties?active=true&search=q` that only fires after a target customer is selected. Disable the property selector until a customer is chosen. |
| Frontend | `invoicing-fe/src/api/customers.js` | Add `getActivePropertiesForCustomer(customerNumber, search)` function |

---

## Gap 6 — Two-step review flow missing from TransferEventModal (MEDIUM)

**Requirement FR-6 / FR-7:** `TransferEventModal.jsx` submits immediately on form completion — there is no review step showing current → proposed values before confirming.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/components/billing/TransferEventModal.jsx` | Add a second step to the modal: after the user fills in customer + property + reason and clicks "Next", show a review panel: "Current customer: X → New customer: Y" (and property if selected). Add Confirm and Back buttons. Only call the transfer API on Confirm. |

---

## Gap 7 — No "Transferred from" banner in detail view (MEDIUM)

**Requirement FR-11:** After a transfer is confirmed, the detail page must show the original customer. Currently `BillingEventDetailPage.jsx` has no such banner, and the entity has no `priorCustomerNumber` field (resolved by Gap 2).

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Frontend | `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` | After the existing exclusion/rejection banners, add: if `event.priorCustomerNumber` is set, render a yellow info strip: "Transferred from: [priorCustomerNumber]" (and prior property if `priorLocationId` is set) |

---

## Gap 8 — Audit log missing "confirmed by" on two-step flow (MEDIUM)

**Requirement FR-10:** The audit log must record both the user who *initiated* the transfer and the user who *confirmed* it. Currently the log only records a single `changedBy` because there is no confirmation step.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/audit/BillingEventAuditLog.java` | Add `confirmedBy` (String, nullable) field |
| DB migration | `invoicing/src/main/resources/db/migration/V7__transfer_pending.sql` | `ALTER TABLE billing_event_audit_log ADD COLUMN confirmed_by VARCHAR(100)` |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventTransferService.java` | In `confirmTransfer()`: write audit entries with `changedBy = originalInitiator` (read from `BillingEventTransferLog`) and `confirmedBy = confirmedBy` parameter |

---

## Gap 9 — No role-based access control on transfer endpoints (LOW)

**Requirement FR-12:** Only users with the invoicing role (or above) may initiate or confirm transfers. No role checks exist on the transfer endpoints.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/billingevent/BillingEventController.java` | Add `@PreAuthorize("hasRole('INVOICING')")` (or equivalent role annotation used elsewhere in the project) to all transfer endpoints: `/transfer`, `/transfer/confirm`, `/transfer/cancel`, `/bulk-transfer`, `/bulk-transfer/confirm`, `/bulk-transfer/cancel` |

---

## Checklist

- [ ] Gap 1a: Add `PENDING_TRANSFER` to `BillingEventStatus.java`
- [ ] Gap 1b: Block edits on `PENDING_TRANSFER` in `BillingEventStatusService.assertMutable()`
- [ ] Gap 1c: Exclude `PENDING_TRANSFER` from billing run queries in `BillingEventRepository.java`
- [ ] Gap 2a: Add `pendingTransferCustomerNumber`, `pendingTransferLocationId`, `priorCustomerNumber`, `priorLocationId` to `BillingEvent.java`
- [ ] Gap 2b: `V7__transfer_pending.sql` migration for new columns
- [ ] Gap 2c: Expose new fields in `BillingEventDetailResponse` and `BillingEventResponse`
- [ ] Gap 3a: Refactor `transfer()` and `bulkTransfer()` to set `PENDING_TRANSFER` + store pending values instead of immediately applying
- [ ] Gap 3b: Add `confirmTransfer()` and `cancelTransfer()` methods to `BillingEventTransferService`
- [ ] Gap 3c: Add confirm/cancel endpoints to `BillingEventController.java` (single + bulk)
- [ ] Gap 4: Replace inline bulk transfer modal in `BillingEventsPage.jsx` with `BulkTransferModal.jsx` that includes customer search + property selector + review step
- [ ] Gap 5a: Add `GET /api/v1/customers/{customerNumber}/properties?active=true` to `CustomerController.java`
- [ ] Gap 5b: Update `TransferEventModal.jsx` property selector to filter to target customer's active properties
- [ ] Gap 6: Add two-step review screen to `TransferEventModal.jsx`
- [ ] Gap 7: Add "Transferred from" banner to `BillingEventDetailPage.jsx`
- [ ] Gap 8a: Add `confirmedBy` field to `BillingEventAuditLog` + migration
- [ ] Gap 8b: Populate `confirmedBy` in `confirmTransfer()` in `BillingEventTransferService`
- [ ] Gap 9: Add role-based `@PreAuthorize` checks to all transfer endpoints in `BillingEventController.java`
