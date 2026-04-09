import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import './Layout.css'

/* ── Navigation structure ──────────────────────────────────
   Each section has:
   • to       — default route (where the tab navigates on click)
   • matches  — pathname prefixes that make this tab active
   • items    — sub-nav links shown in the left panel
   ──────────────────────────────────────────────────────── */
const SECTIONS = [
  {
    label: 'Operations',
    to: '/billing-events',
    matches: ['/billing-events', '/billing/cycles', '/billing/restrictions', '/billing/seasonal-fees'],
    items: [
      { label: 'Billing Events',    to: '/billing-events' },
      { label: 'Review Queue',      to: '/billing-events/review-queue' },
      { label: 'Billing Cycles',    to: '/billing/cycles' },
      { label: 'Restrictions',      to: '/billing/restrictions' },
      { label: 'Seasonal Fees',     to: '/billing/seasonal-fees' },
    ],
  },
  {
    label: 'Invoices',
    to: '/invoices',
    matches: ['/invoices', '/runs'],
    items: [
      { label: 'Invoice List',      to: '/invoices',          end: true },
      { label: 'Generate Invoice',  to: '/invoices/generate', end: true },
      { label: 'Invoice Runs',      to: '/runs/new',          end: true },
    ],
  },
  {
    label: 'Customers & Config',
    to: '/customers',
    matches: ['/customers', '/shared-services', '/config'],
    items: [
      { label: 'Billing Profiles',     to: '/customers',                        end: true },
      { label: 'Property Groups',      to: '/shared-services/property-groups',  end: true },
      { label: 'Classification Rules', to: '/config/classification-rules',      end: true },
      { label: 'Validation Rules',     to: '/config/validation-rules',          end: true },
    ],
  },
  {
    label: 'Master Data',
    to: '/master-data',
    matches: ['/master-data'],
    items: [
      { label: 'Overview',        to: '/master-data',                         end: true },
      { label: 'VAT Rates',       to: '/master-data/vat-rates',               end: true },
      { label: 'Accounting',      to: '/master-data/accounting-accounts',     end: true },
      { label: 'Cost Centers',    to: '/master-data/cost-centers',            end: true },
      { label: 'Products',        to: '/master-data/products',                end: true },
      { label: 'Number Series',   to: '/master-data/invoice-number-series',   end: true },
      { label: 'Allocation Rules',to: '/master-data/allocation-rules',        end: true },
      { label: 'Surcharge',       to: '/master-data/surcharge-config',        end: true },
      { label: 'Min. Fees',       to: '/master-data/minimum-fee-config',      end: true },
    ],
  },
  {
    label: 'Tools',
    to: '/retroactive/price-adjustment',
    matches: ['/retroactive', '/integration', '/authority', '/pd-overview'],
    items: [
      { label: 'Price Adjustment',  to: '/retroactive/price-adjustment',    end: true },
      { label: 'Resp. Change',      to: '/retroactive/responsibility-change',end: true },
      { label: 'Address Sync',      to: '/integration/billing-sync',        end: true },
      { label: 'E-Invoice Op.',     to: '/integration/operator',            end: true },
      { label: 'Authority View',    to: '/authority/invoices',              end: true },
      { label: 'PD Overview',       to: '/pd-overview',                     end: true },
    ],
  },
]

function getActiveSection(pathname) {
  return (
    SECTIONS.find((s) =>
      s.matches.some((m) => pathname === m || pathname.startsWith(m + '/'))
    ) ?? null
  )
}

export default function Layout() {
  const { pathname } = useLocation()
  const active = getActiveSection(pathname)

  return (
    <div className="inv-layout">

      {/* ── Inner topbar ── */}
      <header className="inv-topbar">
        {/* Module brand */}
        <Link to="/master-data" className="inv-brand">
          <svg className="inv-brand__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          <span className="inv-brand__name">Invoicing</span>
        </Link>

        <div className="inv-topbar__sep" />

        {/* Section tabs */}
        <nav className="inv-topbar__tabs">
          {SECTIONS.map((s) => (
            <Link
              key={s.label}
              to={s.to}
              className={`inv-tab${active?.label === s.label ? ' inv-tab--active' : ''}`}
            >
              {s.label}
            </Link>
          ))}
        </nav>
      </header>

      {/* ── Section sub-nav ── */}
      <nav className="inv-subnav">
        {active ? (
          <>
            <p className="inv-subnav__section-label">{active.label}</p>
            {active.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end ?? false}
                className={({ isActive }) =>
                  `inv-subnav__item${isActive ? ' active' : ''}`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </>
        ) : (
          <p className="inv-subnav__hint">Select a section above</p>
        )}
      </nav>

      {/* ── Page content ── */}
      <main className="inv-content">
        <Outlet />
      </main>
    </div>
  )
}
