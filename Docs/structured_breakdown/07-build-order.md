# 07 — Build Order
## What to build first, what depends on what, and why the sequence matters

The sequence below is not arbitrary. Each step unlocks the next. Building out of order means you will need to rewrite things you already built. The reasoning for each step's position is grounded in the requirements.

---

## Step 1 — Master Data Foundation

**Build first:** AccountingAccount, CostCenter, VatRate, Product, ProductTranslation, InvoiceNumberSeries

**Why first:**

> **PD-296:** *"Financial data is defined through a structured pricing and cost framework in which price lists, cost centers, VAT classifications, and accounting accounts are predefined at the product and service level."*

Every billing event, invoice line item, and FINVOICE row references these entities. Nothing else can be built correctly without them existing. The VatRate in particular has a critical dependency rule — it is queried by event date, not by today's date. Getting this data model right at the start prevents a major rework later.

> **PD-308:** *"Products have separate names defined for each language."*

ProductTranslation belongs here too — it is master data, and invoice generation needs it immediately.

The InvoiceNumberSeries goes here because its structure affects how invoices work fundamentally — you need to understand how numbers are issued and released before you design the Invoice entity.

**What you can test:** You can load test data, verify that VAT rate queries by event date work correctly, and verify that product translations resolve by language code. Nothing is operational yet but the foundation is correct.

---

## Step 2 — Customer Billing Profile

**Build next:** BillingProfile, EInvoiceAddress, CustomerType, ClassificationRule, LegalClassification

**Why second:**

> **PD-298:** *"If critical information is missing, the system must prevent the finalisation of an invoice until the missing data has been added."*

You cannot build invoice generation without knowing what customer data is required and how it is validated. BillingProfileValidationService needs to exist before InvoiceGenerationService can call it.

The ClassificationRule and LegalClassification belong here because:

> **PD-285:** *"The system automatically determines the correct classification based on predefined criteria such as customer type, product or service classification, or regional regulations."*

Classification is assigned at event creation time — and you are about to build event creation in Step 3. The rules must exist before events can be created correctly.

**What you can test:** Create a customer with a complete billing profile. Validate it. Break it (remove a required field) and confirm validation fails with the right error message. Create classification rules and confirm they resolve correctly for different customer/product combinations.

---

## Step 3 — BillingEvent Core

**Build next:** BillingEvent entity (full schema from PD-299), BillingEventStatus state machine (PD-297), BillingEventService (create, manual create, edit with audit), BillingEventAuditLog

**Why third:**

> **PD-299:** *"An event must be created in the application such that invoicing data can be generated from the same event."*

This is the core entity. Everything downstream — invoices, FINVOICE, shared service splits, billing cycles, simulations — is generated from BillingEvents. It must be complete before anything else is built.

The status state machine (PD-297) must be built simultaneously with the entity — you cannot add it later without risk of invalid states existing in the database.

The audit log (PD-277) must also be built now. If you add it later, you will have a period of production data with no audit trail — a compliance problem.

> **PD-297 (PJH):** *"The 'Sent' status should mean the billing file has actually been sent, at which point no further changes to the invoice content should be allowed."*

The immutability gate must be in place from the start. If you build editing first and add the gate later, any existing edits in the system will have no audit record.

**What you can test:** Create events via the API. Verify that all schema fields are stored. Verify status transitions: IN_PROGRESS → SENT works; editing a SENT event is rejected. Verify audit log entries are written on every edit.

---

## Step 4 — Exclusion, Transfer, and Driver Events

**Build next:** Event exclusion (PD-318), event transfer between customers (PD-276, PD-344), driver event submission (PD-228), office review workflow, BillingEventTransferLog

**Why fourth:**

These features are all pre-billing corrections that operate on individual events. They must exist before the billing run is built because the run must correctly skip excluded events and respect transfers.

> **PD-318:** *"Events marked as excluded remain in the system but are flagged as non-billable."*

If the billing run is built before exclusion logic, it will not know which events to skip.

> **PD-228:** *"It is necessary to separately define which events/additional tasks can proceed directly to the invoice and which require review by the office worker."*

The office review queue must exist before invoicing is built — otherwise driver events with `requiresOfficeReview = true` would be incorrectly eligible for invoicing.

