import { useEffect, useState } from 'react'
import axios from 'axios'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-292', label: '3.4.20 Billing restrictions / external tool integration', href: 'https://ioteelab.atlassian.net/browse/PD-292' },
]

function StatusDot({ ok }) {
  return (
    <span style={{
      display: 'inline-block', width: 10, height: 10, borderRadius: '50%',
      background: ok ? '#16a34a' : '#dc2626',
      marginRight: 8, flexShrink: 0,
    }} />
  )
}

function billingTypeBadge(type) {
  if (!type) return <span className="muted">—</span>
  const immediate = type === 'IMMEDIATE'
  return (
    <span style={{
      padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 600,
      border: `1px solid ${immediate ? '#fcd34d' : '#bae6fd'}`,
      background: immediate ? '#fffbeb' : '#eff6ff',
      color: immediate ? '#92400e' : '#0369a1',
    }}>
      {immediate ? 'Immediate' : 'Cycle-based'}
    </span>
  )
}

export default function ErpIntegrationStatusPage() {
  const [recentInvoices, setRecentInvoices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lastRefreshed, setLastRefreshed] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    axios.get('/api/v1/invoices?page=0&size=10')
      .then(r => {
        setRecentInvoices(r.data.content || r.data || [])
        setLastRefreshed(new Date())
      })
      .catch(() => setError('Failed to load recent dispatch data.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const sent = recentInvoices.filter(i => i.status === 'SENT' || i.status === 'COMPLETED').length
  const failed = recentInvoices.filter(i => i.status === 'ERROR').length
  const pending = recentInvoices.filter(i => i.status !== 'SENT' && i.status !== 'COMPLETED' && i.status !== 'ERROR').length

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>ERP Integration Status</h1>
          <p>Visibility into external financial tool dispatch — FINVOICE 3.0 operator pipeline</p>
        </div>
        <button className="btn-secondary" onClick={load} disabled={loading}>
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      <div style={{
        display: 'flex', alignItems: 'flex-start', gap: 10,
        padding: '12px 16px', marginBottom: 'var(--space-4)',
        background: '#eff6ff', border: '1px solid #bfdbfe',
        borderRadius: 8, fontSize: 'var(--font-size-sm)', color: '#1e40af',
      }}>
        <span style={{ fontWeight: 700, flexShrink: 0, fontSize: 15 }}>i</span>
        <span>
          Invoices are dispatched to the external financial system via the FINVOICE 3.0 operator pipeline. Each invoice run generates one or more FINVOICE XML files that are transmitted to the operator automatically. <strong>IMMEDIATE</strong> billing-type invoices are dispatched as soon as a run completes; <strong>CYCLE_BASED</strong> invoices are dispatched in the same run but are generated only when the billing cycle is due.
        </span>
      </div>

      {error && <div className="error-msg">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 'var(--space-4)', marginBottom: 'var(--space-6)' }}>
        <div style={{ padding: 'var(--space-4)', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8 }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#15803d' }}>{sent}</div>
          <div style={{ fontSize: 13, color: '#166534', marginTop: 4 }}>Dispatched (recent 10)</div>
        </div>
        <div style={{ padding: 'var(--space-4)', background: '#fff1f2', border: '1px solid #fecdd3', borderRadius: 8 }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#b91c1c' }}>{failed}</div>
          <div style={{ fontSize: 13, color: '#991b1b', marginTop: 4 }}>Dispatch errors (recent 10)</div>
        </div>
        <div style={{ padding: 'var(--space-4)', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 8 }}>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#92400e' }}>{pending}</div>
          <div style={{ fontSize: 13, color: '#78350f', marginTop: 4 }}>Pending / in progress (recent 10)</div>
        </div>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: 'var(--space-3)' }}>Integration Configuration</h2>
        <table className="data-table">
          <thead>
            <tr><th>Setting</th><th>Value</th><th>Status</th></tr>
          </thead>
          <tbody>
            <tr>
              <td>Dispatch protocol</td>
              <td>FINVOICE 3.0 (XML via operator)</td>
              <td><StatusDot ok={true} />Active</td>
            </tr>
            <tr>
              <td>Immediate invoice dispatch</td>
              <td>Triggered at end of each invoice run</td>
              <td><StatusDot ok={true} />Active</td>
            </tr>
            <tr>
              <td>Cycle-based invoice dispatch</td>
              <td>Triggered when billing cycle is due (hourly check)</td>
              <td><StatusDot ok={true} />Active</td>
            </tr>
            <tr>
              <td>Separate invoice streams</td>
              <td>IMMEDIATE and CYCLE_BASED invoices dispatched as separate documents</td>
              <td><StatusDot ok={true} />Active</td>
            </tr>
            <tr>
              <td>Operator connection</td>
              <td>Configured at infrastructure level</td>
              <td><span className="muted">Contact support to verify</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-3)' }}>
          <h2 style={{ fontSize: '1rem', margin: 0 }}>Recent Invoice Dispatch Log (last 10)</h2>
          {lastRefreshed && (
            <span className="muted" style={{ fontSize: 12 }}>
              Refreshed {lastRefreshed.toLocaleTimeString('fi-FI')}
            </span>
          )}
        </div>

        {loading ? (
          <div className="loading">Loading dispatch log…</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Invoice #</th>
                <th>Customer</th>
                <th>Date</th>
                <th>Billing Type</th>
                <th>Status</th>
                <th>Net Amount</th>
              </tr>
            </thead>
            <tbody>
              {recentInvoices.length === 0 && (
                <tr><td colSpan={6} className="empty">No invoices dispatched yet.</td></tr>
              )}
              {recentInvoices.map(inv => {
                const isError = inv.status === 'ERROR'
                const isOk = inv.status === 'SENT' || inv.status === 'COMPLETED'
                return (
                  <tr key={inv.id}>
                    <td>{inv.invoiceNumber || `#${inv.id}`}</td>
                    <td>{inv.customerName || <span className="muted">—</span>}</td>
                    <td>{inv.invoiceDate || <span className="muted">—</span>}</td>
                    <td>{billingTypeBadge(inv.billingType)}</td>
                    <td>
                      <span style={{
                        display: 'inline-flex', alignItems: 'center',
                        padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 500,
                        border: `1px solid ${isError ? '#fecdd3' : isOk ? '#bbf7d0' : '#fcd34d'}`,
                        background: isError ? '#fff1f2' : isOk ? '#f0fdf4' : '#fffbeb',
                        color: isError ? '#b91c1c' : isOk ? '#15803d' : '#92400e',
                      }}>
                        <StatusDot ok={isOk && !isError} />
                        {inv.status}
                      </span>
                    </td>
                    <td>{inv.netAmount != null ? Number(inv.netAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : <span className="muted">—</span>}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
