# 03 — Business Logic
## What services exist, what they do, and why the requirements demand them

Services are grouped by domain area. Each service owns a specific concern. No service does two unrelated things.

---

## AREA 1: Event Management

### BillingEventService

The main service for creating and editing billing events. Most other services call into this one or produce events that end up here.

**Creating events from operational sources**
> **PD-299:** *"Event data can also be generated via a weighbridge integration or a cashier/POS system integration."*

Receives event data from upstream systems (route management, weighbridge, POS) and persists a BillingEvent with status IN_PROGRESS. Applies all defaults from the product and customer configuration at creation time — account code, cost centre, VAT rate (resolved by event date), legal classification.

**Creating manual events**
> **PD-283:** *"Users can create invoices manually by selecting the appropriate product in the system, ensuring that financial classifications remain consistent."*

> **PD-283 (use case):** *"The waste company invoices expert work at an hourly rate. The invoice is made under the 'expert work' of the product manually, without the hourly tracking of the work entering the ERP system."*

When a user creates a manual event, they pick a product from the catalogue. The service loads all financial metadata from the product (account, cost centre, VAT, legal classification) and applies it to the event. The user cannot override these — they are inherited from the product to ensure accounting consistency.

**Editing events before invoicing**
> **PD-277:** *"Authorized users can edit key event data, including product quantities, event type, contractor fees, and customer price."*

> **PD-277:** *"Every change is logged, including the user who made the change, a timestamp, and the reason for the correction."*

The service accepts a partial update (only the fields being changed), checks the event's current status (editing is blocked at SENT or COMPLETED), applies the changes, and writes a BillingEventAuditLog record for every changed field with old value, new value, user, timestamp, and reason. The reason field is mandatory on every edit.

**Excluding events from invoicing**
> **PD-318:** *"Users can exclude an individual event from invoicing."*
> **PD-318:** *"Bulk exclusion: Office staff can select and exclude multiple events from invoicing in a single step."*

Sets excluded = true with reason, excludedBy, excludedAt. Does not delete. Both single and bulk operations. The service also supports reinstating a previously excluded event (setting excluded = false), which requires a reason.

**Separating contractor payment from customer invoicing**
> **PD-318:** *"The system also supports situations where services or products are not invoiced to the customer but still require contractor compensation."*

When an event is excluded from customer billing, if there is a contractor linked to the event who performed the service, the service routes the contractor payment record to a separate contractor payment workflow. The event is excluded from the customer invoice but the contractor still gets paid.

---

### BillingEventStatusService

A single-purpose service that owns the status state machine. Nothing else changes the status field.

> **PD-297:** *"Events can be edited until they are marked as completed and the invoice has been successfully transferred to the invoicing system."*

> **PD-297 (PJH):** *"The 'Sent' status should mean the billing file has actually been sent, at which point no further changes to the invoice content should be allowed."*

Allowed transitions:
- IN_PROGRESS → SENT: triggered when FINVOICE is generated and the file has been physically transmitted
- SENT → ERROR: triggered when the external system reports a transfer failure
- SENT → COMPLETED: triggered when the external system confirms receipt
- ERROR → SENT: triggered after the user corrects the data and re-sends

Any attempt to transition to a state not in the allowed map throws an exception. Any mutation of an event in SENT or COMPLETED status is rejected here before the edit even reaches the database.

---

### DriverEventService

Handles events submitted from the driver mobile app.

> **PD-228:** *"Events such as waste collection, waste weighing at a sorting station, or additional services performed on site are recorded with the assigned driver and vehicle details."*

> **PD-228:** *"It is necessary to separately define which events/additional tasks can proceed directly to the invoice and which require review by the office worker."*

When a driver submits an event, this service:
1. Records driver ID and vehicle ID on the event
2. Looks up the EventTypeConfig for this event/additional-task type
3. Sets requiresOfficeReview based on the config
4. If requiresOfficeReview = false, the event flows directly toward invoicing. If true, it sits in a review queue until an office user approves or rejects it.

---

### BillingEventTransferService

