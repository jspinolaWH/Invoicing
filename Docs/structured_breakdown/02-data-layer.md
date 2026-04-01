# 02 — Data Layer
## What data access is needed and why

Every query or persistence operation below is driven by a specific PD requirement. This file describes what the repositories need to be able to do, not how to code them.

---

## BillingEventRepository

This is the most queried repository in the system. Almost every feature touches it.

**Find unbilled events in a date range for a customer**
> **PD-319:** *"The system identifies all unbilled events for the specified time period."*
> **PD-364:** *"Users with sufficient access rights can define a start date for when the change to service responsibility comes into effect, at which point the system identifies and updates all related uninvoiced events accordingly."*

Needs to query by: customerID + date range + status = IN_PROGRESS. Used by retroactive price adjustment and retroactive service responsibility change.

**Find events by invoice run filter**
> **PD-293:** *"The following can be used as selection criteria for invoicing events per invoicing run: municipality, minimum invoice amount, invoicing period, customer type, events of a certain type of service, reception location, service responsibility."*

This query is dynamic — different runs use different combinations of filters. Must support any subset of the filter fields. Any field can be null (meaning "no filter on this field"). All active, non-excluded events in IN_PROGRESS status that match the filters.

**Find events pending office review**
> **PD-228:** *"Events must be able to be reviewed in bulk for error detection."*

Query: status = IN_PROGRESS AND requiresOfficeReview = true AND reviewedAt IS NULL.

**Find events by shared service group**
> **PD-279:** *"All events falling within the specified date range are redistributed based on the updated participation data."*

Query: sharedServiceGroupId = ? AND status = IN_PROGRESS AND eventDate >= effectiveFrom. Used when a new participant joins a shared service retroactively — all matching events need redistribution.

**Find events by project**
> **PD-287:** *"The work-site-specific events must be able to be generated on separate invoices."*

Query: projectId = ? AND status = IN_PROGRESS. Grouped by project to produce separate invoices when separateInvoice = true.

**Find events by billing cycle**
> **PD-291:** *"Events are grouped according to their billing period."*

Query: linked contract/property/service has a BillingCycle due in the current run window AND status = IN_PROGRESS.

**Find excluded events**
> **PD-318:** *"Events marked as excluded remain in the system but are flagged as non-billable."*

Query: excluded = true for a given customer/date range. Used for review and for the audit trail — excluded events must remain visible.

**Find events with missing mandatory data**
> **PD-278:** *"An event must have mandatory data, such as accounts and cost centers, in order for it to be transferred to billing."*

Query: status = IN_PROGRESS AND (account IS NULL OR costCenter IS NULL OR vatRate IS NULL). Returns the list of events that would fail pre-billing validation.

---

## InvoiceRepository

**Find open invoices for a customer**
> **PD-281:** *"Once the new billing address has been updated, it is applied to all open invoices and payment reminders for that customer in the invoicing system."*

Query: customerId = ? AND status IN (DRAFT, READY, SENT). Used when a billing address changes — all open invoices must have the new address pushed to the external system.

**Find invoices in a run**
> **PD-273:** *"Invoice datasets are available in a consolidated view, where both the status of the invoice data can be seen and the invoice data can be canceled."*

Query: invoiceRunId = ? with status summary. The consolidated view of a run.

**Find credit notes for an invoice**
> **PD-269:** *"Information about reimbursed transactions must remain in the customer's transaction data."*

Query: creditedInvoiceId = ? (finds credit notes linked to the original). Used to show the full history of an invoice including all credits issued against it.

**Find invoices by customer for authority view**
> **PD-171:** *"Authorities must have the ability to view invoices sent to customers and open the invoice view independently."*

Query: customerId = ? AND status IN (SENT, COMPLETED). Authorities only see sent invoices — not drafts.

---

## InvoiceNumberSeriesRepository

**Fetch series with pessimistic lock**
> **PD-309:** *"The same invoice number must not be reused."*

This query must use a write lock. Two simultaneous invoice runs cannot be assigned the same number. The query fetches a specific series by ID and holds an exclusive lock until the number is assigned and the transaction commits.

**Find series by type for invoice type routing**
> **PD-309:** *"Invoicing users can select from pre-defined invoice number series (e.g., sludge invoice, credit invoice, public or private waste collection, annual invoice)."*

Query: find the active series for a given invoice type/category code. The run selects the right series based on what kind of invoice is being generated.

---

## CustomerBillingProfileRepository

**Find customers with missing billing data**
> **PD-298:** *"The application must notify invoicing users of invalid data and lists the issues for the users to correct the data before continuing invoicing."*

Query: customers where businessId IS NULL OR billingAddress IS NULL OR deliveryMethod IS NULL. Run as a pre-flight check before a billing run starts.

**Find customers by delivery method**
> **PD-294:** *"The invoicing-method surcharge must be generated automatically for customers to whom the given invoicing method applies."*

Query: deliveryMethod = PAPER (or EMAIL, or DIRECT_PAYMENT). Used to automatically apply the correct surcharge type during invoice generation.

---

## SeasonalFeeConfigRepository

**Find fees due for generation today**
> **PD-288:** *"Once these fees have been configured, they are automatically recognised as billable events, ensuring that all relevant charges are generated correctly and on time."*

Query: active = true AND nextDueDate <= today. The nightly scheduler calls this and creates BillingEvents for each result. After generating, nextDueDate is advanced by the frequency period.

---

## BillingCycleRepository

**Find cycles due for the current run**
> **PD-291:** *"The system automatically sorts and processes events according to their assigned schedules, preventing conflicts between different billing cycles."*