**What you can test:** Exclude an event and confirm it disappears from billing queries. Transfer an event to a new customer and confirm the transfer log. Submit a driver event, confirm it enters the review queue, approve it, confirm it becomes eligible for invoicing.

---

## Step 5 — Accounting Allocation

**Build next:** AccountingAllocationRule, AccountingAllocationService, CostCenterCompositionService, VatCalculationService (standard and reverse charge)

**Why fifth:**

> **PD-295:** *"Based on these rules, the system can direct charges for the same product or service in the invoicing data to different accounts and/or cost centers as part of the FINVOICE data."*

Invoice generation must call AccountingAllocationService to produce the ledger-split entries. This must be built before invoice generation. If you build invoice generation first and add allocation later, the FINVOICE rows will be wrong and will need rewriting.

Reverse charge VAT (PD-300) goes here too — it is a VAT calculation variant, not an invoice feature. It must be resolved at the line-item level before FINVOICE is built.

**What you can test:** Create an allocation rule for a product + region combination. Run the allocation service on a test event and verify the correct accounts are returned. Verify that a product marked for reverse charge VAT produces zero-rate lines with the correct metadata.

---

## Step 6 — Billing Configuration (Cycles, Bundling, Surcharges, Minimum Fee)

**Build next:** BillingCycle, BillingRestriction, BundlingRule, BillingeSurchargeConfig, MinimumFeeConfig, SeasonalFeeConfig + scheduler

**Why sixth:**

These are all configuration entities that InvoiceGenerationService will need when it is built in Step 7. Building them now means Step 7 can immediately call them — no stubbing or workarounds.

> **PD-291:** *"The system automatically sorts and processes events according to their assigned schedules."*

> **PD-290:** *"Users can define customer-specific rules for bundling transactions."*

> **PD-286:** *"The system must have a function that can be used to calculate the amount in euros, if the amount of invoicing transactions is calculated to be below the minimum payment level."*

> **PD-288:** *"The system must also be able to create invoicing material for time-bound products/services that do not have an event-specific component."*

The seasonal fee scheduler (nightly job) belongs here because it creates BillingEvents — and BillingEvent creation (Step 3) is already built. This is the first thing in the build order that can actually run in production before the invoicing pipeline is complete.

**What you can test:** Configure billing cycles and confirm that the InvoiceRun filter service groups events correctly. Configure bundling rules and confirm that the bundling service aggregates the right events. Run the seasonal fee scheduler manually and verify BillingEvents are created with the correct data.

---

## Step 7 — Invoice Generation Core

**Build next:** InvoiceGenerationService (the orchestrator), InvoiceBundlingService, LegalClassificationService at invoice level, InvoiceNumberingService, MinimumFeeService, SharedServiceInvoicingService, PropertyGroup, SharedServiceParticipant (PD-280, PD-279)

**Why seventh:**

Everything built so far was preparation for this step. Invoice generation orchestrates:
- Billing profile validation (Step 2) ✓
- Event grouping by cycle and restriction (Step 6) ✓
- Bundling (Step 6) ✓
- Accounting allocation (Step 5) ✓
- Minimum fee (Step 6) ✓
- Invoice number assignment (Step 1) ✓
- Gross/net formatting (Step 2, from BillingProfile) ✓

Shared service invoicing belongs here because it is the most complex part of invoice generation and depends on the PropertyGroup + SharedServiceParticipant data model. The 100% validation rule must be enforced here.

> **PD-280:** *"The sum of all percentage shares must equal exactly 100%, and the application must return an error message if the total is not exactly 100%."*

This is the first step where you produce a complete, structurally correct Invoice object. It will not yet produce FINVOICE XML — that comes in Step 8. But the Invoice entity with all its line items, financial amounts, and metadata will be correct.

**What you can test:** Run invoice generation for a simple customer with one contract and one billing cycle. Verify the invoice structure, line items, accounting allocations, and amounts. Test shared service distribution with four neighbours and verify each gets 25% with correct amounts. Test minimum fee enforcement with a below-threshold customer.

---

## Step 8 — FINVOICE Builder

