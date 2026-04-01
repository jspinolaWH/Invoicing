# 06 — Cross-Cutting Concerns
## What applies everywhere regardless of domain area

These are not features — they are system-wide rules and mechanisms that apply across every service, every entity, and every API endpoint.

---

## 1. Audit Trail

The requirements mandate a full audit trail on all sensitive mutations. This is not optional — it is legally and contractually required for waste management companies.

> **PD-277:** *"Every change is logged, including the user who made the change, a timestamp, and the reason for the correction. The system generates before-and-after records, ensuring transparency and providing references for customer inquiries or internal audits."*

> **PD-319:** *"Price adjustments are logged, including the reason for the change, the user who performed the update, and the events affected by the change."*

> **PD-364:** *"The system logs all retroactive changes and stores details such as the user who made the change, timestamps, and the events affected by the change."*

> **PD-269:** *"Information about reimbursed transactions must remain in the customer's transaction data."*

### What must be audited

Every `BillingEvent` mutation must produce a log entry with: field name, old value, new value, user ID, timestamp, and reason. The reason field is always required — no silent changes.

The following operations each produce their own dedicated audit record type (not just a generic change log):
- Retroactive price adjustments (PD-319) — which events were affected, the old and new prices, the date range selected
- Retroactive service responsibility changes (PD-364) — which events were affected, old and new classification
- Customer transfers of billing events (PD-276, PD-344) — from/to customer, event IDs, timestamp, reason
- Credit note creation (PD-269) — which invoice was credited, which line items, the reason

### The immutability rule

Audit logs are never updated or deleted. Once written, they are permanent. The application database user should have INSERT permission on audit tables but not UPDATE or DELETE.

### Base audit fields on all entities

Every entity in the system carries: createdBy, createdAt, lastModifiedBy, lastModifiedAt. These four fields are on every table. They are populated automatically by Spring Data Auditing — never set manually by application code.

---

## 2. Roles and Access Control

The requirements explicitly call out role-based access for several features. This must be a real access control system — not just convention.

> **PD-277:** *"Only authorized personnel (e.g., customer service agents, invoicing clerks) have access to manual corrections. Users can be granted different levels of permissions, such as being allowed to edit pricing but not contractor payments."*

> **PD-364:** *"Role-based access rights ensure that only authorised personnel can make retroactive changes."*

> **PD-319 (PJH note):** *"Billing user = function-specific administrator."*

> **PD-171:** *"Authorities must have the ability to view invoices sent to customers and open the invoice view independently."*

> **PD-228:** *"It is necessary to separately define which events/additional tasks can proceed directly to the invoice and which require review by the office worker."*

### Required roles

| Role | What it covers |
|------|---------------|
| INVOICING_USER | Create billing events, run simulations, generate invoices, add exclusions, view events and invoices |
| FUNCTION_ADMIN | Everything INVOICING_USER can do, plus: retroactive price changes (PD-319), retroactive service responsibility changes (PD-364) |
| BILLING_MANAGER | Everything FUNCTION_ADMIN can do, plus: cancel sent invoice runs (PD-273), approve invoice runs for transmission |
| DRIVER | Submit driver events only. Cannot view invoices, cannot edit events after submission |
| AUTHORITY_VIEWER | Read-only access to sent and completed invoices for authority review. No access to drafts, events, or any write operations (PD-171) |
| SYSTEM | Used by scheduled jobs — seasonal fee generation, e-invoice operator integration, billing address sync |

### Enforced access rules from requirements

These are not suggestions — they are explicitly stated in the requirements:

- Retroactive operations (PD-319, PD-364) require FUNCTION_ADMIN. An INVOICING_USER cannot do them.
- Authority access (PD-171) is strictly read-only. No endpoint in the authority scope allows writes.
- Driver event submission (PD-228) uses the DRIVER role. A driver cannot approve their own submitted events.
- Invoice cancellation after transmission (PD-273) requires BILLING_MANAGER. An INVOICING_USER can cancel before transmission but not after.

---

## 3. Simulation Mode Guard

This is the most important cross-cutting behaviour in the system. Any operation that has real-world consequences — assigning invoice numbers, changing event statuses, transmitting data externally — must check whether the current operation is a simulation.

> **PD-272:** *"The simulation generates data that reflects the invoice structure but is marked as test data, preventing accidental processing."*

> **PD-272:** *"The system applies all invoicing rules, such as billing cycles, pricing models, VAT calculations, and cost allocations."* (these DO run in simulation)

The rule is:

**Things that run in both real and simulation modes:**
- Event grouping and filtering
- Bundling rules
- VAT calculation
- Gross/net formatting
- Minimum fee calculation
- Shared service splits
- Legal classification
- Validation rules
- All of the business logic that produces the invoice content

**Things that run ONLY in real mode (never in simulation):**
- Assigning invoice numbers from a series (PD-309)
- Advancing the InvoiceNumberSeries counter
- Changing BillingEvent status
- Changing Invoice status beyond DRAFT
- Transmitting FINVOICE to the external system
- Writing to audit logs for status changes

Every service that performs any of the "real mode only" operations must receive the simulation flag and check it before executing. This is not optional — running a simulation that consumed invoice numbers or changed event statuses would corrupt the system's state.

---

## 4. Status Gate — Immutability After SENT

This is a subset of the overall state machine, but it deserves its own section because it cuts across many services.

> **PD-297 (PJH):** *"The 'Sent' status should mean the billing file has actually been sent, at which point no further changes to the invoice content should be allowed."*