Query: nextBillingDate <= runDate AND frequency matches the run type. The invoice run service uses this to know which contracts/services/properties have a billing cycle ending in this run window.

---

## ValidationRuleRepository

**Find all active rules for a company**
> **PD-271:** *"Validation rules are configurable. Companies can define their own error-checking rules based on their invoicing requirements."*

Query: companyId = ? AND active = true. Called at the start of every invoice validation pass. Rules are evaluated in order — blocking rules first.

---

## AccountingAllocationRuleRepository

**Find allocation rule for a product/region combination**
> **PD-295:** *"The same product must be able to be routed to a different account and cost center based on e.g. price list or region."*

Query: productId = ? AND (region = ? OR region IS NULL) ORDER BY specificity DESC. Most-specific rule wins — a rule with both productId and region beats one with only productId. Fallback to the default rule if no match.

---

## VatRateRepository

**Find the applicable VAT rate for an event date**
> **PD-299:** *"The same invoice can include multiple VAT rates. VAT rates tied to the event date."*

> **PD-296:** *"The system supports multiple VAT rates, including rates that vary based on the event date, ensuring compliance with national regulations."*

Query: productId = ? AND validFrom <= eventDate AND (validTo IS NULL OR validTo >= eventDate). This is critical — the VAT rate is resolved at event time, not at invoice generation time. If VAT changed between when the event happened and when the invoice was generated, the event date's rate applies.

---

## SharedServiceParticipantRepository

**Find participants active at a given date**
> **PD-279:** *"Participants can be linked to a shared service retroactively as members of the shared service."*

Query: propertyGroupId = ? AND validFrom <= date AND (validTo IS NULL OR validTo >= date). Used for retroactive redistribution — the query finds who was participating at each event date, not just who is currently participating.

**Validate total shares**
> **PD-280:** *"The sum of all percentage shares must equal exactly 100%, and the application must return an error message if the total is not exactly 100%."*

Query: SUM(sharePercentage) WHERE propertyGroupId = ? AND validFrom <= today AND (validTo IS NULL OR validTo >= today). This aggregate query is run before any invoicing of shared service events.

---

## PropertyGroupRepository

No complex queries needed here — standard CRUD. But the repository must support:

**Find all groups a customer participates in**
> **PD-280:** *"The application notifies me if a subscriber has not been allocated a share of the shared service to which they have subscribed."*

Query: find all PropertyGroup entities where there is a SharedServiceParticipant record for this customerId. Used to check for incomplete allocations before a billing run.

---

## ActiveRunLockRepository

**Check if a customer is locked**
> **PD-270:** *"Certain critical data fields are locked during invoice generation to prevent inconsistencies: Billing address cannot be changed if it affects invoices currently being generated."*

Query: customerId = ? AND runId = (any active run). Must be fast — this check happens on every billing profile write attempt during a run. An index on customerId is essential.

**Lock and unlock all customers in a run**
> **PD-270:** *"Once the billing run is complete, all functionalities are fully restored."*

Insert all customer IDs for a run at run start. Delete all records for a runId at run end. This is a bulk operation — performance matters when runs involve thousands of customers.

---

## BillingEventAuditLogRepository

**Find all audit log entries for an event**
> **PD-277:** *"The system generates before-and-after records, ensuring transparency and providing references for customer inquiries or internal audits."*

Query: eventId = ? ORDER BY changedAt DESC. Used when a dispute arises — the full change history of the event is retrievable.

**Find changes made by a specific user**
> **PD-277:** *"The system logs all retroactive changes and stores details such as the user who made the change."*

Query: changedBy = ? AND changedAt BETWEEN ? AND ?. Used for internal audits of what a specific user has done.

---

## PriceAdjustmentRunRepository / ServiceResponsibilityChangeLogRepository

Both are append-only audit stores.

> **PD-319:** *"In addition, price adjustments are logged, including the reason for the change, the user who performed the update, and the events affected by the change."*

> **PD-364:** *"Role-based access rights ensure that only authorised personnel can make retroactive changes."*

These are queried only for audit/review purposes — they are never used in the main billing flow. Find by companyId + date range, or find by userId to review a specific person's retroactive actions.

---

## InvoiceAttachmentRepository

**Find attachments for an invoice**
> **PD-303:** *"When the sent invoice is opened for review, the attachments sent with the invoice are also seen."*

Query: invoiceId = ?. Returns all attachment metadata (not the binary content — that is fetched from the external system on demand).

**Count attachments for an invoice**
> **PD-305:** *"There may be no more than ten attachments and their total size may not exceed one megabyte before encoding."*

Query: COUNT(*) WHERE invoiceId = ?. And SUM(sizeBytes) WHERE invoiceId = ?. Both checked before adding a new attachment.

---

## Key data integrity rules that the data layer must enforce

These cannot be left to application logic alone — they need database-level constraints:

1. **Invoice number uniqueness** — UNIQUE constraint on the invoice number column. No two invoices ever share a number.

2. **Shared service percentages** — A CHECK constraint or application-enforced rule: the sum of sharePercentage for active participants in a PropertyGroup must equal 100.00 at all times.

3. **BillingEvent status transitions** — Enforced at the application layer (not database), but the data layer must never allow direct status writes that bypass the status service.

4. **Audit log immutability** — The audit log tables must have no UPDATE or DELETE permissions granted to the application user. They are insert-only.

5. **Excluded events** — When excluded = true, the event must never appear in billing run queries. This filter must be in every relevant query definition, not left to calling code to add.