**Build next:** FinvoiceBuilderService, FinvoiceXmlMapper (JAXB), FINVOICE 3.0 XML generation including multi-account splits, language handling, reverse charge, attachments (PD-310, PD-305, PD-304)

**Why eighth:**

> **PD-310:** *"The FINVOICE specification/definition is to be agreed during implementation. The current latest standard is Finvoice 3."*

This step transforms the Invoice object (built in Step 7) into the FINVOICE XML that actually goes to the external system. It is the most technically specific step — it requires knowledge of the FINVOICE 3.0 XSD schema and must produce schema-valid XML.

Build this after the Invoice entity is finalised. Building FINVOICE generation before the Invoice data model is stable means rewriting the XML mapper every time a field changes.

The attachment handling (PD-305, PD-304) belongs here because it is part of the FINVOICE structure. The validation rules (max 10 attachments, max 1 MB, PDF/A only) are enforced at this stage.

**What you can test:** Generate a FINVOICE XML file from a test invoice. Validate it against the official Finvoice 3.0 XSD schema — this should be an automated test. Test a mixed VAT invoice (standard and reverse charge on the same invoice). Test an invoice with an attachment. Test a batch attachment reference (identifier only, no binary).

---

## Step 9 — Invoice Run and Simulation

**Build next:** InvoiceRun entity, InvoiceRunService, InvoiceRunLockService, InvoiceSimulationService, InvoiceCancellationService, ValidationEngine (PD-272, PD-274, PD-271, PD-278, PD-270, PD-273)

**Why ninth:**

The invoice run orchestrates everything built so far into a complete end-to-end batch operation. The simulation (PD-272) and per-invoice preview (PD-274) reuse the same pipeline — simulation mode just skips the side-effect steps.

> **PD-272:** *"Through this simulation, users can preview invoice data under the same conditions as in a real invoicing run."*

The simulation guard (from 06-cross-cutting.md) is enforced at this step — invoice numbers are not consumed in simulation mode, event statuses do not change.

The ValidationEngine (PD-271) runs at this stage before invoices are finalised. The error list (PD-278) is produced here. The locking mechanism (PD-270) is activated here — at run start, all in-scope customers are locked; at run end, they are unlocked.

> **PD-273 (PJH):** *"PJH wants the billing data not to be sent automatically in the first phase."*

The manual send action and scheduled send belong here. The default for PJH is: run generates invoices, sets them to READY, stops. The billing user then manually triggers transmission.

**What you can test:** Run a full simulation and verify the SimulationReport is correct — total count, VAT breakdown, error list. Run a real run and verify that invoice numbers are consumed, event statuses change to SENT, and the lock is released at the end. Cancel a run mid-execution and verify all states revert.

---

## Step 10 — Credit Notes and Post-Invoice Corrections

**Build next:** CreditNoteService, BilledEventCorrectionService, credit note FINVOICE generation (PD-269, PD-275)

**Why tenth:**

> **PD-275 depends on PD-269.** The BilledEventCorrectionService always creates a credit note first (PD-269) and then copies events to a new invoice. CreditNoteService must exist before BilledEventCorrectionService can be built.

> **PD-269:** *"A FINVOICE-formatted file is generated for the credit note and can be delivered to an external invoicing system."*

The FINVOICE builder (Step 8) must already exist to handle credit note FINVOICE generation. That is why Step 10 comes after Step 8.

> **PD-275 (use case):** *"It is expected that each event to be corrected does not need to be manually entered into the system as a new event with all its details."*

The event copy mechanism is the core of BilledEventCorrectionService. It must copy all event data from the original, not just the financial amounts — ensuring the new invoice starts with accurate event history.

**What you can test:** Create a full credit note for an invoice. Verify the credit note FINVOICE has correct negative amounts. Verify the original invoice and credit note remain linked and visible. Test the partial credit path. Test BilledEventCorrectionService: credit an invoice, copy events, create a new invoice for the correct customer.

---

## Step 11 — Retroactive Corrections

**Build next:** RetroactivePriceAdjustmentService, ServiceResponsibilityChangeService, associated audit logs and preview-then-apply pattern (PD-319, PD-364)

**Why eleventh:**

These features require the full billing event and audit infrastructure from Steps 3 and 4 to be stable. They also require the status gate from Step 3 to be in place — the retroactive services must never touch SENT or COMPLETED events.

