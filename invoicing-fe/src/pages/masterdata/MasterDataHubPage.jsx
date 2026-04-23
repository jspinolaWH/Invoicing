import { Link } from 'react-router-dom'
import './VatRatesPage.css'
import './MasterDataHubPage.css'

const CARDS = [
  {
    title: 'VAT Rates',
    path: '/master-data/vat-rates',
    description: 'Tax rates applied to billing events by event date.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="9" cy="9" r="3"/><circle cx="15" cy="15" r="3"/>
        <line x1="6" y1="18" x2="18" y2="6"/>
      </svg>
    ),
  },
  {
    title: 'Accounting Accounts',
    path: '/master-data/accounting-accounts',
    description: 'Chart of accounts used for invoice line allocation.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
      </svg>
    ),
  },
  {
    title: 'Cost Centers',
    path: '/master-data/cost-centers',
    description: 'Cost center segments used in accounting allocation.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="9" width="18" height="13" rx="2"/>
        <path d="M8 22V9M16 22V9M3 13h18M3 17h18"/>
        <path d="M9 5l3-3 3 3"/>
      </svg>
    ),
  },
  {
    title: 'Products',
    path: '/master-data/products',
    description: 'Billable product codes with pricing unit and VAT class.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 8V21H3V8"/><path d="M23 3H1l2 5h18l2-5z"/>
        <line x1="12" y1="12" x2="12" y2="21"/>
      </svg>
    ),
  },
  {
    title: 'Invoice Number Series',
    path: '/master-data/invoice-number-series',
    description: 'Number sequences and format patterns for generated invoices.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="4" y1="9" x2="20" y2="9"/><line x1="4" y1="15" x2="20" y2="15"/>
        <line x1="10" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="14" y2="21"/>
      </svg>
    ),
  },
  {
    title: 'Invoice Templates',
    path: '/master-data/invoice-templates',
    description: 'Invoice templates linked to a number series for structured invoicing.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>
      </svg>
    ),
  },
  {
    title: 'Allocation Rules',
    path: '/master-data/allocation-rules',
    description: 'Rules that map products and regions to accounting accounts.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
        <path d="M9 12h6M8.59 7.41l6.82 3.18M8.59 16.59l6.82-3.18"/>
      </svg>
    ),
  },
  {
    title: 'Surcharge Config',
    path: '/master-data/surcharge-config',
    description: 'Delivery-method surcharges automatically added to invoices.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="16"/>
        <line x1="8" y1="12" x2="16" y2="12"/>
      </svg>
    ),
  },
  {
    title: 'Minimum Fee Config',
    path: '/master-data/minimum-fee-config',
    description: 'Minimum fee thresholds applied by customer type and billing period.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
        <line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>
      </svg>
    ),
  },
  {
    title: 'VAT Report',
    path: '/reports/vat',
    description: 'Standard VAT totals and reverse charge VAT base totals by date range.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/>
        <line x1="12" y1="17" x2="12" y2="21"/><line x1="7" y1="8" x2="7" y2="13"/>
        <line x1="12" y1="6" x2="12" y2="13"/><line x1="17" y1="10" x2="17" y2="13"/>
      </svg>
    ),
  },
  {
    title: 'Price Lists',
    path: '/master-data/price-lists',
    description: 'Tariff price lists with validity periods and customer type variants.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 2L2 7l10 5 10-5-10-5z"/>
        <path d="M2 17l10 5 10-5"/>
        <path d="M2 12l10 5 10-5"/>
      </svg>
    ),
  },
  {
    title: 'Accounting Report',
    path: '/reports/accounting',
    description: 'Revenue and cost breakdown by accounting account and cost center.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="4"/><line x1="12" y1="20" x2="12" y2="10"/>
        <line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>
      </svg>
    ),
  },
  {
    title: 'Projects',
    path: '/master-data/projects',
    description: 'Customer-linked projects for project-based invoicing and separate invoice generation.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="7" width="20" height="14" rx="2"/>
        <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
        <line x1="12" y1="12" x2="12" y2="16"/>
        <line x1="10" y1="14" x2="14" y2="14"/>
      </svg>
    ),
  },
  {
    title: 'Weighbridge Config',
    path: '/integration/weighbridge-config',
    description: 'Customer-specific weighbridge integration settings for billing event ingestion.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.07 4.93a10 10 0 0 1 0 14.14M4.93 4.93a10 10 0 0 0 0 14.14"/>
        <path d="M16.24 7.76a6 6 0 0 1 0 8.49M7.76 7.76a6 6 0 0 0 0 8.49"/>
      </svg>
    ),
  },
  {
    title: 'Invoicing Defaults',
    path: '/config/invoicing-defaults',
    description: 'Company-wide default invoicing mode (gross/net) applied when a customer profile has no override.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="3"/>
        <line x1="12" y1="2" x2="12" y2="6"/>
        <line x1="12" y1="18" x2="12" y2="22"/>
        <line x1="4.22" y1="4.22" x2="7.05" y2="7.05"/>
        <line x1="16.95" y1="16.95" x2="19.78" y2="19.78"/>
        <line x1="2" y1="12" x2="6" y2="12"/>
        <line x1="18" y1="12" x2="22" y2="12"/>
        <line x1="4.22" y1="19.78" x2="7.05" y2="16.95"/>
        <line x1="16.95" y1="7.05" x2="19.78" y2="4.22"/>
      </svg>
    ),
  },
]

export default function MasterDataHubPage() {
  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Master Data</h1>
          <p>Reference tables that drive billing calculations, invoice generation, and accounting allocation.</p>
        </div>
      </div>

      <div className="hub-grid">
        {CARDS.map(card => (
          <Link key={card.path} to={card.path} className="hub-card">
            <div className="hub-card-icon">{card.icon}</div>
            <div className="hub-card-title">{card.title}</div>
            <div className="hub-card-desc">{card.description}</div>
            <div className="hub-card-cta">Open →</div>
          </Link>
        ))}
      </div>
    </div>
  )
}