Moves events between customers or properties.

**Transferring unbilled events to a new customer**
> **PD-276 / PD-344:** *"Uninvoiced events can be edited until they are included in an invoice. If a customer change or operational error is detected before invoicing, the system allows office staff to manually reassign these events line by line."*

> **PD-344 (use case):** *"The customer states that he has already moved out of the apartment, but has still received invoices that should no longer have come to him. The waste company's office worker transfers the uninvoiced transactions to the new resident."*

Checks that the event is IN_PROGRESS (not yet on any invoice). Moves the event to the new customer. Writes a BillingEventTransferLog with from/to customer, user, timestamp, and reason. The original customer record no longer has this event associated.

**Transferring events to the correct location**
> **PD-276 (use case):** *"The driver has acknowledged the emptying of the container to the wrong destination. Before the invoice is sent, the error is noticed."*

Same service, different input — instead of a new customerId, the caller provides a new propertyId. All reporting metadata (municipality, location, reception site) is updated accordingly.

---

### RetroactivePriceAdjustmentService

Corrects prices on unbilled events in bulk.

> **PD-319:** *"Users can change prices based on percentage or fixed euro adjustments. These updates can be linked to products, services, or customer contracts."*

> **PD-319:** *"The solution also includes tools for validating and previewing the impact of retroactive price changes before they are finalised."*

> **PD-319 (example):** *"In February, the employee notices this during a review and uses WasteHero's retroactive price change feature. They select the period (1 January–31 January 2025) and the relevant products. The system updates the container and base fees by 3%."*

Works in two phases:
1. **Preview phase**: given a filter (date range + product selection) and an adjustment (% or fixed €), the service returns a list of affected events with their current prices and what the new prices would be. Nothing is changed yet.
2. **Apply phase**: after the user confirms the preview, the service applies the changes, recalculates totals on each affected event, and writes a PriceAdjustmentRun audit record.

The critical guard: events already on an invoice (status != IN_PROGRESS) are never touched, even if they fall within the selected date range.

---

### ServiceResponsibilityChangeService

Corrects service responsibility classification on unbilled events.

> **PD-364:** *"It must also be possible to make the change afterwards so that the service responsibility changes to non-billable events for the desired customers retroactively from the desired date."*

> **PD-364 (scenario 2):** *"A business customer was mistakenly assigned TSV service responsibility (municipal secondary responsibility). The error is detected after reviewing the service records. An office worker performs a retroactive update and corrects the responsibility from the date the error began."*

Same two-phase pattern as PD-319: preview first, then apply. Writes a ServiceResponsibilityChangeLog. Only IN_PROGRESS events are affected. Issued invoices are never touched.

---

## AREA 2: Invoice Generation

### InvoiceGenerationService

The orchestrator. This service calls all the others in the right order to produce an invoice from a set of BillingEvents.

> **PD-310:** *"The FINVOICE material is to be generated during the invoicing run."*

For each customer-cycle group of events, the service:
1. Validates that the customer's BillingProfile is complete (PD-298)
2. Checks legal classification and decides combined vs separate invoicing (PD-284, PD-285)
3. Applies bundling rules to group events into line items (PD-290)
4. Resolves the invoice template code (PD-307)
5. Resolves the customer's language (PD-308)
6. Applies seasonal fees if due (PD-288)
7. Applies gross/net setting (PD-301)
8. Applies reverse charge VAT where products require it (PD-300)
9. Applies billing surcharge if the delivery method requires one (PD-294)
10. Checks minimum fee and adds adjustment line if needed (PD-286)
11. Assigns an invoice number from the correct series (PD-309)
12. Calls FinvoiceBuilderService to produce the FINVOICE XML (PD-310)
13. Calls ValidationEngine to check the result (PD-271)
14. Sets invoice status to DRAFT

If simulation mode is active, steps 11 (number assignment) and 12 (FINVOICE transmission) are skipped. Numbers are not consumed. Statuses do not change.

---

### InvoiceBundlingService

Converts a flat list of BillingEvents into a structured list of invoice line items.

