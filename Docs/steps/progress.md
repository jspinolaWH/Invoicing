# Build Progress Tracker

---

## Build Area 1 — Master Data Foundation
- [x] STEP-01 — Foundation setup + VAT Rates
- [x] STEP-02 — Accounting Accounts
- [x] STEP-03 — Cost Centers
- [x] STEP-04 — Products + Translations
- [x] STEP-05 — Invoice Number Series

---

## Build Area 2 — Customer Billing Profile
- [x] STEP-06 — BillingProfile entity + delivery method + language
- [x] STEP-07 — EInvoiceAddress + manual lock flag
- [x] STEP-08 — ClassificationRule entity + LegalClassificationService
- [x] STEP-09 — BillingProfileValidationService + missing data checks

---

## Build Area 3 — BillingEvent Core
- [x] STEP-10 — BillingEvent entity + all fields + schema
- [x] STEP-11 — Status state machine (IN_PROGRESS → SENT → COMPLETED/ERROR)
- [x] STEP-12 — Create BillingEvent (external source + manual)
- [x] STEP-13 — Edit BillingEvent + BillingEventAuditLog
- [x] STEP-14 — BillingEventValidationService (pre-flight, missing fields)

---

## Build Area 4 — Exclusion, Transfer, Driver Events
- [x] STEP-15 — Exclude + reinstate + bulk exclude
- [x] STEP-16 — Transfer single + bulk (with audit log)
- [x] STEP-17 — Driver event submission + office review + approve/reject
- [x] STEP-18 — BillingEventAuditLog queries + audit trail endpoints

---

## Build Area 5 — Accounting Allocation
- [x] STEP-19 — AccountingAllocationRule entity + specificity ordering
- [x] STEP-20 — AccountingAllocationService (product + region matching)
- [x] STEP-21 — CostCenterCompositionService (dynamic composite assembly)
- [x] STEP-22 — VatCalculationService (event-date VAT + reverse charge)

---

## Build Area 6 — Billing Configuration
- [x] STEP-23 — BillingCycle entity + frequency + next billing date
- [x] STEP-24 — BillingRestriction entity
- [x] STEP-25 — BundlingRule entity (SINGLE_LINE / SEPARATE)
- [x] STEP-26 — SurchargeConfig + BillingSurchargeService
- [x] STEP-27 — MinimumFeeConfig + MinimumFeeService
- [x] STEP-28 — SeasonalFeeConfig + nightly scheduler

---

## Build Area 7 — Invoice Generation Core
- [x] STEP-29 — Invoice entity + InvoiceLineItem entity + status lifecycle
- [x] STEP-30 — InvoiceBundlingService (events → line items per BundlingRule)
- [x] STEP-31 — LegalClassificationService (PUBLIC_LAW / PRIVATE_LAW)
- [x] STEP-32 — PropertyGroup + SharedServiceParticipant + 100% rule
- [x] STEP-33 — SharedServiceInvoicingService + retroactive redistribution
- [x] STEP-34 — InvoiceGenerationService (full orchestration pipeline)

---

## Build Area 8 — FINVOICE Builder
- [x] STEP-35 — FinvoiceBuilderService core (seller/buyer, rows, VAT, language)
- [x] STEP-36 — FINVOICE advanced (multi-account splits, mixed VAT, reverse charge)
- [x] STEP-37 — Attachment handling (≤10 files, ≤1MB, base64, SHA1, PDF/A)

---

## Build Area 9 — Invoice Run + Simulation
- [x] STEP-38 — InvoiceRun entity + filter criteria + validation report
- [x] STEP-39 — ActiveRunLock + InvoiceRunLockService (423 Locked)
- [x] STEP-40 — InvoiceSimulationService (full pipeline, no side effects)
- [x] STEP-41 — InvoiceRunService (batch orchestration)
- [x] STEP-42 — InvoiceValidationEngine (MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, CLASSIFICATION)
- [x] STEP-43 — InvoiceCancellationService + scheduled send

---

## Build Area 10 — Credit Notes
- [x] STEP-44 — CreditNoteService (full + partial credit)
- [x] STEP-45 — Batch credit notes + credit FINVOICE
- [x] STEP-46 — BilledEventCorrectionService (credit + copy to new invoice)

---

## Build Area 11 — Retroactive Corrections
- [x] STEP-47 — RetroactivePriceAdjustmentService — preview phase
- [x] STEP-48 — RetroactivePriceAdjustmentService — apply phase + audit
- [x] STEP-49 — ServiceResponsibilityChangeService (preview + apply)

---

## Build Area 12 — Integration Layer
- [x] STEP-50 — ExternalInvoicingClient: FINVOICE transmission + status update
- [x] STEP-51 — Invoice image + attachment retrieval from external system
- [x] STEP-52 — Billing address sync (WasteHero as master)
- [x] STEP-53 — E-invoice operator integration (daily batch START/TERMINATE)
- [x] STEP-54 — Invoice recall

---

## Build Area 13 — Authority Access
- [x] STEP-55 — AuthorityInvoiceViewController (read-only, AUTHORITY_VIEWER role)
- [x] STEP-56 — Authority invoice image retrieval

---

## Summary
- Total steps: 56
- Completed: 56 / 56
