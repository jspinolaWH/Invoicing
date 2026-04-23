# Ralph Brother Summary

**Completed:** 2026-04-22T18:00:00Z
**Project:** /mnt/c/Users/drasm/Desktop/Invoicing

## Results

| Status | Count |
|--------|-------|
| ✅ Fully done (UI_MAPPED + passes) | 52 |
| ⚠️ Still PARTIAL_UI | 0 |
| ❌ Still NO_UI | 0 |
| 🔍 Needs review | 0 |

## Fixed This Run

All 34 requirements in the work queue were processed:

**Code gaps fixed (backend + frontend wiring):**
- PD-307 — Invoice template selection: InvoiceRunResponse now exposes templateId/numberSeriesId; InvoiceRunDetailPage shows template card
- PD-280 — Shared service events: V28 migration created property_groups + shared_service_participants tables
- PD-107 — E-invoice integration: direct debit mandate UI, create-if-absent path, operator page improvements

**UI gaps fixed (frontend surfaces added):**
- PD-364 — Retroactive service responsibility changes: audit trail, export, filter help text
- PD-363 — Euro limit alerts: ticket conversion workflow, cross-linked audit trail
- PD-344 — Transfer of billing events: origin filter dropdown + backend support
- PD-310 — FINVOICE data: PASSES (no fix needed)
- PD-308 — Invoice language selection: language code constrained dropdown, unit labels reference panel
- PD-306 — Displaying invoice image: role gate on "View Invoice Image" button
- PD-305 — PDF attachment to invoice: compliance gate banner, SHA1 badge on attachments
- PD-304 — PDF attachment to invoice batch: AttachmentDetails panels, validation, audit trail, verify button
- PD-303 — Displaying invoice image (attachments): in-page error banners replace alert() popups
- PD-302 — Custom and bulk invoice texts: audit trail entries for custom text saves, template info panel
- PD-301 — Gross or net invoicing: CompanyInvoicingDefaults entity + page, role-gated Invoicing Mode dropdown
- PD-299 — Billing event details: FINVOICE data section, parent invoice link, Weighbridge Config nav link
- PD-298 — Billing data details: FINVOICE compliance panel, mandatory fields matrix, FINVOICE baseline table
- PD-295 — Account and cost center data: support guidance banner, Cash Register Integration page
- PD-293 — Invoice batch filtering: RunFilterCriteriaConfigPage with localStorage-backed toggles
- PD-292 — Billing restrictions: conflict-prevention banner, ERP integration status page, billing type badge
- PD-291 — Billing cycles: simulation cycle grouping table, external system integration status card, billing cycle column
- PD-290 — Billing data generation, bundling: bundling rule change history section
- PD-287 — Projects: linkedPropertyId on Project entity + property search in Projects page modal
- PD-286 — Minimum fee: minimum fee exemption + top-up counts on simulation and run detail pages
- PD-285 — Private and public law invoices: enforcement path indicator on billing event + invoice line items
- PD-284 — Public and private law sales: combine/separate toggle, auto-split toggle, classification reason, FINVOICE classification row, enforcement routing codes
- PD-282 — Editing e-invoice data: e-invoice address source label with last-modified-by
- PD-281 — Updating billing information via API: sync result counts, address change history, SENT invoice warning
- PD-276 — Transfer of unbilled events: fee component checkboxes in TransferEventModal and BulkTransferModal
- PD-274 — Invoice simulation: simulation phase audit trail surfaced in SimulationResultsPage and InvoiceRunDetailPage
- PD-270 — System behavior during billing run: billing address lock indicators, global active-run banner
- PD-269 — Credit invoices: credit note numbering policy info banner and pre-submit note
- PD-177 — Billing event data for reporting: Reporting Fields nav link added to sidebar
- PD-163 — Billing list: PASSES (no fix needed)
- PD-427 — Invoicing REST API: PASSES (no fix needed)

## Still Needs Work

None — all requirements fully resolved.
