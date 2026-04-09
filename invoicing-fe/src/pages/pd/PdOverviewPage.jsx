import { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import './PdOverviewPage.css'

const STEP_PAGES = {
  1:  [{ label: 'VAT Rates', path: '/master-data/vat-rates' }],
  2:  [{ label: 'Accounting Accounts', path: '/master-data/accounting-accounts' }],
  3:  [{ label: 'Cost Centers', path: '/master-data/cost-centers' }],
  4:  [{ label: 'Products', path: '/master-data/products' }],
  5:  [{ label: 'Invoice Number Series', path: '/master-data/invoice-number-series' }],
  6:  [{ label: 'Billing Profile', path: '/customers' }],
  7:  [{ label: 'Billing Profile', path: '/customers' }],
  8:  [{ label: 'Classification Rules', path: '/config/classification-rules' }],
  9:  [{ label: 'Validation Rules', path: '/config/validation-rules' }, { label: 'Billing Profile', path: '/customers' }],
  10: [{ label: 'Billing Events', path: '/billing-events' }],
  11: [{ label: 'Billing Events', path: '/billing-events' }],
  12: [{ label: 'Create Billing Event', path: '/billing-events/new' }],
  13: [{ label: 'Edit Billing Event', path: '/billing-events' }],
  14: [{ label: 'Billing Events', path: '/billing-events' }],
  15: [{ label: 'Billing Events', path: '/billing-events' }],
  16: [{ label: 'Billing Events', path: '/billing-events' }],
  17: [{ label: 'Review Queue', path: '/billing-events/review-queue' }],
  18: [{ label: 'Billing Events', path: '/billing-events' }],
  19: [{ label: 'Allocation Rules', path: '/master-data/allocation-rules' }],
  20: [{ label: 'Allocation Rules', path: '/master-data/allocation-rules' }],
  21: [{ label: 'Cost Centers', path: '/master-data/cost-centers' }],
  22: [{ label: 'VAT Rates', path: '/master-data/vat-rates' }],
  23: [{ label: 'Billing Cycles', path: '/billing/cycles' }],
  24: [{ label: 'Billing Restrictions', path: '/billing/restrictions' }],
  25: [{ label: 'Bundling Rules', path: '/customers' }],
  26: [{ label: 'Surcharge Config', path: '/master-data/surcharge-config' }],
  27: [{ label: 'Minimum Fee Config', path: '/master-data/minimum-fee-config' }],
  28: [{ label: 'Seasonal Fees', path: '/billing/seasonal-fees' }],
  29: [{ label: 'Invoice List', path: '/invoices' }],
  30: [{ label: 'Invoice List', path: '/invoices' }],
  31: [{ label: 'Classification Rules', path: '/config/classification-rules' }],
  32: [{ label: 'Property Groups', path: '/shared-services/property-groups' }],
  33: [{ label: 'Property Groups', path: '/shared-services/property-groups' }],
  34: [{ label: 'Generate Invoice', path: '/invoices/generate' }],
  35: [{ label: 'Invoice Detail', path: '/invoices' }],
  36: [{ label: 'Invoice Detail', path: '/invoices' }],
  37: [{ label: 'Invoice Detail', path: '/invoices' }],
  38: [{ label: 'Invoice Runs', path: '/runs/new' }],
  39: [{ label: 'Invoice Runs', path: '/runs/new' }],
  40: [{ label: 'Simulation Results', path: '/runs/simulation-results' }],
  41: [{ label: 'Run Detail', path: '/runs/new' }],
  42: [{ label: 'Validation Rules', path: '/config/validation-rules' }],
  43: [{ label: 'Invoice Detail', path: '/invoices' }],
  44: [{ label: 'Credit Note', path: '/invoices' }],
  45: [{ label: 'Credit Note', path: '/invoices' }],
  46: [{ label: 'Correct Invoice', path: '/invoices' }],
  47: [{ label: 'Price Adjustment', path: '/retroactive/price-adjustment' }],
  48: [{ label: 'Price Adjustment', path: '/retroactive/price-adjustment' }],
  49: [{ label: 'Responsibility Change', path: '/retroactive/responsibility-change' }],
  50: [{ label: 'Invoice Detail', path: '/invoices' }],
  51: [{ label: 'Invoice Detail', path: '/invoices' }],
  52: [{ label: 'Billing Addr. Sync', path: '/integration/billing-sync' }],
  53: [{ label: 'E-Invoice Operator', path: '/integration/operator' }],
  54: [{ label: 'Invoice Detail', path: '/invoices' }],
  55: [{ label: 'Authority Invoice View', path: '/authority/invoices' }],
  56: [{ label: 'Authority Invoice View', path: '/authority/invoices' }],
}

const RELEASES = [
  { key: 'Invoicing 1', label: 'Invoicing 1', due: '22 May 2026' },
  { key: 'Invoicing 2', label: 'Invoicing 2', due: '19 Jun 2026' },
  { key: 'Invoicing 3', label: 'Invoicing 3', due: '28 Aug 2026' },
  { key: 'Invoicing 4', label: 'Invoicing 4', due: '25 Sep 2026' },
]

const RAW_REQUIREMENTS = [
  // ── Invoicing 1 ──────────────────────────────────────────────────────────
  {
    id: 'PD-163', summary: '3.7.15 Billing list', release: 'Invoicing 1', steps: [], status: 'deferred',
    location: 'Deferred — no UI screen planned for this release.',
  },
  {
    id: 'PD-177', summary: '3.6.34 Billing event data for reporting', release: 'Invoicing 1', steps: [17], status: 'covered',
    location: 'Operations → Review Queue: driver submission rows include all billing event data fields used for reporting.',
  },
  {
    id: 'PD-228', summary: '3.5.42 Driver and vehicle login to event', release: 'Invoicing 1', steps: [17], status: 'covered',
    location: 'Operations → Review Queue: each driver event shows the submitting driver and vehicle. Office staff approve or reject from this table.',
  },
  {
    id: 'PD-277', summary: '3.4.36 Manual editing of events', release: 'Invoicing 1', steps: [13, 18], status: 'covered',
    location: 'Operations → Billing Events → click "Edit" on an IN_PROGRESS or ERROR event to open the edit form. All editable fields (fees, quantity, product, dates) are exposed here. The Audit Trail tab on the detail page logs every change.',
  },
  {
    id: 'PD-283', summary: '3.4.30 Manual creation of billing events', release: 'Invoicing 1', steps: [12], status: 'covered',
    location: 'Operations → Billing Events → "+ New Event" button opens the manual creation form. Fill in customer, product, fees, quantity, and event date.',
  },
  {
    id: 'PD-289', summary: '3.4.23 Accounts receivable data', release: 'Invoicing 1', steps: [6, 8], status: 'covered',
    location: 'Customers & Config → Billing Profiles → open a customer → billing address, payment terms, and accounts receivable fields. Also drives Classification Rules for legal entity type.',
  },
  {
    id: 'PD-297', summary: '3.4.15 Billing event status information', release: 'Invoicing 1', steps: [10, 11, 50], status: 'covered',
    location: 'Operations → Billing Events: "Status" column shows IN_PROGRESS / SENT / COMPLETED / ERROR with colour badges. Transitions happen on edit, validation, and invoice transmission. Invoices → Invoice Detail → Transmit panel drives the SENT transition.',
  },
  {
    id: 'PD-298', summary: '3.4.14 Billing data details', release: 'Invoicing 1', steps: [10, 29], status: 'covered',
    location: 'Operations → Billing Events: table columns show waste fee, transport fee, eco fee, and quantity. Full detail visible on the Billing Event Detail page. Also reflected as line items in Invoices → Invoice List → Invoice Detail.',
  },
  {
    id: 'PD-299', summary: '3.4.13 Billing event details', release: 'Invoicing 1', steps: [10, 12, 15, 18], status: 'covered',
    location: 'Operations → Billing Events: main table shows all event fields. Open any row for the full Billing Event Detail page. "+ New Event" exposes the creation form. "Exclude Selected" in the toolbar handles exclusion. Audit Trail tab shows the event history.',
  },
  {
    id: 'PD-307', summary: '3.4.5 Invoice template selection', release: 'Invoicing 1', steps: [29, 35], status: 'covered',
    location: 'Invoices → Invoice List → open an invoice: the invoice header shows the selected template. Template assignment is driven by the customer\'s billing profile and legal classification.',
  },
  {
    id: 'PD-308', summary: '3.4.4 Invoice data based on language selection', release: 'Invoicing 1', steps: [4, 29, 35], status: 'covered',
    location: 'Master Data → Products: each product has translatable name fields per language. Customers & Config → Billing Profiles → customer language field. Invoices → Invoice Detail: invoice header and line item descriptions reflect the customer\'s language.',
  },
  {
    id: 'PD-309', summary: '3.4.3 Invoice numbering sequence determination', release: 'Invoicing 1', steps: [1, 5, 35], status: 'covered',
    location: 'Master Data → Invoice Number Series: configure number format, prefix, and current sequence per invoice type. Invoices → Invoice Detail: the assigned invoice number is shown in the header and is derived from this series at generation time.',
  },
  {
    id: 'PD-310', summary: '3.4.2 FINVOICE data', release: 'Invoicing 1', steps: [1, 12, 20, 35, 36, 50], status: 'covered',
    location: 'Invoices → Invoice Detail → "Transmit" panel: triggers FINVOICE XML generation and sends to external system. Invoice line items, VAT rows, account splits, and seller/buyer data are all included. Master Data (VAT Rates, Allocation Rules) feeds the calculation. Operations → Create Billing Event feeds the source data.',
  },
  {
    id: 'PD-318', summary: '3.3.18 Editing billing events', release: 'Invoicing 1', steps: [11, 13, 15, 18], status: 'covered',
    location: 'Operations → Billing Events: "Edit" button (visible on IN_PROGRESS/ERROR rows) opens the edit form. Status transitions are shown in the Status column. "Exclude Selected" toolbar button bulk-excludes. Billing Event Detail → Audit Trail tab shows the full change history.',
  },

  // ── Invoicing 2 ──────────────────────────────────────────────────────────
  {
    id: 'PD-275', summary: '3.4.38 Transfer and copy of billed events', release: 'Invoicing 2', steps: [16, 46], status: 'covered',
    location: 'Operations → Billing Events → "Transfer Selected" toolbar: select billed events and transfer to another customer (copy preserved, original audited). Invoices → Invoice Detail → "Correct Invoice" button: issues a credit note on the original and copies events to a new IN_PROGRESS state for re-invoicing.',
  },
  {
    id: 'PD-276', summary: '3.4.37 Transfer of unbilled events', release: 'Invoicing 2', steps: [16], status: 'covered',
    location: 'Operations → Billing Events → "Transfer Selected" toolbar: select IN_PROGRESS events → enter target customer number and reason in the transfer modal. Only unbilled (non-SENT/COMPLETED) events are moved.',
  },
  {
    id: 'PD-279', summary: '3.4.34 Dynamic updates to shared service events', release: 'Invoicing 2', steps: [32, 33], status: 'covered',
    location: 'Customers & Config → Property Groups: open a property group → participant list shows current cost-sharing percentages. Retroactive redistribution is triggered when percentages change, automatically adjusting previously billed shared service events.',
  },
  {
    id: 'PD-280', summary: '3.4.33 Shared service events on the invoice', release: 'Invoicing 2', steps: [32, 33], status: 'covered',
    location: 'Customers & Config → Property Groups: configure which customers share a property group and their percentage splits. Invoices → Invoice Detail: shared service events appear as separate line items attributed to the property group.',
  },
  {
    id: 'PD-286', summary: '3.4.27 Minimum fee', release: 'Invoicing 2', steps: [27], status: 'covered',
    location: 'Master Data → Minimum Fee Config: table of minimum fee thresholds per customer type and billing period. Applied automatically during invoice generation — if the total falls below the threshold a minimum fee line is added.',
  },
  {
    id: 'PD-287', summary: '3.4.25 Projects', release: 'Invoicing 2', steps: [], status: 'deferred',
    location: 'Deferred — no UI screen planned for this release.',
  },
  {
    id: 'PD-288', summary: '3.4.24 Seasonal fees', release: 'Invoicing 2', steps: [28], status: 'covered',
    location: 'Operations → Seasonal Fees: table of seasonal rate rules (date range, product, fee override). A nightly scheduler applies active rules automatically to matching billing events.',
  },
  {
    id: 'PD-290', summary: '3.4.22 Billing data generation, bundling', release: 'Invoicing 2', steps: [25, 30], status: 'covered',
    location: 'Customers & Config → Billing Profiles → open a customer → Bundling Rules tab: set SINGLE_LINE (events merged into one line) or SEPARATE (one line per event). The result is visible in Invoices → Invoice Detail → line items.',
  },
  {
    id: 'PD-291', summary: '3.4.21 Billing cycles', release: 'Invoicing 2', steps: [23, 41], status: 'covered',
    location: 'Operations → Billing Cycles: configure frequency (monthly, quarterly, etc.) and next billing date per customer segment. Invoices → Invoice Run Detail: the run groups customers by billing cycle and shows which cycles were included.',
  },
  {
    id: 'PD-292', summary: '3.4.20 Billing restrictions', release: 'Invoicing 2', steps: [24], status: 'covered',
    location: 'Operations → Billing Restrictions: table of rules that block specific customers or products from being included in an invoice run. Restrictions are checked during run execution and excluded customers are listed in the run report.',
  },
  {
    id: 'PD-293', summary: '3.4.19 Invoice batch filtering', release: 'Invoicing 2', steps: [24, 38], status: 'covered',
    location: 'Operations → Billing Restrictions: restriction rules act as batch filters. Invoices → Invoice Runs: the run configuration form has explicit filter criteria (date range, customer segment, billing cycle) that control which events are included in the batch.',
  },
  {
    id: 'PD-294', summary: '3.4.18 Billing surcharge', release: 'Invoicing 2', steps: [26], status: 'covered',
    location: 'Master Data → Surcharge Config: table of surcharge rules per delivery method (e-invoice, paper, etc.). Surcharges are added automatically as a separate line item on invoices for matching customers.',
  },
  {
    id: 'PD-319', summary: '3.3.14 Price changes for unbilled events', release: 'Invoicing 2', steps: [47, 48], status: 'covered',
    location: 'Tools → Price Adjustment: (1) Preview phase — enter date range, product, and optional customer filter to see a diff of current vs new prices for all matching unbilled events. (2) Apply phase — confirm to commit all changes in one atomic transaction with a full audit log.',
  },
  {
    id: 'PD-344', summary: '3.2.18 Transfer of billing events', release: 'Invoicing 2', steps: [16], status: 'covered',
    location: 'Operations → Billing Events → "Transfer Selected" toolbar: select events → bulk transfer modal → enter target customer number and reason. Covers both manual operator transfers and API-triggered transfers.',
  },
  {
    id: 'PD-364', summary: '3.1.17 Retroactive changes to service responsibilities', release: 'Invoicing 2', steps: [49], status: 'covered',
    location: 'Tools → Responsibility Change: (1) Preview — enter date range, customer, and product to see which events would have their legal classification or cost centre changed. (2) Apply — commits changes and writes a full audit log per event.',
  },

  // ── Invoicing 3 ──────────────────────────────────────────────────────────
  {
    id: 'PD-271', summary: '3.4.42 Automatic checks on billing data', release: 'Invoicing 3', steps: [9, 14, 34, 42], status: 'covered',
    location: 'Customers & Config → Validation Rules: configure rule types (MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, CLASSIFICATION) with BLOCKING or WARNING severity. Operations → Billing Events → "Validate" toolbar button runs checks on selected events and opens a validation report modal. Invoices → Generate Invoice runs these checks before generation.',
  },
  {
    id: 'PD-272', summary: '3.4.41 Simulation run of billing data', release: 'Invoicing 3', steps: [9, 14, 34, 38, 40, 41], status: 'covered',
    location: 'Invoices → Generate Invoice: toggle "Simulate" mode — runs the full pipeline with no side effects (no invoice numbers assigned, no FINVOICE sent). Invoices → Invoice Runs → check "Simulation" before starting a run. Results appear in Invoices → Simulation Results with a per-customer breakdown.',
  },
  {
    id: 'PD-273', summary: '3.4.40 Cancellation option for billing data', release: 'Invoicing 3', steps: [38, 41, 43], status: 'covered',
    location: 'Invoices → Invoice Runs: a "Cancel Run" button is available while the run is in RUNNING state. Invoices → Invoice Run Detail: run status shows CANCELLED with a reason. Invoices → Invoice Detail → "Cancel Invoice" button cancels a pre-transmission invoice (reverts statuses and releases the invoice number).',
  },
  {
    id: 'PD-274', summary: '3.4.39 Invoice simulation', release: 'Invoicing 3', steps: [9, 14, 34, 40], status: 'covered',
    location: 'Invoices → Generate Invoice: "Preview" button shows a simulation result for a single customer without committing anything. Invoices → Simulation Results: batch simulation output — per-customer invoice preview with line items, VAT, and validation results.',
  },
  {
    id: 'PD-278', summary: '3.4.35 Error listing of events', release: 'Invoicing 3', steps: [9, 14, 34, 42], status: 'covered',
    location: 'Operations → Billing Events → "Validate" button opens a Validation Report modal listing all BLOCKING and WARNING errors per event with field-level detail. Invoices → Generate Invoice: a pre-flight error list is shown before generation proceeds. Errors reference the specific event ID and rule that fired.',
  },
  {
    id: 'PD-282', summary: '3.4.31 Editing e-invoice data', release: 'Invoicing 3', steps: [7], status: 'covered',
    location: 'Customers & Config → Billing Profiles → open a customer → "E-Invoice Address" section: edit operator ID, e-invoice address, and intermediary. The "Manually Locked" toggle prevents the daily operator sync from overwriting manual edits.',
  },
  {
    id: 'PD-284', summary: '3.4.29 Public and private law sales – billing', release: 'Invoicing 3', steps: [8, 31], status: 'covered',
    location: 'Customers & Config → Classification Rules: rules that assign PUBLIC_LAW or PRIVATE_LAW based on customer type, municipality, and product. The classification badge is visible on Billing Event rows and drives separate invoice generation paths.',
  },
  {
    id: 'PD-285', summary: '3.4.28 Private and public law invoices', release: 'Invoicing 3', steps: [8, 21, 29, 31], status: 'covered',
    location: 'Customers & Config → Classification Rules: defines which customers/products are public vs private law. Master Data → Cost Centers: cost centre split between public and private law. Invoices → Invoice List → Invoice Detail: the invoice header shows the legal classification and uses the correct template and VAT treatment.',
  },
  {
    id: 'PD-295', summary: '3.4.17 Account and cost center data', release: 'Invoicing 3', steps: [2, 3, 19, 20, 21, 36], status: 'covered',
    location: 'Master Data → Accounting Accounts: the chart of accounts. Master Data → Cost Centers: cost centre segments. Master Data → Allocation Rules: rules that map product + region to a specific account. Invoices → Invoice Detail → line items show the resolved account code per row.',
  },
  {
    id: 'PD-296', summary: '3.4.16 Cost centers and accounts', release: 'Invoicing 3', steps: [2, 3, 19, 20, 21], status: 'covered',
    location: 'Master Data → Cost Centers: cost centre table with segment codes. Master Data → Accounting Accounts: associated accounts. Master Data → Allocation Rules: the rules engine that links cost centres and accounts to billing events at invoice generation time.',
  },
  {
    id: 'PD-300', summary: '3.4.12 Reverse charge VAT', release: 'Invoicing 3', steps: [4, 22, 36], status: 'covered',
    location: 'Master Data → Products: "Reverse Charge" flag on each product. Invoices → Invoice Detail → VAT breakdown section: reverse charge events show 0% VAT with a reverse charge annotation. The FINVOICE XML includes the mandatory reverse charge tax code.',
  },
  {
    id: 'PD-301', summary: '3.4.11 Gross or net invoicing', release: 'Invoicing 3', steps: [6, 22, 29], status: 'covered',
    location: 'Customers & Config → Billing Profiles → open a customer → "Invoicing Method" field (Gross / Net). Invoices → Invoice Detail: amounts are displayed as gross or net according to this setting. VAT is either included in the line price or shown as a separate line.',
  },
  {
    id: 'PD-302', summary: '3.4.10 Custom and bulk invoice texts', release: 'Invoicing 3', steps: [29], status: 'covered',
    location: 'Invoices → Invoice Detail → Custom Text panel: free-text fields for invoice header text and footer text. These appear on the printed/FINVOICE output. Bulk text can be set via the invoice run configuration.',
  },

  // ── Invoicing 4 ──────────────────────────────────────────────────────────
  {
    id: 'PD-107', summary: '3.11.39 E-invoice integration', release: 'Invoicing 4', steps: [7, 53], status: 'covered',
    location: 'Customers & Config → Billing Profiles → E-Invoice Address section → "Manually Locked" toggle: lock an address to prevent daily sync from overwriting it. Tools → E-Invoice Operator: the daily batch table shows START and TERMINATE messages received from operators (Ropo, Maventa, etc.) — unmatched messages are flagged for manual review.',
  },
  {
    id: 'PD-171', summary: '3.7.3 Right of authorities to view invoices', release: 'Invoicing 4', steps: [55, 56], status: 'covered',
    location: 'Tools → Authority: Inv. View: read-only list of SENT and COMPLETED invoices accessible to AUTHORITY_VIEWER role only. DRAFT, READY, ERROR, and CANCELLED invoices are hidden. Each row has a "View Image" button that fetches the PDF from the external invoicing system.',
  },
  {
    id: 'PD-269', summary: '3.4.49 Credit invoices', release: 'Invoicing 4', steps: [44, 45, 46], status: 'covered',
    location: 'Invoices → Invoice Detail → "Issue Credit Note" button: opens the credit note form for full or partial credit (select specific line items). Invoices → Invoice List → bulk credit action for multiple invoices with a shared reason. Invoices → Invoice Detail → "Correct Invoice": credit + copy flow for re-invoicing corrected events.',
  },
  {
    id: 'PD-270', summary: '3.4.48 System behavior during billing run', release: 'Invoicing 4', steps: [38, 39, 43, 54], status: 'covered',
    location: 'Invoices → Invoice Runs: an active run locks all billing profile address edits (HTTP 423 — lock indicator shown on the billing profile page). Invoices → Invoice Run Detail: real-time status polling shows PENDING → RUNNING → COMPLETED/FAILED. Invoices → Invoice Detail → "Cancel" / "Recall" buttons for post-run corrections.',
  },
  {
    id: 'PD-281', summary: '3.4.32 Updating billing information via API', release: 'Invoicing 4', steps: [6, 7, 39, 52], status: 'covered',
    location: 'Customers & Config → Billing Profiles → customer detail: all address and billing info fields. Changes are automatically synced to the external invoicing system on save (WasteHero is always master). Tools → Billing Addr. Sync: shows sync status and allows manual re-trigger. Edits are blocked during an active invoice run.',
  },
  {
    id: 'PD-303', summary: '3.4.9 Displaying invoice image', release: 'Invoicing 4', steps: [51], status: 'covered',
    location: 'Invoices → Invoice Detail → "View Invoice Image" button: fetches the PDF from the external invoicing system on demand and opens it in the browser. Only available for SENT and COMPLETED invoices. Returns HTTP 502 if the external system is unavailable.',
  },
  {
    id: 'PD-304', summary: '3.4.8 PDF attachment to invoice batch', release: 'Invoicing 4', steps: [37], status: 'covered',
    location: 'Invoices → Invoice Detail → Attachments panel: upload up to 10 PDF/A files (max 1 MB each) to be attached to the invoice batch. Attachments are base64-encoded and SHA1-hashed in the FINVOICE XML.',
  },
  {
    id: 'PD-305', summary: '3.4.7 PDF attachment to invoice', release: 'Invoicing 4', steps: [37], status: 'covered',
    location: 'Invoices → Invoice Detail → Attachments panel: upload PDF/A attachments directly to a single invoice. Same 10-file / 1 MB per file limit. The upload zone shows file name, size, and SHA1 hash after upload.',
  },
  {
    id: 'PD-306', summary: '3.4.6 Displaying invoice image', release: 'Invoicing 4', steps: [51, 56], status: 'covered',
    location: 'Invoices → Invoice Detail → "View Invoice Image" button (standard user view). Tools → Authority: Inv. View → each invoice row also has a "View Image" button for AUTHORITY_VIEWER role. Both fetch the PDF from the external system; the authority view enforces the SENT/COMPLETED-only restriction.',
  },
]

function getUniquePages(steps) {
  const seen = new Set()
  const result = []
  for (const step of steps) {
    const pages = STEP_PAGES[step] || []
    for (const page of pages) {
      if (!seen.has(page.path)) {
        seen.add(page.path)
        result.push(page)
      }
    }
  }
  return result
}

const REQUIREMENTS = RAW_REQUIREMENTS.map(r => ({
  ...r,
  pages: getUniquePages(r.steps),
}))

const RELEASE_COLORS = {
  'Invoicing 1': { bg: '#eff6ff', border: '#bfdbfe', text: '#1d4ed8' },
  'Invoicing 2': { bg: '#f0fdf4', border: '#bbf7d0', text: '#15803d' },
  'Invoicing 3': { bg: '#fefce8', border: '#fde68a', text: '#a16207' },
  'Invoicing 4': { bg: '#fdf4ff', border: '#e9d5ff', text: '#7e22ce' },
}

export default function PdOverviewPage() {
  const [search, setSearch] = useState('')
  const [activeRelease, setActiveRelease] = useState('all')

  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return REQUIREMENTS.filter(r => {
      const matchesSearch = !q || r.id.toLowerCase().includes(q) || r.summary.toLowerCase().includes(q) || r.location.toLowerCase().includes(q)
      const matchesRelease = activeRelease === 'all' || r.release === activeRelease
      return matchesSearch && matchesRelease
    })
  }, [search, activeRelease])

  const coveredCount = REQUIREMENTS.filter(r => r.status === 'covered').length
  const totalCount = REQUIREMENTS.length

  const grouped = useMemo(() => {
    return RELEASES.map(release => ({
      ...release,
      items: filtered.filter(r => r.release === release.key),
    })).filter(g => g.items.length > 0)
  }, [filtered])

  return (
    <div className="pd-overview">
      <div className="pd-overview__header">
        <div>
          <h1 className="pd-overview__title">PD Requirements — Implementation Overview</h1>
          <p className="pd-overview__subtitle">
            Navigate via the top section tabs (Operations · Invoices · Customers&nbsp;&amp;&nbsp;Config · Master Data · Tools)
            then pick the page in the left sub-nav. Each entry below shows the exact path.
          </p>
        </div>
        <div className="pd-overview__stats">
          <div className="pd-stat pd-stat--covered">
            <span className="pd-stat__number">{coveredCount}</span>
            <span className="pd-stat__label">Covered</span>
          </div>
          <div className="pd-stat pd-stat--deferred">
            <span className="pd-stat__number">{totalCount - coveredCount}</span>
            <span className="pd-stat__label">Deferred</span>
          </div>
          <div className="pd-stat">
            <span className="pd-stat__number">{totalCount}</span>
            <span className="pd-stat__label">Total</span>
          </div>
        </div>
      </div>

      <div className="pd-overview__toolbar">
        <input
          className="pd-overview__search"
          type="text"
          placeholder="Search by PD number, keyword, or location…"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <div className="pd-overview__filters">
          <button
            className={`pd-filter-btn${activeRelease === 'all' ? ' active' : ''}`}
            onClick={() => setActiveRelease('all')}
          >
            All releases
          </button>
          {RELEASES.map(r => (
            <button
              key={r.key}
              className={`pd-filter-btn${activeRelease === r.key ? ' active' : ''}`}
              style={activeRelease === r.key ? {
                background: RELEASE_COLORS[r.key].bg,
                borderColor: RELEASE_COLORS[r.key].border,
                color: RELEASE_COLORS[r.key].text,
              } : {}}
              onClick={() => setActiveRelease(r.key)}
            >
              {r.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="pd-overview__empty">No requirements match your search.</div>
      ) : (
        grouped.map(group => {
          const colors = RELEASE_COLORS[group.key]
          const groupCovered = group.items.filter(i => i.status === 'covered').length
          return (
            <div key={group.key} className="pd-release-group">
              <div className="pd-release-group__header">
                <div className="pd-release-group__title-row">
                  <span
                    className="pd-release-badge"
                    style={{ background: colors.bg, borderColor: colors.border, color: colors.text }}
                  >
                    {group.label}
                  </span>
                  <span className="pd-release-group__due">Due {group.due}</span>
                </div>
                <span className="pd-release-group__count">
                  {groupCovered}/{group.items.length} covered
                </span>
              </div>

              <div className="pd-table-wrapper">
                <table className="pd-table">
                  <thead>
                    <tr>
                      <th className="pd-table__th pd-table__th--id">PD</th>
                      <th className="pd-table__th pd-table__th--summary">Requirement</th>
                      <th className="pd-table__th pd-table__th--steps">Steps</th>
                      <th className="pd-table__th pd-table__th--location">Where to find it</th>
                      <th className="pd-table__th pd-table__th--status">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {group.items.map(req => (
                      <tr key={req.id} className={`pd-table__row${req.status === 'deferred' ? ' pd-table__row--deferred' : ''}`}>
                        <td className="pd-table__td pd-table__td--id">
                          <a
                            href={`https://ioteelab.atlassian.net/browse/${req.id}`}
                            target="_blank"
                            rel="noreferrer"
                            className="pd-id-link"
                          >
                            {req.id}
                          </a>
                        </td>
                        <td className="pd-table__td pd-table__td--summary">{req.summary}</td>
                        <td className="pd-table__td pd-table__td--steps">
                          <div className="pd-steps">
                            {req.steps.length === 0 ? (
                              <span className="pd-steps__none">—</span>
                            ) : (
                              req.steps.map(s => (
                                <span key={s} className="pd-step-chip">
                                  {String(s).padStart(2, '0')}
                                </span>
                              ))
                            )}
                          </div>
                        </td>
                        <td className="pd-table__td pd-table__td--location">
                          <p className="pd-location-text">{req.location}</p>
                          {req.pages.length > 0 && (
                            <div className="pd-pages">
                              {req.pages.map(page => (
                                <Link key={page.path} to={page.path} className="pd-page-link">
                                  {page.label}
                                </Link>
                              ))}
                            </div>
                          )}
                        </td>
                        <td className="pd-table__td pd-table__td--status">
                          {req.status === 'covered' ? (
                            <span className="pd-status pd-status--covered">Covered</span>
                          ) : (
                            <span className="pd-status pd-status--deferred">Deferred</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )
        })
      )}
    </div>
  )
}
