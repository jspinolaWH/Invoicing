import { Link } from 'react-router-dom'
import './NotFoundPage.css'

export default function NotFoundPage() {
  return (
    <div className="nf-page">
      <div className="nf-card">
        <div className="nf-logo">
          <div className="nf-logo__mark">W</div>
          <span className="nf-logo__text">WasteHero</span>
        </div>

        <div className="nf-divider" />

        <div className="nf-code">404</div>
        <h1 className="nf-title">Page not found</h1>
        <p className="nf-message">
          This section is part of the core WasteHero platform and is not
          available inside the <strong>Invoicing</strong> module.
        </p>

        <Link to="/" className="nf-back-btn">
          Back to Invoicing
        </Link>

        <p className="nf-hint">
          Looking for something in the invoicing system?
          Try&nbsp;
          <Link to="/billing-events" className="nf-link">Billing Events</Link>
          ,&nbsp;
          <Link to="/invoices" className="nf-link">Invoices</Link>
          , or&nbsp;
          <Link to="/master-data" className="nf-link">Master Data</Link>.
        </p>
      </div>
    </div>
  )
}
