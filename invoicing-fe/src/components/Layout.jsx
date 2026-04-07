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
    ],
  },
  {
    label: 'Billing',
    children: [
      { label: 'Billing Events', to: '/billing-events' },
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