Once a BillingEvent reaches SENT or COMPLETED status, the following operations are blocked:
- Any field edit via PATCH /billing-events/{id}
- Exclusion
- Transfer to another customer
- Any retroactive price or service responsibility change

Any service that touches a BillingEvent must check its status before making changes. This check happens in BillingEventStatusService and in BillingEventService. It is not left to the controller.

For invoices, the same principle applies — once status = SENT, the invoice content is locked. Corrections must go through the credit-and-re-invoice flow (PD-275, PD-269).

---

## 5. Data Locking During Billing Runs

> **PD-270:** *"Certain critical data fields are locked during invoice generation to prevent inconsistencies: Billing address cannot be changed if it affects invoices currently being generated. Billing groups cannot be modified to prevent changes to invoice bundling."*

> **PD-270 (example message):** *"Invoice processing in progress. Address changes cannot be made during this time."*

The locking mechanism works as follows:
- When an InvoiceRun starts, the system registers all customer IDs in scope as locked
- Any attempt to write to a locked customer's billing address or billing group is rejected with a 423 Locked status
- The error message is exactly as specified in the requirements — not a generic 400
- When the InvoiceRun finishes (success, failure, or cancellation), all locks for that run are released

Non-financial fields are not locked. Customer service agents can still update contact details, service schedules for future periods, and other non-billing-critical data during a run.

> **PD-270:** *"The ERP remains fully operational for users, allowing them to handle other customer-related tasks while invoices are being generated."*

The lock is field-level and customer-scope-level — not a full system lock. Only the specific fields for the specific customers currently in the run are locked.

---

## 6. Validation Report Pattern

The validation report appears in multiple places in the requirements. It is always the same structure — a list of items that failed, categorised by severity and type.

> **PD-272:** *"The simulation feature generates a summary report from the simulation, providing insights into potential errors, missing information, and financial overviews."*

> **PD-271:** *"A summary report of detected errors is generated, allowing users to review and resolve issues efficiently. Errors are categorised (e.g. missing mandatory fields, incorrect financial data)."*

> **PD-278:** *"The system compiles an error list of failed events, allowing billing clerks to correct issues before invoicing."*

A ValidationReport contains:
- totalChecked (int): how many events or invoices were evaluated
- passed (int): how many passed all checks
- failures (list): each failure has — entity ID, entity type (event or invoice), rule that failed, severity (BLOCKING or WARNING), description of the issue

Blocking failures prevent progression. Warning failures are reported but do not block. The user must see both — not just the blocking ones.

---

## 7. The Preview-Then-Apply Pattern

Two features use a two-phase commit pattern where the user previews the effect before committing:

> **PD-319:** *"The solution also includes tools for validating and previewing the impact of retroactive price changes before they are finalised."*

> **PD-364:** *"Users with sufficient access rights can define a start date for when the change to service responsibility comes into effect, at which point the system identifies and updates all related uninvoiced events."*

The pattern is:
1. User calls a `preview` endpoint with their intended operation parameters
2. The system computes what would change and returns it — no changes are made
3. The user reviews the list of affected entities
4. User calls an `apply` endpoint referencing the preview
5. The changes are committed in a single transaction

The preview result is not stored permanently — it is held temporarily (e.g. in-memory or in a short-lived cache) for the duration of the user's review. If the user does not apply within a reasonable time, the preview expires and they must re-run it.

This pattern prevents surprises. A retroactive price change affecting 10,000 events is a significant operation — the user must see what they are about to do before doing it.

---

## 8. The Shared Service 100% Rule

This constraint appears once in the requirements but it is fundamental to the correctness of shared service invoicing.

> **PD-280:** *"The sum of all percentage shares must equal exactly 100%, and the application must return an error message if the total is not exactly 100%."*

This check must happen:
- On every write to a PropertyGroup's participant list
- Before generating invoices for any shared service event
- When a retroactive participant is added (PD-279) — after redistribution, totals must still be 100%

The check must use exact decimal arithmetic, not floating point. A sum of 99.99% or 100.01% must fail. The constraint cannot be expressed as "approximately 100%". Use `BigDecimal` with scale 2 and `ROUND_HALF_UP` throughout.

---

## 9. Event Date Drives VAT Rate — Not Today's Date

This is a subtle but critical rule that appears in the requirements.

> **PD-299:** *"VAT rates tied to the event date."*

> **PD-296:** *"The system supports multiple VAT rates, including rates that vary based on the event date, ensuring compliance with national regulations."*

If an event happened in January when VAT was 24%, and the invoice is generated in March when VAT has changed to 25.5%, the January VAT rate (24%) must apply to that event on the invoice. The rate is fixed at the time the event occurred — not at the time the invoice is generated.

This affects the VatRateRepository query (which queries by event date, not today's date) and the FinvoiceBuilderService (which reads the rate already stored on the event, not a freshly resolved one).

---

## 10. WasteHero is the Master for Billing Data

> **PD-281:** *"I want the WasteHero application to be the source of truth for customer invoicing information so that I can ensure that all the required invoicing information is up to date."*

This is an architectural principle that affects the integration design:
- When WasteHero and the external invoicing system have conflicting billing addresses, WasteHero's version is correct
- When WasteHero updates a billing address, the external system must accept the update — not the other way around
- Integration flows that push data FROM an external system INTO WasteHero (like the e-invoice operator) do not override manually set data (PD-282 lock flag)

The external invoicing system manages payment processing, customer-facing delivery, and archiving. WasteHero manages the customer relationship data and billing configuration. Each system is master of its own domain.