The preview-then-apply pattern (from 06-cross-cutting.md) is introduced here. These are the only features that use it.

> **PD-319:** *"The system ensures that retroactive changes do not affect events that have already been invoiced."*

**What you can test:** Select a date range with both invoiced and uninvoiced events. Preview a price change. Verify that only uninvoiced events appear in the preview result — invoiced events are absent. Apply the change and verify only the correct events were updated. Verify the audit log.

---

## Step 12 — Integration Layer

**Build next:** ExternalInvoicingClient, FINVOICE transmission, invoice image retrieval (PD-306), attachment retrieval (PD-303), billing address sync (PD-281), EInvoiceIntegrationService + daily scheduler (PD-107), invoice recall (PD-273)

**Why twelfth:**

The integration layer is the last thing to build because it requires the complete invoice pipeline to exist before you can test it end-to-end. Building it earlier would mean testing against incomplete invoices.

> **PD-306:** *"If an invoice image cannot be retrieved, the system provides clear error messages and troubleshooting guidance."*

> **PD-303:** *"If an attachment is missing or cannot be retrieved, the external system must return a structured error response."*

The failure handling for each integration endpoint must be explicitly defined and tested — not left to default Spring error handling.

The e-invoice integration (PD-107) needs the EInvoiceAddress lock mechanism from Step 2 (PD-282) to be in place. Confirm that the lock check happens before every integration-driven address update.

**What you can test:** Mock the external invoicing system and verify FINVOICE transmission works end-to-end. Verify that a transmission failure puts the invoice in ERROR status. Verify image and attachment retrieval, including the error cases. Verify the e-invoice integration processes START and TERMINATE messages correctly and respects the manual lock.

---

## Step 13 — Authority Access

**Build last:** AuthorityInvoiceViewController, authority role configuration (PD-171)

**Why last:**

This is a read-only view built on top of everything else. It adds no new data model, no new business logic. It is purely a security-scoped read endpoint that reuses InvoiceImageService from Step 12.

> **PD-171:** *"In the viewing view, the image of the invoice must appear as it did when it was sent to the customer."*

The image retrieval via external system (Step 12) must already work before this step. That is the only dependency.

**What you can test:** Authenticate as AUTHORITY_VIEWER. Confirm that only sent and completed invoices are visible. Confirm that draft invoices are not visible. Confirm that invoice images load correctly. Confirm that no write operation is accessible from this role.

---

## Dependency Summary

```
Step 1  (Master Data)
  └─► Step 2  (Customer Billing Profile)
        └─► Step 3  (BillingEvent Core)
              └─► Step 4  (Exclusion, Transfer, Driver)
                    └─► Step 5  (Accounting Allocation)
                          └─► Step 6  (Billing Configuration)
                                └─► Step 7  (Invoice Generation Core)
                                      └─► Step 8  (FINVOICE Builder)
                                            └─► Step 9  (Invoice Run & Simulation)
                                                  └─► Step 10 (Credit Notes)
                                                        └─► Step 11 (Retroactive Corrections)
                                                              └─► Step 12 (Integration Layer)
                                                                    └─► Step 13 (Authority Access)
```

Steps 4, 5, and 6 can be parallelised once Step 3 is complete — they do not depend on each other, only on Step 3. Steps 10 and 11 can be parallelised once Step 9 is complete.

---

## What is explicitly deferred (not MVP)

**PD-163 — Billing list / Authority invoice approval**

> **PD-163:** *"This requirement needs further clarification before it can be refined. PJH currently has no need for an event-specific billing list."*

This is the only story in all four releases that explicitly says it needs clarification before work begins. Do not attempt to build it in the MVP. Build a placeholder endpoint that returns a message saying this feature is pending requirement clarification. The authority *view* (PD-171) is in scope — the authority *approval workflow* (PD-163) is not.

**Weighbridge and cashier/POS integrations**

> **PD-299:** *"Event data can also be generated via a weighbridge integration or a cashier/POS system integration."*

The BillingEvent endpoint and manual event creation cover the MVP. The specific weighbridge and POS integration clients depend on which external systems are in scope for the first deployment — those are implementation-phase decisions, not MVP architecture decisions.
