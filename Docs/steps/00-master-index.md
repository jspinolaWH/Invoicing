# Invoicing System — Full Build Step Index

> Total estimated steps: ~60
> Each step = small, independently testable, BE + FE included.
> Steps must be completed in order within each build area.
> Parallel tracks marked with *.

---

## Build Area 1 — Master Data Foundation
> Docs ref: `07-build-order.md` Step 1, `01-domain-model.md`, `02-data-layer.md`, `04-api-layer.md`

- [Step 01](step-01-foundation-vat-rates.md) — Foundation setup + VAT Rates
- [Step 02](step-02-accounting-accounts.md) — Accounting Accounts
- [Step 03](step-03-cost-centers.md) — Cost Centers
- [Step 04](step-04-products.md) — Products + Translations
- [Step 05](step-05-invoice-number-series.md) — Invoice Number Series (pessimistic lock)

---

## Build Area 2 — Customer Billing Profile
> Docs ref: `07-build-order.md` Step 2, `01-domain-model.md` (Customer/BillingProfile, ClassificationRule), `03-business-logic.md` (Area 1)

- [Step 06](step-06-customer-billing-profile.md) — BillingProfile entity + delivery method + language
- [Step 07](step-07-einvoice-address.md) — EInvoiceAddress + manual lock flag
- [Step 08](step-08-classification-rules.md) — ClassificationRule entity + LegalClassificationService
- [Step 09](step-09-billing-profile-validation.md) — BillingProfileValidationService + missing data checks

---

## Build Area 3 — BillingEvent Core
> Docs ref: `07-build-order.md` Step 3, `01-domain-model.md` (BillingEvent), `02-data-layer.md` (BillingEventRepository), `03-business-logic.md` (Area 1: BillingEventService, BillingEventStatusService)

- [Step 10](step-10-billing-event-entity.md) — BillingEvent entity + all fields + schema
- [Step 11](step-11-billing-event-status-machine.md) — Status state machine (IN_PROGRESS → SENT → COMPLETED/ERROR)
- [Step 12](step-12-billing-event-create.md) — Create BillingEvent (external source + manual)
- [Step 13](step-13-billing-event-edit-audit.md) — Edit BillingEvent + BillingEventAuditLog (append-only)
- [Step 14](step-14-billing-event-validation.md) — BillingEventValidationService (pre-flight, missing fields)

---

## Build Area 4 — Exclusion, Transfer, Driver Events
> Docs ref: `07-build-order.md` Step 4, `03-business-logic.md` (DriverEventService, BillingEventTransferService), `04-api-layer.md` (Driver Events section)

- [Step 15](step-15-billing-event-exclusion.md) — Exclude + reinstate + bulk exclude
- [Step 16](step-16-billing-event-transfer.md) — Transfer single + bulk (with audit log)
- [Step 17](step-17-driver-events.md) — Driver event submission + office review flag + approve/reject
- [Step 18](step-18-audit-logs.md) — BillingEventAuditLog entity + full audit trail queries

---

## Build Area 5 — Accounting Allocation
> Docs ref: `07-build-order.md` Step 5, `03-business-logic.md` (Area 6: AccountingAllocationService, CostCenterCompositionService), `02-data-layer.md` (AccountingAllocationRuleRepository)

- [Step 19](step-19-allocation-rules.md) — AccountingAllocationRule entity + specificity ordering
- [Step 20](step-20-accounting-allocation-service.md) — AccountingAllocationService (product + region matching)
- [Step 21](step-21-cost-center-composition.md) — CostCenterCompositionService (dynamic composite assembly)
- [Step 22](step-22-vat-calculation.md) — VatCalculationService (event-date VAT + reverse charge)

---

## Build Area 6 — Billing Configuration
> Docs ref: `07-build-order.md` Step 6, `01-domain-model.md` (BillingCycle, BundlingRule, SurchargeConfig, MinimumFeeConfig, SeasonalFeeConfig)

- [Step 23](step-23-billing-cycles.md) — BillingCycle entity + frequency enum + next billing date
- [Step 24](step-24-billing-restrictions.md) — BillingRestriction entity
- [Step 25](step-25-bundling-rules.md) — BundlingRule entity (SINGLE_LINE / SEPARATE per product group)
- [Step 26](step-26-surcharge-config.md) — SurchargeConfig (delivery method based) + BillingSurchargeService
- [Step 27](step-27-minimum-fee.md) — MinimumFeeConfig + MinimumFeeService (threshold + adjustment line)
- [Step 28](step-28-seasonal-fees.md) — SeasonalFeeConfig + nightly scheduler (SeasonalFeeGenerationService)

---

## Build Area 7 — Invoice Generation Core
> Docs ref: `07-build-order.md` Step 7, `01-domain-model.md` (Invoice, InvoiceLineItem, PropertyGroup, SharedServiceParticipant), `03-business-logic.md` (Area 2)

- [Step 29](step-29-invoice-entity.md) — Invoice entity + InvoiceLineItem entity + status lifecycle
- [Step 30](step-30-invoice-bundling.md) — InvoiceBundlingService (events → line items per BundlingRule)
- [Step 31](step-31-legal-classification.md) — LegalClassificationService (PUBLIC_LAW / PRIVATE_LAW rule evaluation)
- [Step 32](step-32-shared-services.md) — PropertyGroup + SharedServiceParticipant + 100% validation rule
- [Step 33](step-33-shared-service-invoicing.md) — SharedServiceInvoicingService (per-subscriber lines + retroactive redistribution)
- [Step 34](step-34-invoice-generation-orchestrator.md) — InvoiceGenerationService (full orchestration pipeline)

