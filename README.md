# Build Progress Tracker

---

## Build Area 1 — Master Data Foundation
- [x] STEP-01 — Foundation setup + VAT Rates
- [x] STEP-02 — Accounting Accounts
- [x] STEP-03 — Cost Centers
- [ ] STEP-04 — Products + Translations
- [ ] STEP-05 — Invoice Number Series

---

## Build Area 2 — Customer Billing Profile
- [ ] STEP-06 — BillingProfile entity + delivery method + language
- [ ] STEP-07 — EInvoiceAddress + manual lock flag
- [ ] STEP-08 — ClassificationRule entity + LegalClassificationService
- [ ] STEP-09 — BillingProfileValidationService + missing data checks

---

## Build Area 3 — BillingEvent Core
- [ ] STEP-10 — BillingEvent entity + all fields + schema
- [ ] STEP-11 — Status state machine (IN_PROGRESS → SENT → COMPLETED/ERROR)
- [ ] STEP-12 — Create BillingEvent (external source + manual)
- [ ] STEP-13 — Edit BillingEvent + BillingEventAuditLog
- [ ] STEP-14 — BillingEventValidationService (pre-flight, missing fields)

---

## Build Area 4 — Exclusion, Transfer, Driver Events
- [ ] STEP-15 — Exclude + reinstate + bulk exclude
- [ ] STEP-16 — Transfer single + bulk (with audit log)
- [ ] STEP-17 — Driver event submission + office review + approve/reject
- [ ] STEP-18 — BillingEventAuditLog queries + audit trail endpoints

---

## Build Area 5 — Accounting Allocation
- [ ] STEP-19 — AccountingAllocationRule entity + specificity ordering
- [ ] STEP-20 — AccountingAllocationService (product + region matching)
- [ ] STEP-21 — CostCenterCompositionService (dynamic composite assembly)
- [ ] STEP-22 — VatCalculationService (event-date VAT + reverse charge)

---

## Build Area 6 — Billing Configuration
- [ ] STEP-23 — BillingCycle entity + frequency + next billing date
- [ ] STEP-24 — BillingRestriction entity
- [ ] STEP-25 — BundlingRule entity (SINGLE_LINE / SEPARATE)
- [ ] STEP-26 — SurchargeConfig + BillingSurchargeService
- [ ] STEP-27 — MinimumFeeConfig + MinimumFeeService
- [ ] STEP-28 — SeasonalFeeConfig + nightly scheduler

---

## Build Area 7 — Invoice Generation Core
- [ ] STEP-29 — Invoice entity + InvoiceLineItem entity + status lifecycle
- [ ] STEP-30 — InvoiceBundlingService (events → line items per BundlingRule)
- [ ] STEP-31 — LegalClassificationService (PUBLIC_LAW / PRIVATE_LAW)
- [ ] STEP-32 — PropertyGroup + SharedServiceParticipant + 100% rule
- [ ] STEP-33 — SharedServiceInvoicingService + retroactive redistribution
- [ ] STEP-34 — InvoiceGenerationService (full orchestration pipeline)

---

## Build Area 8 — FINVOICE Builder
- [ ] STEP-35 — FinvoiceBuilderService core (seller/buyer, rows, VAT, language)
- [ ] STEP-36 — FINVOICE advanced (multi-account splits, mixed VAT, reverse charge)
- [ ] STEP-37 — Attachment handling (≤10 files, ≤1MB, base64, SHA1, PDF/A)

---

## Build Area 9 — Invoice Run + Simulation
- [ ] STEP-38 — InvoiceRun entity + filter criteria + validation report
- [ ] STEP-39 — ActiveRunLock + InvoiceRunLockService (423 Locked)
- [ ] STEP-40 — InvoiceSimulationService (full pipeline, no side effects)
- [ ] STEP-41 — InvoiceRunService (batch orchestration)
- [ ] STEP-42 — InvoiceValidationEngine (MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, CLASSIFICATION)
- [ ] STEP-43 — InvoiceCancellationService + scheduled send

---

## Build Area 10 — Credit Notes
- [ ] STEP-44 — CreditNoteService (full + partial credit)
- [ ] STEP-45 — Batch credit notes + credit FINVOICE
- [ ] STEP-46 — BilledEventCorrectionService (credit + copy to new invoice)

---

## Build Area 11 — Retroactive Corrections
- [ ] STEP-47 — RetroactivePriceAdjustmentService — preview phase
- [ ] STEP-48 — RetroactivePriceAdjustmentService — apply phase + audit
- [ ] STEP-49 — ServiceResponsibilityChangeService (preview + apply)

---

## Build Area 12 — Integration Layer
- [ ] STEP-50 — ExternalInvoicingClient: FINVOICE transmission + status update
- [ ] STEP-51 — Invoice image + attachment retrieval from external system
- [ ] STEP-52 — Billing address sync (WasteHero as master)
- [ ] STEP-53 — E-invoice operator integration (daily batch START/TERMINATE)
- [ ] STEP-54 — Invoice recall

---

## Build Area 13 — Authority Access
- [ ] STEP-55 — AuthorityInvoiceViewController (read-only, AUTHORITY_VIEWER role)
- [ ] STEP-56 — Authority invoice image retrieval

---

## Summary
- Total steps: 56
- Completed: 2 / 56
