import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import './WasteHeroShell.css'

const OUTER_NAV = [
  {
    label: 'Data & Analytics',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
        <line x1="6" y1="20" x2="6" y2="14"/>
      </svg>
    ),
    children: ['Overview', 'Dashboards', 'Data Exports', 'Classic Exports', 'Data Imports'],
  },
  {
    label: 'Customers',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
    ),
    children: ['Properties', 'Map View', 'Groups', 'Contacts', 'SMS / Email Service'],
  },
  {
    label: 'Tickets',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/>
      </svg>
    ),
    children: ['Overview', 'Map View', 'Kanban', 'Customer Inbox'],
  },
  {
    label: 'Operations',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
      </svg>
    ),
    children: ['Routes', 'Route Schemes', 'Pickup History', 'Weight Control'],
  },
  {
    label: 'Fleet',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="1" y="3" width="15" height="13" rx="2"/>
        <path d="M16 8h4l3 3v5h-7V8z"/>
        <circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>
      </svg>
    ),
    children: ['Vehicles', 'Drivers', 'Locations', 'Planning'],
  },
  {
    label: 'Assets',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
      </svg>
    ),
    children: ['Containers', 'Map View', 'Groups'],
  },
  {
    label: 'Alerts',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
    ),
    children: [],
  },
]

export default function WasteHeroShell() {
  const location = useLocation()
  const breadcrumb = location.pathname === '/settings' ? 'Settings' : 'Dashboard'

  const [expanded, setExpanded] = useState(() => new Set(['Customers', 'Tickets']))
  const [bannerVisible, setBannerVisible] = useState(true)

  function toggle(label) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(label)) next.delete(label)
      else next.add(label)
      return next
    })
  }

  return (
    <div className="wh-shell">
      {/* ── Topbar ── */}
      <header className="wh-shell__topbar">
        <div className="wh-shell__topbar-left">
          <Link to="/404" className="wh-shell__brand">
            <div className="wh-shell__logo-mark">W</div>
            <span className="wh-shell__brand-name">WasteHero</span>
          </Link>
          <div className="wh-shell__topbar-divider" />
          <span className="wh-shell__breadcrumb">{breadcrumb}</span>
        </div>

        <div className="wh-shell__topbar-center">
          <div className="wh-shell__search-bar">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <span className="wh-shell__search-placeholder">Search…</span>
          </div>
        </div>

        <div className="wh-shell__topbar-right">
          <Link to="/404" className="wh-shell__icon-btn" title="Notifications">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
          </Link>
          <Link to="/settings" className="wh-shell__icon-btn" title="Settings">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
          </Link>
          <Link to="/404" className="wh-shell__avatar" title="Profile">JD</Link>
        </div>
      </header>

      {/* ── Render free-tier notice ── */}
      {bannerVisible && (
        <div style={{
          position: 'fixed',
          bottom: '20px',
          right: '20px',
          width: '320px',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px',
          padding: '12px 14px',
          background: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '10px',
          boxShadow: '0 4px 16px rgba(0,0,0,0.10)',
          fontSize: '13px',
          color: '#374151',
          zIndex: 'calc(var(--wh-shell-z) + 10)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#d97706" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <strong style={{ fontSize: '13px', color: '#111827' }}>API on Render free tier</strong>
            <button
              onClick={() => setBannerVisible(false)}
              style={{
                marginLeft: 'auto',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: '#9ca3af',
                padding: '2px',
                lineHeight: 1,
                flexShrink: 0,
              }}
              title="Dismiss"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <p style={{ margin: 0, lineHeight: '1.5', color: '#6b7280' }}>
            If no data loads or the status is stuck on loading, the server may be cold-starting. Wait up to <strong style={{ color: '#374151' }}>5 minutes</strong> and refresh.
          </p>
        </div>
      )}

      {/* ── Left sidebar ── */}
      <nav className="wh-shell__sidebar">
        <div className="wh-shell__sidebar-search">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <span>Search…</span>
        </div>

        {OUTER_NAV.map((section) => {
          const isOpen = expanded.has(section.label)
          const hasChildren = section.children.length > 0

          return (
            <div key={section.label} className="wh-shell__nav-section">
              <button
                className="wh-shell__nav-item"
                onClick={() => hasChildren && toggle(section.label)}
                style={{ cursor: hasChildren ? 'pointer' : 'default' }}
              >
                <span className="wh-shell__nav-item-inner">
                  <span className="wh-shell__nav-icon">{section.icon}</span>
                  <span className="wh-shell__nav-label">{section.label}</span>
                </span>
                {hasChildren && (
                  <span className={`wh-shell__nav-chevron${isOpen ? ' wh-shell__nav-chevron--open' : ''}`}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </span>
                )}
              </button>

              {hasChildren && isOpen && (
                <div className="wh-shell__nav-children">
                  {section.children.map((child) => (
                    <Link key={child} to="/404" className="wh-shell__nav-child">
                      {child}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </nav>

      {/* ── Content area — invoicing app renders here ── */}
      <div className="wh-shell__main">
        <Outlet />
      </div>
    </div>
  )
}
