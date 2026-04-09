import { Link } from 'react-router-dom'
import { useState } from 'react'
import './SettingsPage.css'

/* ── Card data ───────────────────────────────────────────── */

function Icon404() {
  return null // placeholder — cards below provide their own icon
}

const SECTIONS = [
  {
    title: 'System',
    cards: [
      {
        title: 'Company system preferences',
        description: 'Manage your company settings and company-based different features.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        ),
        color: '#737373',
      },
      {
        title: 'Small Accounts',
        description: 'Companies that want to connect with your customers and other organizations.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
        ),
        color: '#0369a1',
      },
      {
        title: 'User Management',
        description: "Administrate user's roles and team.",
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        ),
        color: '#7c3aed',
      },
      {
        title: 'System history',
        description: 'View all historical changes including books, containers, projects and company information.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="12 8 12 12 14 14"/>
            <path d="M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5"/>
          </svg>
        ),
        color: '#059669',
      },
    ],
  },
  {
    title: 'Platform',
    cards: [
      {
        title: 'Invoicing',
        description: 'Manage billing events, invoices, customers, products and all invoicing configuration.',
        to: '/',
        invoicing: true,
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        ),
        color: '#155dfc',
      },
      {
        title: 'Asset management',
        description: 'Manage and configure different assets imported from agreements.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>
          </svg>
        ),
        color: '#d97706',
      },
      {
        title: 'Operation management',
        description: 'Use your operations for faster and easy route creation and processing plans.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
        ),
        color: '#0369a1',
      },
      {
        title: 'Tickets',
        description: 'Adjust ticket types and categories and customize ticket processing flow.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/>
          </svg>
        ),
        color: '#7c3aed',
      },
      {
        title: 'Customer management',
        description: 'Manage your platform and set up by picking features for the platform.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        ),
        color: '#059669',
      },
      {
        title: 'Print management',
        description: "Manage your company's operation print settings.",
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="6 9 6 2 18 2 18 9"/>
            <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
            <rect x="6" y="14" width="12" height="8"/>
          </svg>
        ),
        color: '#0369a1',
      },
      {
        title: 'Product Management',
        description: 'Manage your categories, zones, and selling options for products.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
            <line x1="3" y1="6" x2="21" y2="6"/>
            <path d="M16 10a4 4 0 0 1-8 0"/>
          </svg>
        ),
        color: '#dc2626',
      },
      {
        title: 'Documents',
        description: 'Upload documents for documentation sharing for platform users.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <line x1="10" y1="9" x2="8" y2="9"/>
          </svg>
        ),
        color: '#737373',
      },
      {
        title: 'Custom fields',
        description: 'Manage and configure custom fields for tickets, zones, products, and more.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/>
            <line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/>
            <line x1="1" y1="14" x2="7" y2="14"/>
            <line x1="9" y1="8" x2="15" y2="8"/>
            <line x1="17" y1="16" x2="23" y2="16"/>
          </svg>
        ),
        color: '#d97706',
      },
      {
        title: 'Communication',
        description: 'Manage your notification settings and review the history of sent messages.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        ),
        color: '#7c3aed',
      },
    ],
  },
  {
    title: 'Other systems',
    cards: [
      {
        title: 'Citizen portal',
        description: 'Customer-facing frontend portal powered by WasteHero.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="2" y1="12" x2="22" y2="12"/>
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
          </svg>
        ),
        color: '#0369a1',
      },
      {
        title: 'Navigation App',
        description: 'Field navigation app powered by WasteHero.',
        to: '/404',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="3 11 22 2 13 21 11 13 3 11"/>
          </svg>
        ),
        color: '#059669',
      },
    ],
  },
]

/* ── Component ───────────────────────────────────────────── */

export default function SettingsPage() {
  const [search, setSearch] = useState('')
  const query = search.toLowerCase()

  const filtered = SECTIONS.map((s) => ({
    ...s,
    cards: s.cards.filter(
      (c) =>
        !query ||
        c.title.toLowerCase().includes(query) ||
        c.description.toLowerCase().includes(query),
    ),
  })).filter((s) => s.cards.length > 0)

  return (
    <div className="sp-page">
      {/* ── Left profile panel ── */}
      <aside className="sp-profile">
        <div className="sp-profile__avatar">JS</div>
        <p className="sp-profile__greeting">Hello,</p>
        <p className="sp-profile__name">Jack Spinola</p>
        <Link to="/404" className="sp-profile__edit-link">Edit</Link>

        <div className="sp-profile__divider" />

        <p className="sp-profile__section-label">Your company</p>
        <p className="sp-profile__company-name">WasteHero</p>
        <p className="sp-profile__company-domain">wastehero.io</p>
        <p className="sp-profile__company-address">Copenhagen, Denmark</p>
        <Link to="/404" className="sp-profile__edit-link">Edit</Link>

        <div className="sp-profile__divider" />

        <div className="sp-profile__projects-row">
          <span className="sp-profile__section-label" style={{ margin: 0 }}>Projects</span>
          <Link to="/404" className="sp-profile__add-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
          </Link>
        </div>
        <p className="sp-profile__projects-empty">No projects yet.</p>
      </aside>

      {/* ── Main content ── */}
      <div className="sp-content">
        {/* Search */}
        <div className="sp-search-wrap">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            className="sp-search"
            type="text"
            placeholder="Search settings…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {/* Sections */}
        {filtered.map((section) => (
          <div key={section.title} className="sp-section">
            <h2 className="sp-section__title">{section.title}</h2>
            <div className="sp-card-grid">
              {section.cards.map((card) => (
                <Link
                  key={card.title}
                  to={card.to}
                  className={`sp-card${card.invoicing ? ' sp-card--invoicing' : ''}`}
                >
                  <div className="sp-card__icon" style={{ color: card.color, background: card.color + '18' }}>
                    {card.icon}
                  </div>
                  <div className="sp-card__body">
                    <div className="sp-card__title-row">
                      <span className="sp-card__title">{card.title}</span>
                      {card.invoicing && <span className="sp-card__badge">Active</span>}
                    </div>
                    <p className="sp-card__desc">{card.description}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        ))}

        {filtered.length === 0 && (
          <p className="sp-empty">No settings match &ldquo;{search}&rdquo;.</p>
        )}
      </div>
    </div>
  )
}
