# Step → PD Task Mapping
_WasteHero Invoicing Module — Spring Boot Implementation_

All PD tasks live across four Jira releases:
- **Invoicing 1** (v11193, due 22 May 2026) → [release report](https://ioteelab.atlassian.net/projects/PD/versions/11193/tab/release-report-all-issues)
- **Invoicing 2** (v11198, due 19 Jun 2026) → [release report](https://ioteelab.atlassian.net/projects/PD/versions/11198/tab/release-report-all-issues)
- **Invoicing 3** (v11199, due 28 Aug 2026) → [release report](https://ioteelab.atlassian.net/projects/PD/versions/11199/tab/release-report-all-issues)
- **Invoicing 4** (v11200, due 25 Sep 2026) → [release report](https://ioteelab.atlassian.net/projects/PD/versions/11200/tab/release-report-all-issues)

Base URL for issue links: `https://ioteelab.atlassian.net/browse/PD-XXX`

---

## Build Area 1 — Master Data Foundation

## Step 01 — Foundation Setup + VAT Rates

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |
| PD-309 | 3.4.3 Invoice numbering sequence determination | [PD-309](https://ioteelab.atlassian.net/browse/PD-309) |

**Rationale:** VatRate underpins FINVOICE output (PD-310) and invoice number assignment (PD-309). The foundation JPA auditing and `BaseAuditEntity` are scaffolding required before any story can be coded.

---

## Step 02 — Accounting Accounts

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-296 | 3.4.16 Cost centers and accounts | [PD-296](https://ioteelab.atlassian.net/browse/PD-296) |

---

## Step 03 — Cost Centers

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-296 | 3.4.16 Cost centers and accounts | [PD-296](https://ioteelab.atlassian.net/browse/PD-296) |

---

## Step 04 — Products + Translations

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-308 | 3.4.4 Invoice data based on language selection | [PD-308](https://ioteelab.atlassian.net/browse/PD-308) |
| PD-300 | 3.4.12 Reverse charge VAT | [PD-300](https://ioteelab.atlassian.net/browse/PD-300) |

---

## Step 05 — Invoice Number Series

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-309 | 3.4.3 Invoice numbering sequence determination | [PD-309](https://ioteelab.atlassian.net/browse/PD-309) |

---

## Build Area 2 — Customer Billing Profile

## Step 06 — Customer Billing Profile

**Release:** Invoicing 1 & Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-289 | 3.4.23 Accounts receivable data | [PD-289](https://ioteelab.atlassian.net/browse/PD-289) |
| PD-281 | 3.4.32 Updating billing information via API | [PD-281](https://ioteelab.atlassian.net/browse/PD-281) |
| PD-301 | 3.4.11 Gross or net invoicing | [PD-301](https://ioteelab.atlassian.net/browse/PD-301) |

---

## Step 07 — EInvoice Address

**Release:** Invoicing 3 & Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-282 | 3.4.31 Editing e-invoice data | [PD-282](https://ioteelab.atlassian.net/browse/PD-282) |
| PD-107 | 3.11.39 E-invoice integration | [PD-107](https://ioteelab.atlassian.net/browse/PD-107) |
| PD-281 | 3.4.32 Updating billing information via API | [PD-281](https://ioteelab.atlassian.net/browse/PD-281) |

**Rationale:** `EInvoiceAddress` with `manuallyLocked` flag implements PD-282. The lock prevents the daily operator sync (PD-107, built fully in Step 53) from overwriting a manually set address.

---

## Step 08 — Classification Rules

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-284 | 3.4.29 Public and private law sales – billing | [PD-284](https://ioteelab.atlassian.net/browse/PD-284) |
| PD-285 | 3.4.28 Private and public law invoices | [PD-285](https://ioteelab.atlassian.net/browse/PD-285) |
| PD-289 | 3.4.23 Accounts receivable data | [PD-289](https://ioteelab.atlassian.net/browse/PD-289) |

---

## Step 09 — Billing Profile Validation

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-271 | 3.4.42 Automatic checks on billing data | [PD-271](https://ioteelab.atlassian.net/browse/PD-271) |
| PD-278 | 3.4.35 Error listing of events | [PD-278](https://ioteelab.atlassian.net/browse/PD-278) |
| PD-274 | 3.4.39 Invoice simulation | [PD-274](https://ioteelab.atlassian.net/browse/PD-274) |

---

## Build Area 3 — BillingEvent Core

## Step 10 — BillingEvent Entity + Repository

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-299 | 3.4.13 Billing event details | [PD-299](https://ioteelab.atlassian.net/browse/PD-299) |
| PD-297 | 3.4.15 Billing event status information | [PD-297](https://ioteelab.atlassian.net/browse/PD-297) |
| PD-298 | 3.4.14 Billing data details | [PD-298](https://ioteelab.atlassian.net/browse/PD-298) |
| PD-277 | 3.4.36 Manual editing of events | [PD-277](https://ioteelab.atlassian.net/browse/PD-277) |

---

## Step 11 — BillingEvent Status Machine

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-297 | 3.4.15 Billing event status information | [PD-297](https://ioteelab.atlassian.net/browse/PD-297) |
| PD-318 | 3.3.18 Editing billing events | [PD-318](https://ioteelab.atlassian.net/browse/PD-318) |

---

## Step 12 — BillingEvent Create (External + Manual)

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-299 | 3.4.13 Billing event details | [PD-299](https://ioteelab.atlassian.net/browse/PD-299) |
| PD-283 | 3.4.30 Manual creation of billing events | [PD-283](https://ioteelab.atlassian.net/browse/PD-283) |
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |

---

## Step 13 — BillingEvent Edit + Audit Log

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-277 | 3.4.36 Manual editing of events | [PD-277](https://ioteelab.atlassian.net/browse/PD-277) |
| PD-318 | 3.3.18 Editing billing events | [PD-318](https://ioteelab.atlassian.net/browse/PD-318) |

---

## Step 14 — BillingEvent Validation

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-271 | 3.4.42 Automatic checks on billing data | [PD-271](https://ioteelab.atlassian.net/browse/PD-271) |
| PD-278 | 3.4.35 Error listing of events | [PD-278](https://ioteelab.atlassian.net/browse/PD-278) |
| PD-272 | 3.4.41 Simulation run of billing data | [PD-272](https://ioteelab.atlassian.net/browse/PD-272) |

---

## Build Area 4 — Exclusion, Transfer, Driver Events

## Step 15 — BillingEvent Exclusion and Reinstatement

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-318 | 3.3.18 Editing billing events | [PD-318](https://ioteelab.atlassian.net/browse/PD-318) |
| PD-299 | 3.4.13 Billing event details | [PD-299](https://ioteelab.atlassian.net/browse/PD-299) |

---

## Step 16 — BillingEvent Transfer

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-276 | 3.4.37 Transfer of unbilled events | [PD-276](https://ioteelab.atlassian.net/browse/PD-276) |
| PD-344 | 3.2.18 Transfer of billing events | [PD-344](https://ioteelab.atlassian.net/browse/PD-344) |
| PD-275 | 3.4.38 Transfer and copy of billed events | [PD-275](https://ioteelab.atlassian.net/browse/PD-275) |

---

## Step 17 — Driver Events and Office Review Queue

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-228 | 3.5.42 Driver and vehicle login to event | [PD-228](https://ioteelab.atlassian.net/browse/PD-228) |
| PD-177 | 3.6.34 Billing event data for reporting | [PD-177](https://ioteelab.atlassian.net/browse/PD-177) |

---

## Step 18 — Audit Logs

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-277 | 3.4.36 Manual editing of events | [PD-277](https://ioteelab.atlassian.net/browse/PD-277) |
| PD-318 | 3.3.18 Editing billing events | [PD-318](https://ioteelab.atlassian.net/browse/PD-318) |
| PD-299 | 3.4.13 Billing event details | [PD-299](https://ioteelab.atlassian.net/browse/PD-299) |

---

## Build Area 5 — Accounting Allocation

## Step 19 — Accounting Allocation Rules

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-296 | 3.4.16 Cost centers and accounts | [PD-296](https://ioteelab.atlassian.net/browse/PD-296) |

---

## Step 20 — AccountingAllocationService

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-296 | 3.4.16 Cost centers and accounts | [PD-296](https://ioteelab.atlassian.net/browse/PD-296) |
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |

**Rationale:** The `LedgerEntry` split per price component (WASTE_FEE, TRANSPORT_FEE, ECO_FEE) is the core of PD-295. Results feed the FINVOICE row generation (PD-310).

---

## Step 21 — CostCenterCompositionService

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-296 | 3.4.16 Cost centers and accounts | [PD-296](https://ioteelab.atlassian.net/browse/PD-296) |
| PD-285 | 3.4.28 Private and public law invoices | [PD-285](https://ioteelab.atlassian.net/browse/PD-285) |

---

## Step 22 — VatCalculationService

**Release:** Invoicing 1 & Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-300 | 3.4.12 Reverse charge VAT | [PD-300](https://ioteelab.atlassian.net/browse/PD-300) |
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |
| PD-301 | 3.4.11 Gross or net invoicing | [PD-301](https://ioteelab.atlassian.net/browse/PD-301) |

---

## Build Area 6 — Billing Configuration

## Step 23 — Billing Cycles

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-291 | 3.4.21 Billing cycles | [PD-291](https://ioteelab.atlassian.net/browse/PD-291) |

---

## Step 24 — Billing Restrictions

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-292 | 3.4.20 Billing restrictions | [PD-292](https://ioteelab.atlassian.net/browse/PD-292) |
| PD-293 | 3.4.19 Invoice batch filtering | [PD-293](https://ioteelab.atlassian.net/browse/PD-293) |

---

## Step 25 — Bundling Rules

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-290 | 3.4.22 Billing data generation, bundling | [PD-290](https://ioteelab.atlassian.net/browse/PD-290) |

---

## Step 26 — Surcharge Config

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-294 | 3.4.18 Billing surcharge | [PD-294](https://ioteelab.atlassian.net/browse/PD-294) |

---

## Step 27 — Minimum Fee Config

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-286 | 3.4.27 Minimum fee | [PD-286](https://ioteelab.atlassian.net/browse/PD-286) |

---

## Step 28 — Seasonal Fee Config

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-288 | 3.4.24 Seasonal fees | [PD-288](https://ioteelab.atlassian.net/browse/PD-288) |

---

## Build Area 7 — Invoice Generation Core

## Step 29 — Invoice Entity + InvoiceLineItem Entity

**Release:** Invoicing 1 & Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-298 | 3.4.14 Billing data details | [PD-298](https://ioteelab.atlassian.net/browse/PD-298) |
| PD-302 | 3.4.10 Custom and bulk invoice texts | [PD-302](https://ioteelab.atlassian.net/browse/PD-302) |
| PD-307 | 3.4.5 Invoice template selection | [PD-307](https://ioteelab.atlassian.net/browse/PD-307) |
| PD-308 | 3.4.4 Invoice data based on language selection | [PD-308](https://ioteelab.atlassian.net/browse/PD-308) |
| PD-301 | 3.4.11 Gross or net invoicing | [PD-301](https://ioteelab.atlassian.net/browse/PD-301) |
| PD-285 | 3.4.28 Private and public law invoices | [PD-285](https://ioteelab.atlassian.net/browse/PD-285) |

---

## Step 30 — Invoice Bundling Service

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-290 | 3.4.22 Billing data generation, bundling | [PD-290](https://ioteelab.atlassian.net/browse/PD-290) |

---

## Step 31 — Legal Classification Service

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-284 | 3.4.29 Public and private law sales – billing | [PD-284](https://ioteelab.atlassian.net/browse/PD-284) |
| PD-285 | 3.4.28 Private and public law invoices | [PD-285](https://ioteelab.atlassian.net/browse/PD-285) |

---

## Step 32 — Shared Services (PropertyGroup + SharedServiceParticipant)

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-280 | 3.4.33 Shared service events on the invoice | [PD-280](https://ioteelab.atlassian.net/browse/PD-280) |
| PD-279 | 3.4.34 Dynamic updates to shared service events | [PD-279](https://ioteelab.atlassian.net/browse/PD-279) |

---

## Step 33 — Shared Service Invoicing Service

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-280 | 3.4.33 Shared service events on the invoice | [PD-280](https://ioteelab.atlassian.net/browse/PD-280) |
| PD-279 | 3.4.34 Dynamic updates to shared service events | [PD-279](https://ioteelab.atlassian.net/browse/PD-279) |

---

## Step 34 — Invoice Generation Orchestrator

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-272 | 3.4.41 Simulation run of billing data | [PD-272](https://ioteelab.atlassian.net/browse/PD-272) |
| PD-274 | 3.4.39 Invoice simulation | [PD-274](https://ioteelab.atlassian.net/browse/PD-274) |
| PD-271 | 3.4.42 Automatic checks on billing data | [PD-271](https://ioteelab.atlassian.net/browse/PD-271) |
| PD-278 | 3.4.35 Error listing of events | [PD-278](https://ioteelab.atlassian.net/browse/PD-278) |

---

## Build Area 8 — FINVOICE Builder

## Step 35 — FINVOICE Core Builder

**Release:** Invoicing 1

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |
| PD-307 | 3.4.5 Invoice template selection | [PD-307](https://ioteelab.atlassian.net/browse/PD-307) |
| PD-308 | 3.4.4 Invoice data based on language selection | [PD-308](https://ioteelab.atlassian.net/browse/PD-308) |
| PD-309 | 3.4.3 Invoice numbering sequence determination | [PD-309](https://ioteelab.atlassian.net/browse/PD-309) |

---

## Step 36 — FINVOICE Advanced: Multi-Account Splits, Mixed VAT, Reverse Charge

**Release:** Invoicing 1 & Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |
| PD-295 | 3.4.17 Account and cost center data | [PD-295](https://ioteelab.atlassian.net/browse/PD-295) |
| PD-300 | 3.4.12 Reverse charge VAT | [PD-300](https://ioteelab.atlassian.net/browse/PD-300) |

---

## Step 37 — FINVOICE Attachments

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-305 | 3.4.7 PDF attachment to invoice | [PD-305](https://ioteelab.atlassian.net/browse/PD-305) |
| PD-304 | 3.4.8 PDF attachment to invoice batch | [PD-304](https://ioteelab.atlassian.net/browse/PD-304) |

---

## Build Area 9 — Invoice Run + Simulation

## Step 38 — Invoice Run Entity

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-272 | 3.4.41 Simulation run of billing data | [PD-272](https://ioteelab.atlassian.net/browse/PD-272) |
| PD-273 | 3.4.40 Cancellation option for billing data | [PD-273](https://ioteelab.atlassian.net/browse/PD-273) |
| PD-293 | 3.4.19 Invoice batch filtering | [PD-293](https://ioteelab.atlassian.net/browse/PD-293) |
| PD-270 | 3.4.48 System behavior during billing run | [PD-270](https://ioteelab.atlassian.net/browse/PD-270) |

---

## Step 39 — Invoice Run Lock

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-270 | 3.4.48 System behavior during billing run | [PD-270](https://ioteelab.atlassian.net/browse/PD-270) |
| PD-281 | 3.4.32 Updating billing information via API | [PD-281](https://ioteelab.atlassian.net/browse/PD-281) |

**Rationale:** `ActiveRunLock` enforces HTTP 423 on billing profile writes during a run. The error message "Invoice processing in progress. Address changes cannot be made during this time." is explicitly stated in PD-270. The blocked API is PD-281.

---

## Step 40 — Invoice Simulation Service

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-272 | 3.4.41 Simulation run of billing data | [PD-272](https://ioteelab.atlassian.net/browse/PD-272) |
| PD-274 | 3.4.39 Invoice simulation | [PD-274](https://ioteelab.atlassian.net/browse/PD-274) |

**Rationale:** `InvoiceSimulationService` executes the full generation pipeline but explicitly skips invoice number assignment, status changes, external transmission, and audit logging — the simulation mode guard from PD-272. `POST /invoice-runs/simulate` is the run-level simulation endpoint that expands the single-customer preview from Step 34 (PD-274) to a full batch.

---

## Step 41 — Invoice Run Orchestration

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-272 | 3.4.41 Simulation run of billing data | [PD-272](https://ioteelab.atlassian.net/browse/PD-272) |
| PD-273 | 3.4.40 Cancellation option for billing data | [PD-273](https://ioteelab.atlassian.net/browse/PD-273) |
| PD-270 | 3.4.48 System behavior during billing run | [PD-270](https://ioteelab.atlassian.net/browse/PD-270) |
| PD-291 | 3.4.21 Billing cycles | [PD-291](https://ioteelab.atlassian.net/browse/PD-291) |

**Rationale:** `InvoiceRunService` is the batch orchestrator that implements the full run lifecycle described in PD-270 and PD-272: lock customers → group events by billing cycle (PD-291) → generate per-customer → collect results → release locks. The cancel hook connects to PD-273.

---

## Step 42 — Invoice Validation Engine

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-271 | 3.4.42 Automatic checks on billing data | [PD-271](https://ioteelab.atlassian.net/browse/PD-271) |
| PD-278 | 3.4.35 Error listing of events | [PD-278](https://ioteelab.atlassian.net/browse/PD-278) |

**Rationale:** `InvoiceValidationEngine` is the configurable rule evaluation engine with four rule types: MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, and CLASSIFICATION. This is the `ValidationRule` entity described in PD-271. The structured `ValidationReport` with BLOCKING/WARNING severity directly produces the error listing in PD-278.

---

## Step 43 — Invoice Cancellation and Schedule Send

**Release:** Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-273 | 3.4.40 Cancellation option for billing data | [PD-273](https://ioteelab.atlassian.net/browse/PD-273) |
| PD-270 | 3.4.48 System behavior during billing run | [PD-270](https://ioteelab.atlassian.net/browse/PD-270) |

**Rationale:** `InvoiceCancellationService` covers PD-273 — both pre-transmission cancellation (revert statuses, release invoice numbers, remove locks) and post-transmission cancellation via recall. The scheduled send (`sendAt` datetime + scheduler trigger) is part of the system behaviour in PD-270. BILLING_MANAGER role required for post-transmission cancellation.

---

## Build Area 10 — Credit Notes

## Step 44 — Credit Notes

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-269 | 3.4.49 Credit invoices | [PD-269](https://ioteelab.atlassian.net/browse/PD-269) |

**Rationale:** `CreditNoteService` creates an `Invoice` with `invoiceType = CREDIT_NOTE` that references the original invoice. Full and partial credit (selected line items negated) are both described in PD-269. The mandatory `internalComment` field and the requirement to generate a credit FINVOICE are explicit story requirements.

---

## Step 45 — Batch Credit and Customer Credit History

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-269 | 3.4.49 Credit invoices | [PD-269](https://ioteelab.atlassian.net/browse/PD-269) |

**Rationale:** Batch credit (issuing credit notes for multiple invoices in a single transaction with a shared reason) and the `GET /customers/{id}/credit-history` endpoint extend PD-269 to cover bulk operations and the customer-facing credit history view.

---

## Step 46 — Billed Event Correction

**Release:** Invoicing 2 & Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-275 | 3.4.38 Transfer and copy of billed events | [PD-275](https://ioteelab.atlassian.net/browse/PD-275) |
| PD-269 | 3.4.49 Credit invoices | [PD-269](https://ioteelab.atlassian.net/browse/PD-269) |

**Rationale:** `BilledEventCorrectionService` implements the two-step correction flow: (1) credit note for the original invoice (PD-269), (2) copy of `BillingEvent` records to a new IN_PROGRESS state for re-invoicing. The copy (not move) of billed events maps directly to PD-275 ("Transfer and copy of billed events") — the audit trail of the original is preserved.

---

## Build Area 11 — Retroactive Corrections

## Step 47 — Retroactive Price Adjustment — Preview Phase

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-319 | 3.3.14 Price changes for unbilled events | [PD-319](https://ioteelab.atlassian.net/browse/PD-319) |

**Rationale:** `RetroactivePriceAdjustmentService` (preview phase) computes what would change — current price, new price, difference — without making any changes. The `PriceAdjustmentPreview` entity with TTL implements the preview half of the preview-then-apply pattern required by PD-319. FUNCTION_ADMIN role required.

---

## Step 48 — Retroactive Price Adjustment — Apply Phase

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-319 | 3.3.14 Price changes for unbilled events | [PD-319](https://ioteelab.atlassian.net/browse/PD-319) |

**Rationale:** The apply phase commits all price changes from a non-expired preview in a single atomic transaction and writes a `PriceAdjustmentRun` audit record plus individual `BillingEventAuditLog` entries for every changed event. This is the apply half of PD-319's preview-then-apply flow.

---

## Step 49 — Service Responsibility Change

**Release:** Invoicing 2

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-364 | 3.1.17 Retroactive changes to service responsibilities | [PD-364](https://ioteelab.atlassian.net/browse/PD-364) |

**Rationale:** `ServiceResponsibilityChangeService` uses the same preview-then-apply pattern as Steps 47/48. The user changes the legal classification or cost centre for a set of events (date range, customer, product). The `ServiceResponsibilityChangeLog` is the append-only audit record required by PD-364. FUNCTION_ADMIN role required.

---

## Build Area 12 — Integration Layer

## Step 50 — FINVOICE Transmission to External Invoicing System

**Release:** Invoicing 1 & Invoicing 3

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-310 | 3.4.2 FINVOICE data | [PD-310](https://ioteelab.atlassian.net/browse/PD-310) |
| PD-297 | 3.4.15 Billing event status information | [PD-297](https://ioteelab.atlassian.net/browse/PD-297) |

**Rationale:** `ExternalInvoicingClient` transmits the FINVOICE XML generated in Steps 35/36 to the external invoicing system (PD-310). On success: invoice → SENT; BillingEvent → SENT (PD-297 status transition). On failure: invoice → ERROR; retry strategy applied. `POST /invoice-runs/{id}/send` triggers batch transmission.

---

## Step 51 — Invoice Image Retrieval

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-303 | 3.4.9 Displaying invoice image | [PD-303](https://ioteelab.atlassian.net/browse/PD-303) |
| PD-306 | 3.4.6 Displaying invoice image | [PD-306](https://ioteelab.atlassian.net/browse/PD-306) |

**Rationale:** `InvoiceImageService` fetches the PDF binary from the external invoicing system on demand (not cached locally) and streams it back. `GET /api/v1/invoices/{id}/image` is PD-303. The AUTHORITY_VIEWER role can also access this endpoint (PD-306). HTTP 502/503 on retrieval failure.

---

## Step 52 — Billing Address Sync to External System

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-281 | 3.4.32 Updating billing information via API | [PD-281](https://ioteelab.atlassian.net/browse/PD-281) |

**Rationale:** `BillingProfileSyncService` is the event-driven sync that pushes address changes to the external invoicing system whenever a `BillingProfile` is saved. WasteHero is always master — save succeeds locally even if the sync fails; failures are logged and retried. This is the outbound integration side of PD-281.

---

## Step 53 — E-Invoice Operator Integration

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-107 | 3.11.39 E-invoice integration | [PD-107](https://ioteelab.atlassian.net/browse/PD-107) |

**Rationale:** `EInvoiceIntegrationService` is the daily batch job that fetches START and TERMINATE messages from the e-invoice operator (Ropo, Maventa, etc.) and updates customer delivery method and e-invoice addresses. The `manuallyLocked` flag (set in Step 07) prevents integration from overwriting manually set addresses. Unmatched messages are logged for manual review. This is the full implementation of PD-107.

---

## Step 54 — Invoice Recall

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-270 | 3.4.48 System behavior during billing run | [PD-270](https://ioteelab.atlassian.net/browse/PD-270) |

**Rationale:** `InvoiceRecallService` calls `ExternalInvoicingClient.recall()` to retract a transmitted invoice. On success: invoice status → CANCELLED. On failure (unsupported by external system): HTTP 422. BILLING_MANAGER role required. This covers the post-transmission cancellation scenario described in PD-270 and partially in Step 43.

---

## Build Area 13 — Authority Access

## Step 55 — Authority Invoice Access (Read-Only)

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-171 | 3.7.3 Right of authorities to view invoices | [PD-171](https://ioteelab.atlassian.net/browse/PD-171) |

**Rationale:** `AuthorityInvoiceViewController` provides `GET /authority/customers/{id}/invoices` restricted to AUTHORITY_VIEWER role. Only SENT and COMPLETED invoices are accessible — no DRAFT, READY, ERROR, or CANCELLED. No mutation endpoints exist in this scope. This is the direct implementation of PD-171.

---

## Step 56 — Authority Invoice Image Retrieval

**Release:** Invoicing 4

| PD Task | Summary | Jira Link |
|---------|---------|-----------|
| PD-171 | 3.7.3 Right of authorities to view invoices | [PD-171](https://ioteelab.atlassian.net/browse/PD-171) |

**Rationale:** `GET /api/v1/authority/invoices/{id}/image` is an authority-scoped extension of the image retrieval built in Step 51. It enforces the same status restriction as Step 55 — HTTP 403 if the invoice is not SENT or COMPLETED. Uses the same `InvoiceImageService` but from the authority security context. Completes PD-171.

---

## Full PD Task Reference

All 51 PD tasks across all four releases — final version.

### Invoicing 1 (v11193 — due 22 May 2026)

| PD Key | Summary | Used in Steps |
|--------|---------|---------------|
| [PD-163](https://ioteelab.atlassian.net/browse/PD-163) | 3.7.15 Billing list | _(deferred — placeholder only)_ |
| [PD-177](https://ioteelab.atlassian.net/browse/PD-177) | 3.6.34 Billing event data for reporting | Step 17 |
| [PD-228](https://ioteelab.atlassian.net/browse/PD-228) | 3.5.42 Driver and vehicle login to event | Step 17 |
| [PD-277](https://ioteelab.atlassian.net/browse/PD-277) | 3.4.36 Manual editing of events | Steps 13, 18 |
| [PD-283](https://ioteelab.atlassian.net/browse/PD-283) | 3.4.30 Manual creation of billing events | Step 12 |
| [PD-289](https://ioteelab.atlassian.net/browse/PD-289) | 3.4.23 Accounts receivable data | Steps 06, 08 |
| [PD-297](https://ioteelab.atlassian.net/browse/PD-297) | 3.4.15 Billing event status information | Steps 10, 11, 50 |
| [PD-298](https://ioteelab.atlassian.net/browse/PD-298) | 3.4.14 Billing data details | Steps 10, 29 |
| [PD-299](https://ioteelab.atlassian.net/browse/PD-299) | 3.4.13 Billing event details | Steps 10, 12, 15, 18 |
| [PD-307](https://ioteelab.atlassian.net/browse/PD-307) | 3.4.5 Invoice template selection | Steps 29, 35 |
| [PD-308](https://ioteelab.atlassian.net/browse/PD-308) | 3.4.4 Invoice data based on language selection | Steps 04, 29, 35 |
| [PD-309](https://ioteelab.atlassian.net/browse/PD-309) | 3.4.3 Invoice numbering sequence determination | Steps 01, 05, 35 |
| [PD-310](https://ioteelab.atlassian.net/browse/PD-310) | 3.4.2 FINVOICE data | Steps 01, 12, 20, 35, 36, 50 |
| [PD-318](https://ioteelab.atlassian.net/browse/PD-318) | 3.3.18 Editing billing events | Steps 11, 13, 15, 18 |

### Invoicing 2 (v11198 — due 19 Jun 2026)

| PD Key | Summary | Used in Steps |
|--------|---------|---------------|
| [PD-275](https://ioteelab.atlassian.net/browse/PD-275) | 3.4.38 Transfer and copy of billed events | Steps 16, 46 |
| [PD-276](https://ioteelab.atlassian.net/browse/PD-276) | 3.4.37 Transfer of unbilled events | Step 16 |
| [PD-279](https://ioteelab.atlassian.net/browse/PD-279) | 3.4.34 Dynamic updates to shared service events | Steps 32, 33 |
| [PD-280](https://ioteelab.atlassian.net/browse/PD-280) | 3.4.33 Shared service events on the invoice | Steps 32, 33 |
| [PD-286](https://ioteelab.atlassian.net/browse/PD-286) | 3.4.27 Minimum fee | Step 27 |
| [PD-287](https://ioteelab.atlassian.net/browse/PD-287) | 3.4.25 Projects | _(deferred — no step file produced)_ |
| [PD-288](https://ioteelab.atlassian.net/browse/PD-288) | 3.4.24 Seasonal fees | Step 28 |
| [PD-290](https://ioteelab.atlassian.net/browse/PD-290) | 3.4.22 Billing data generation, bundling | Steps 25, 30 |
| [PD-291](https://ioteelab.atlassian.net/browse/PD-291) | 3.4.21 Billing cycles | Steps 23, 41 |
| [PD-292](https://ioteelab.atlassian.net/browse/PD-292) | 3.4.20 Billing restrictions | Step 24 |
| [PD-293](https://ioteelab.atlassian.net/browse/PD-293) | 3.4.19 Invoice batch filtering | Steps 24, 38 |
| [PD-294](https://ioteelab.atlassian.net/browse/PD-294) | 3.4.18 Billing surcharge | Step 26 |
| [PD-319](https://ioteelab.atlassian.net/browse/PD-319) | 3.3.14 Price changes for unbilled events | Steps 47, 48 |
| [PD-344](https://ioteelab.atlassian.net/browse/PD-344) | 3.2.18 Transfer of billing events | Step 16 |
| [PD-364](https://ioteelab.atlassian.net/browse/PD-364) | 3.1.17 Retroactive changes to service responsibilities | Step 49 |

### Invoicing 3 (v11199 — due 28 Aug 2026)

| PD Key | Summary | Used in Steps |
|--------|---------|---------------|
| [PD-271](https://ioteelab.atlassian.net/browse/PD-271) | 3.4.42 Automatic checks on billing data | Steps 09, 14, 34, 42 |
| [PD-272](https://ioteelab.atlassian.net/browse/PD-272) | 3.4.41 Simulation run of billing data | Steps 09, 14, 34, 38, 40, 41 |
| [PD-273](https://ioteelab.atlassian.net/browse/PD-273) | 3.4.40 Cancellation option for billing data | Steps 38, 41, 43 |
| [PD-274](https://ioteelab.atlassian.net/browse/PD-274) | 3.4.39 Invoice simulation | Steps 09, 14, 34, 40 |
| [PD-278](https://ioteelab.atlassian.net/browse/PD-278) | 3.4.35 Error listing of events | Steps 09, 14, 34, 42 |
| [PD-282](https://ioteelab.atlassian.net/browse/PD-282) | 3.4.31 Editing e-invoice data | Step 07 |
| [PD-284](https://ioteelab.atlassian.net/browse/PD-284) | 3.4.29 Public and private law sales – billing | Steps 08, 31 |
| [PD-285](https://ioteelab.atlassian.net/browse/PD-285) | 3.4.28 Private and public law invoices | Steps 08, 21, 29, 31 |
| [PD-295](https://ioteelab.atlassian.net/browse/PD-295) | 3.4.17 Account and cost center data | Steps 02, 03, 19, 20, 21, 36 |
| [PD-296](https://ioteelab.atlassian.net/browse/PD-296) | 3.4.16 Cost centers and accounts | Steps 02, 03, 19, 20, 21 |
| [PD-300](https://ioteelab.atlassian.net/browse/PD-300) | 3.4.12 Reverse charge VAT | Steps 04, 22, 36 |
| [PD-301](https://ioteelab.atlassian.net/browse/PD-301) | 3.4.11 Gross or net invoicing | Steps 06, 22, 29 |
| [PD-302](https://ioteelab.atlassian.net/browse/PD-302) | 3.4.10 Custom and bulk invoice texts | Step 29 |

### Invoicing 4 (v11200 — due 25 Sep 2026)

| PD Key | Summary | Used in Steps |
|--------|---------|---------------|
| [PD-107](https://ioteelab.atlassian.net/browse/PD-107) | 3.11.39 E-invoice integration | Steps 07 _(lock flag)_, 53 _(full integration)_ |
| [PD-171](https://ioteelab.atlassian.net/browse/PD-171) | 3.7.3 Right of authorities to view invoices | Steps 55, 56 |
| [PD-269](https://ioteelab.atlassian.net/browse/PD-269) | 3.4.49 Credit invoices | Steps 44, 45, 46 |
| [PD-270](https://ioteelab.atlassian.net/browse/PD-270) | 3.4.48 System behavior during billing run | Steps 38, 39, 43, 54 |
| [PD-281](https://ioteelab.atlassian.net/browse/PD-281) | 3.4.32 Updating billing information via API | Steps 06, 07, 39, 52 |
| [PD-303](https://ioteelab.atlassian.net/browse/PD-303) | 3.4.9 Displaying invoice image | Step 51 |
| [PD-304](https://ioteelab.atlassian.net/browse/PD-304) | 3.4.8 PDF attachment to invoice batch | Step 37 |
| [PD-305](https://ioteelab.atlassian.net/browse/PD-305) | 3.4.7 PDF attachment to invoice | Step 37 |
| [PD-306](https://ioteelab.atlassian.net/browse/PD-306) | 3.4.6 Displaying invoice image | Steps 51, 56 |

---

## Coverage Notes

**All 56 step files mapped.** Every PD task across all four releases has at least one step assigned.

**PD-163 (Billing list):** Deferred by product decision — PJH has no current requirement. No step file was produced. A placeholder endpoint is noted in the master index.

**PD-287 (Projects):** Invoicing 2 story with no corresponding step file in the 56-step plan. Treat as deferred scope for now.

**Steps sharing multiple PD tasks:** Stories were written at feature level, not implementation level. PD-295 (Account and cost center data) spans Steps 02, 03, 19, 20, 21, and 36 because the story covers the full ledger allocation feature from entity definition through to FINVOICE output. PD-310 (FINVOICE data) appears across Steps 01, 12, 20, 35, 36, and 50 because it covers the entire FINVOICE lifecycle from data model through transmission.

**Steps spanning multiple PD tasks across releases:** Several steps touch tasks in different releases (e.g. Step 46 touches Invoicing 2 PD-275 and Invoicing 4 PD-269). The release listed on the step header reflects the earliest release the step's core entity/service is required; the step may also deliver features for later releases.
