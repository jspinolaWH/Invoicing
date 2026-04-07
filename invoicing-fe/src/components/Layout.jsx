import { NavLink, Outlet } from 'react-router-dom'
import './Layout.css'

const navItems = [
  {
    label: 'Master Data',
    children: [
      { label: 'VAT Rates', to: '/master-data/vat-rates' },
      { label: 'Accounting Accounts', to: '/master-data/accounting-accounts' },
      { label: 'Cost Centers', to: '/master-data/cost-centers' },
      { label: 'Products', to: '/master-data/products' },
      { label: 'Invoice Number Series', to: '/master-data/invoice-number-series' },
      { label: 'Allocation Rules', to: '/master-data/allocation-rules' },
      { label: 'Surcharge Config', to: '/master-data/surcharge-config' },
      { label: 'Minimum Fee Config', to: '/master-data/minimum-fee-config' },
    ],
  },
  {
    label: 'Billing',
    children: [
      { label: 'Billing Events', to: '/billing-events' },
      { label: 'Review Queue', to: '/billing-events/review-queue' },
      { label: 'Billing Cycles', to: '/billing/cycles' },
      { label: 'Billing Restrictions', to: '/billing/restrictions' },
      { label: 'Seasonal Fees', to: '/billing/seasonal-fees' },
    ],
  },
  {
    label: 'Invoices',
    children: [
      { label: 'Generate Invoice', to: '/invoices/generate' },
    ],
  },
  {
    label: 'Shared Services',
    children: [
      { label: 'Property Groups', to: '/shared-services/property-groups' },
    ],
  },
  {
    label: 'Customers',
    children: [
      { label: 'Billing Profiles', to: '/customers' },
    ],
  },
  {
    label: 'Configuration',
    children: [
      { label: 'Classification Rules', to: '/config/classification-rules' },
      { label: 'Validation Rules', to: '/config/validation-rules' },
    ],
  },
]

export default function Layout() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-brand">Invoicing</div>
        <nav>
          {navItems.map((group) => (
            <div key={group.label} className="nav-group">
              <div className="nav-group-label">{group.label}</div>
              {group.children.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