> **PD-290:** *"Users can define customer-specific rules for bundling transactions so that I can avoid transaction-specific invoice rows for those same transactions."*

> **PD-290 (use case):** *"For one customer, all waste bin emptyings are wanted on the same line, basic payments on their own line and all waste pallet imports on their own separate lines."*

Reads the customer's BundlingRules. Groups events by the rule that matches their product group. For events covered by a SINGLE_LINE rule, aggregates their quantities and amounts into one line item. For events covered by a SEPARATE rule, each event becomes its own line item. For events not covered by any rule, the default behaviour is applied (one line per event). The underlying BillingEvent records are never modified.

---

### InvoiceSimulationService

Runs the full generation pipeline without any side effects.

> **PD-272:** *"Through this simulation, users can preview invoice data under the same conditions as in a real invoicing run, ensuring accuracy before invoices are finalized and sent to customers."*

> **PD-272:** *"The simulation generates data that reflects the invoice structure but is marked as test data, preventing accidental processing."*

Calls InvoiceGenerationService in dry-run mode. Collects all generated Invoice objects in memory. Produces a SimulationReport: total count, sum totals, VAT breakdown by rate, cost centre breakdown, and a list of all events or invoices that would fail validation. Nothing is persisted. No invoice numbers are consumed.

**Single-invoice preview**
> **PD-274:** *"Users can generate a draft invoice that replicates the final invoice, including all financial and service-related data."*

Same service, scoped to a single customer. Returns the full invoice structure as a response — not saved to the database.

---

### LegalClassificationService

Determines whether each transaction is public-law or private-law.

> **PD-285:** *"The system automatically determines the correct classification based on predefined criteria such as customer type, product or service classification, or regional regulations."*

Evaluates ClassificationRules in priority order. The first rule whose conditions all match (customer type, product code, region) determines the classification. If no rule matches, a default classification (configurable per company) is applied. The classification is set on the BillingEvent at creation time — it is not re-derived at invoice time.

**Combined vs separate invoicing**
> **PD-284:** *"Users can choose whether public and private sales transactions are invoiced on the same invoice or on separate invoices."*

> **PD-285 (PJH question):** *"Does the decision on the approach need to be made before go-live, and can the operating model be changed or selected after go-live?"*

The answer to PJH's question is yes — the `CompanyInvoicingConfig.combinedPublicPrivateInvoicing` flag is toggleable at any time. InvoiceGenerationService reads it before deciding whether to split events into two invoice batches or keep them together. When combined, each line item carries its classification identifier in the FINVOICE data. When separate, each invoice is entirely one classification.

---

### InvoiceNumberingService

Assigns invoice numbers atomically.

> **PD-309:** *"Waste management companies can define the structure of the invoice number in an invoicing template. The invoice number must always be unique for each invoice."*

> **PD-273:** *"If an entire invoice dataset is canceled, the invoice numbers are returned and released for reuse."*

Fetches the correct InvoiceNumberSeries with an exclusive lock. Increments the sequence counter. Returns the formatted number. If the invoice is subsequently cancelled, the number is added to the released number pool on the series — but it is never re-issued. The pool tracks released numbers purely for audit purposes.

---

### MinimumFeeService

Enforces minimum charge requirements.

> **PD-286:** *"The system calculates the total amount of the invoice for the billing period and checks whether it meets the minimum charge requirement. If the final invoice amount is below the defined minimum, the system adds a surcharge to cover the difference."*

> **PD-286 (PJH):** *"The total amount used for comparison must be VAT 0% (net invoicing)."*

Called by InvoiceGenerationService after all line items are assembled. Computes the net (VAT 0%) total. Reads the MinimumFeeConfig. If the net total is below the minimum:
- Checks the contract period — if the contract started after the billing period start, or ended before the billing period end, the minimum does NOT apply
- If the minimum applies, adds an adjustment line item for the difference

---

### SharedServiceInvoicingService

Distributes shared service costs across participants.

> **PD-280:** *"Each subscriber in a shared service is assigned an individual invoice line, with the amount determined according to their percentage share."*