---

## Build Area 8 — FINVOICE Builder
> Docs ref: `07-build-order.md` Step 8, `05-integration-layer.md` (FINVOICE 3.0 Standard), `03-business-logic.md`

- [Step 35](step-35-finvoice-core.md) — FinvoiceBuilderService: seller/buyer, rows, VAT breakdown, language
- [Step 36](step-36-finvoice-advanced.md) — Multi-account splits + mixed VAT + reverse charge in FINVOICE
- [Step 37](step-37-finvoice-attachments.md) — Attachment handling (≤10 files, ≤1MB, base64, SHA1, PDF/A)

---

## Build Area 9 — Invoice Run + Simulation
> Docs ref: `07-build-order.md` Step 9, `01-domain-model.md` (InvoiceRun), `03-business-logic.md` (Area 3: InvoiceRunService, InvoiceRunLockService, InvoiceSimulationService), `06-cross-cutting.md` (Simulation Mode Guard, Data Locking)

- [Step 38](step-38-invoice-run-entity.md) — InvoiceRun entity + filter criteria + validation report
- [Step 39](step-39-invoice-run-lock.md) — ActiveRunLock + InvoiceRunLockService (423 Locked response)
- [Step 40](step-40-invoice-simulation.md) — InvoiceSimulationService (full pipeline, no side effects, SimulationReport)
- [Step 41](step-41-invoice-run-orchestration.md) — InvoiceRunService (batch orchestration, grouping by cycle/restriction)
- [Step 42](step-42-invoice-validation-engine.md) — InvoiceValidationEngine (MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, CLASSIFICATION rules)
- [Step 43](step-43-invoice-cancellation.md) — InvoiceCancellationService + scheduled send

---

## Build Area 10 — Credit Notes
> Docs ref: `07-build-order.md` Step 10, `03-business-logic.md` (Area 5: CreditNoteService, BilledEventCorrectionService)

- [Step 44](step-44-credit-notes.md) — CreditNoteService (full + partial credit, custom text + mandatory internal comment)
- [Step 45](step-45-batch-credit.md) — Batch credit notes + credit FINVOICE generation
- [Step 46](step-46-billed-event-correction.md) — BilledEventCorrectionService (credit original + copy to new invoice)

---

## Build Area 11 — Retroactive Corrections
> Docs ref: `07-build-order.md` Step 11, `03-business-logic.md` (Area 1: RetroactivePriceAdjustmentService, ServiceResponsibilityChangeService), `06-cross-cutting.md` (Preview-Then-Apply Pattern)

- [Step 47](step-47-price-adjustment-preview.md) — RetroactivePriceAdjustmentService — preview phase (no changes, returns affected entities)
- [Step 48](step-48-price-adjustment-apply.md) — RetroactivePriceAdjustmentService — apply phase (single transaction + PriceAdjustmentRun audit)
- [Step 49](step-49-service-responsibility-change.md) — ServiceResponsibilityChangeService (preview + apply + ServiceResponsibilityChangeLog)

---

## Build Area 12 — Integration Layer
> Docs ref: `07-build-order.md` Step 12, `05-integration-layer.md` (all 3 external system relationships)

- [Step 50](step-50-finvoice-transmission.md) — ExternalInvoicingClient: FINVOICE transmission + SENT→COMPLETED/ERROR handling
- [Step 51](step-51-invoice-image-retrieval.md) — Invoice image + attachment retrieval from external system
- [Step 52](step-52-billing-address-sync.md) — Billing address sync (WasteHero as master, retry on failure)
- [Step 53](step-53-einvoice-operator.md) — E-invoice operator integration (daily batch START/TERMINATE, manual lock respect)
- [Step 54](step-54-invoice-recall.md) — Invoice recall (if supported by external system)

---

## Build Area 13 — Authority Access
> Docs ref: `07-build-order.md` Step 13, `04-api-layer.md` (Authority Access section), `06-cross-cutting.md` (AUTHORITY_VIEWER role)

- [Step 55](step-55-authority-access.md) — AuthorityInvoiceViewController (read-only, AUTHORITY_VIEWER role, sent/completed only)
- [Step 56](step-56-authority-image-retrieval.md) — Authority invoice image retrieval endpoint

---

## Deferred / Out of Scope (MVP)
> Source: `07-build-order.md` — "Deferred (not MVP)"

- PD-163: Authority approval workflow (needs clarification from product)
- Weighbridge integration
- POS integration

---

## Key Cross-Cutting Rules (apply to ALL steps)
> Source: `06-cross-cutting.md`

1. **Audit trail** — every mutation logged (user, timestamp, field, old/new value, reason)
2. **Simulation mode guard** — no number assignment, no status changes, no external calls, no audit logging in simulation
3. **Immutability after SENT** — no edits, exclusion, or transfer once BillingEvent is SENT
4. **Data locking during runs** — 423 Locked response when customer is in active run
5. **VAT by event date** — never use today's date for VAT resolution
6. **Shared service 100% rule** — BigDecimal, ROUND_HALF_UP, scale 2, must equal exactly 100.00%
7. **WasteHero is master** — billing data in WasteHero always wins over external system
8. **Preview-then-apply** — two-phase pattern for all retroactive operations