> **PD-280:** *"The sum of all percentage shares must equal exactly 100%."*

For each BillingEvent tagged with a sharedServiceGroupId, this service:
1. Validates that the active participant percentages sum to exactly 100% — throws if not
2. Creates a per-subscriber invoice line for each participant: amount = totalEventCost × sharePercentage
3. Each line includes: the service description, the subscriber's share percentage, and the total service cost for transparency

**Retroactive participant addition**
> **PD-279:** *"When a new participant is added to the shared service retroactively, all events falling within the specified date range are redistributed based on the updated participation data."*

When a new participant is added with a validFrom date in the past:
1. Finds all IN_PROGRESS events for this property group where eventDate >= validFrom
2. Re-runs the distribution calculation with the new participant included
3. Updates existing participant lines and creates the new participant's line
4. Validation: total distributed amounts still sum to the original service cost

---

### BillingeSurchargeService

Adds invoicing method surcharges to invoices.

> **PD-294:** *"The invoicing-method surcharge must be generated automatically for customers to whom the given invoicing method applies. However, it must be possible to globally toggle the invoicing-method surcharge on and off."*

> **PD-294 (clarification):** *"A separate product is created for the invoicing surcharge. The surcharge is shown automatically whenever the customer is to receive a paper invoice. The invoicing user can switch it off if necessary."*

Checks: global toggle on? Does the customer's delivery method have a configured surcharge product? Has the surcharge been manually removed from this specific invoice? If all checks pass, adds the surcharge product as a line item with the correct amount for the customer type (business vs consumer rates may differ).

---

## AREA 3: Invoice Run Management

### InvoiceRunService

Orchestrates a complete billing run from filter configuration to invoice generation for all matching customers.

> **PD-270:** *"The WasteHero system ensures that customer data can be processed and updated even during billing runs, allowing users to carry out their daily tasks without disruption."*

At run start: calls InvoiceRunLockService to lock all in-scope customers' billing addresses and billing groups. Notifies the system that a run is active. Calls InvoiceGenerationService for each customer-cycle group. At run end: releases all locks.

**Grouping events by billing cycle**
> **PD-291:** *"The system automatically sorts and processes events according to their assigned schedules, preventing conflicts between different billing cycles."*
> **PD-292:** *"Immediate invoicing for certain events and scheduled invoicing for recurring services."*

Before generating invoices, the run service separates events into groups:
1. IMMEDIATE events (per BillingRestriction config) — processed immediately, each potentially on their own invoice
2. Cycle-based events — grouped by their billing cycle window into batch invoices

---

### InvoiceRunLockService

Controls which customer fields are read-only during a billing run.

> **PD-270:** *"Billing address: cannot be changed if it affects invoices currently being generated. Billing groups: cannot be modified to prevent changes to invoice bundling."*

> **PD-270 (example):** *"A user attempts to change the billing address for a customer whose invoice is currently being generated. The system blocks the change and displays the message: 'Invoice processing in progress. Address changes cannot be made during this time.'"*

Maintains an ActiveRunLock table. Any service that modifies a billing address or billing group calls `isLocked(customerId, fieldName)` first. If locked, it returns a 423 Locked response with the message from PD-270. When the run finishes (success or failure), all locks for that run are released.

---

### InvoiceCancellationService

Handles cancellation before and after transmission.

> **PD-273:** *"Users can cancel the sending of invoices to customers or a third-party invoicing system after the invoices have been generated in an invoicing run so that I can prevent invoices with errors or incomplete information from being sent."*

> **PD-273:** *"When an invoice dataset is canceled, its status is reverted to the previous state or the invoice data is deleted entirely."*

**Before transmission**: sets invoice status to CANCELLED, releases invoice number back to the released pool (but never re-issues it), reverts affected BillingEvent statuses back to IN_PROGRESS.

**After transmission to external system (if integration supports recall)**:
> **PD-273:** *"I want to be able to cancel the sending of invoices from a third-party invoicing system after they have been sent from the WasteHero application to that system, if supported by the integration."*

Calls the external invoicing system's recall API. Updates invoice status based on the recall result. If recall is not supported, the user is told so and must handle it externally.

**Deferred send**
> **PD-273 (PJH):** *"PJH wants the billing data not to be sent automatically in the first phase, but this operating model may change after go-live. PJH wants the billing data to be sent by the billing user."*

Supports a `scheduledSendAt` timestamp on the InvoiceRun. A scheduler job checks for runs with a due send time and triggers transmission. When no scheduled time is set, a manual send action is required.

---

## AREA 4: Validation

### InvoiceValidationEngine

Runs all configured validation rules against an invoice before finalisation.

> **PD-271:** *"An automatic error-checking function ensures that all required invoicing data fields are correct before invoices are finalized. Checks are performed during invoice creation. If an error is detected, the system blocks invoice processing until the issues are corrected."*

Loads all active ValidationRules for the company. Evaluates each against the invoice being validated. Collects failures. Blocking failures prevent the invoice from moving from DRAFT to READY. Warning failures are reported but do not block. Returns a full ValidationResult with the list of failures categorised by severity.

**Configurable rules**
> **PD-278 (use case):** *"An employee of the waste company builds an automation where an alert is triggered for all billing events with more than 30 container collections."*

The ValidationRule entity stores the rule configuration as structured data. The engine maps rule types to implementations:
- MANDATORY_FIELD: checks a specific field is not null/empty
- PRICE_CONSISTENCY: checks the event price matches the price list price
- QUANTITY_THRESHOLD: checks a quantity field does not exceed a configured maximum
- CLASSIFICATION: checks that legal classification is set and valid

Each rule implementation receives the ValidationRule's config and the invoice/event being checked. Companies can create as many rules of each type as they need.

---

### BillingEventValidationService

Pre-flight validation at the event level — before events enter a run.

> **PD-278:** *"An event must have mandatory data, such as accounts and cost centers, in order for it to be transferred to billing."*

Called during the simulation run and optionally as a standalone check. Returns a list of events with missing mandatory fields. These events are flagged in the error report and excluded from the run until corrected.

---

### BillingProfileValidationService

Validates that a customer has everything needed to receive an invoice.

> **PD-298:** *"The application must notify invoicing users of invalid data and lists the issues for the users to correct the data before continuing invoicing."*

Checks: CustomerID present (6–9 digits), delivery method set, billing address complete, BusinessID present (for e-invoicing), e-invoice address set (if delivery method = E_INVOICE). Returns a structured list of issues. InvoiceGenerationService calls this before starting generation for each customer. If the profile is incomplete, generation for that customer is skipped and the issues are added to the run's error report.

---

## AREA 5: Credit Notes and Corrections

### CreditNoteService

Creates credit notes against existing invoices.

> **PD-269:** *"An invoice can be fully credited or partially credited. A partial credit can be issued, for example, when the customer has not yet paid the invoice."*

> **PD-269:** *"The credit note is assigned the number of the invoice being credited."*

> **PD-269:** *"A FINVOICE-formatted file is generated for the credit note and can be delivered to an external invoicing system."*

For a full credit: creates a Credit Note invoice with all line items negated, assigns the original invoice's number as the credit reference, generates FINVOICE with negative amounts, writes to customer transaction history.

For a partial credit: creates a Credit Note invoice with only the selected line items negated.

For a batch of credits:
> **PD-269:** *"A credit note can be created ... as a separate batch run if, for example, credit notes are to be created for several different invoices at the same time."*

Accepts a list of invoice IDs with credit instructions, processes all of them in a single transaction.

**Custom text and internal notes**
> **PD-269:** *"Users can define custom text for the credit note and also add an internal comment explaining the reason for the credit."*

Both the customer-visible credit text and the internal reason comment are required inputs on every credit note creation. Neither is optional.

---

### BilledEventCorrectionService

Handles the case where an already-invoiced event needs to be corrected and re-billed.

> **PD-275:** *"In a crediting situation, some of the events can be transferred to be re-invoiced."*

> **PD-275 (PJH):** *"The system must allow creating a credit note and a new invoice with corrected information, either for the same customer or for another customer."*

> **PD-275 (use case):** *"In the above cases, it is expected that each event to be corrected does not need to be manually entered into the system as a new event with all its details."*

This is always a two-step operation:
1. Create a credit note for the original invoice (calls CreditNoteService)
2. Copy the events from the original invoice to a new invoice (same or different customer), preserving all original event data

Copying means the new invoice starts with all the same event data, but the user can modify what needs correcting (customer, quantity, price, etc.) before the new invoice is generated. The original invoice and the credit note remain permanently visible on the original customer's account.

---

## AREA 6: Accounting and Financial Allocation

### AccountingAllocationService

Resolves which ledger accounts and cost centres apply to each event.

> **PD-295:** *"Based on these rules, the system can direct charges for the same product or service in the invoicing data to different accounts and/or cost centers as part of the FINVOICE data."*

> **PD-295:** *"Accounts for different products are defined in connection with product price lists. For example, VAT may be posted to a VAT account, waste treatment fees to their own account, and transport fees to a separate account."*

Given a BillingEvent, finds the most specific AccountingAllocationRule (product + region + price list). Uses that rule to split the event's total amount into ledger-account-level sub-entries. This split is what makes the FINVOICE accounting rows differ from the customer-visible invoice rows — one customer line may become three ledger entries.

### CostCenterCompositionService

Assembles the final cost centre string from multiple components.

> **PD-295:** *"The cost center information for an event may consist of multiple components and data points. These may include, for example, the product's cost center, the reception point's cost center, and the service responsibility information defined in the contract."*

Takes: product cost centre segment, reception point cost centre segment, service responsibility segment. Combines them into the final cost centre code per the company's configured format. The format itself is defined during implementation and stored in the configuration.

---

## AREA 7: Scheduling

### SeasonalFeeGenerationService

Auto-creates BillingEvents for time-bound fees.

> **PD-288:** *"Once these fees have been configured, they are automatically recognised as billable events, ensuring that all relevant charges are generated correctly and on time."*

> **PD-288 (use case):** *"A certain basic fee has been defined for each property (e.g. permanent apartment, leisure apartment), which is billed once a year. The amount of the basic fee may vary annually."*

Runs nightly. Fetches all SeasonalFeeConfig records where nextDueDate <= today. For each: creates a BillingEvent with origin = SEASONAL and all financial metadata inherited from the configured product. Advances nextDueDate by the frequency period. If the fee amount has been updated for the new year, the new amount applies automatically on the new event.

---

## AREA 8: E-Invoice and Billing Profile

### EInvoiceIntegrationService

Processes inbound messages from the invoice operator.

> **PD-107:** *"WasteHero receives information via integration from an invoice intermediary about a customer's e-invoice and direct debit orders. The integration transmits information indicating either the start or the termination of an order."*

> **PD-107:** *"The recommended approach is that changes are delivered to the system once per day."*

Runs daily. Fetches the message batch from the operator (Ropo / Maventa). For each message:
- Matches to the correct customer using the agreed identifiers (invoice reference, customer number, or both)
- If order START: sets delivery method to E_INVOICE, adds e-invoice address, logs the change
- If order TERMINATE: sets delivery method back to PAPER, removes e-invoice address, logs the change

Before updating the e-invoice address, checks the manuallyLocked flag (PD-282). If locked, skips the integration update entirely — the manually set address is preserved.

### BillingProfileSyncService

Pushes billing address changes to the external invoicing system.

> **PD-281:** *"When a customer's billing address changes, the ERP system updates the correct billing address via the integration interface. Once the new billing address has been updated, it is applied to all open invoices and payment reminders for that customer in the invoicing system."*

Triggered as a Spring Application Event whenever a billing address is updated in WasteHero. Pushes the new address to the external invoicing system via API. On success, fetches all open invoices for that customer and updates each one's delivery address. This ensures that even invoices already transmitted to the external system reach the correct address.
