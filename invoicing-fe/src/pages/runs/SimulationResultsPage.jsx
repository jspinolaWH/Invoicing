import { useLocation, useNavigate } from 'react-router-dom'
import { createRun } from '../../api/invoiceRuns'
import { useState } from 'react'
import '../masterdata/VatRatesPage.css'

export default function SimulationResultsPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const [committing, setCommitting] = useState(false)
  const [error, setError] = useState(null)

  const report = state?.report
  const filter = state?.filter

  if (!report) return <div className="page"><p>No simulation data. Go back and run a simulation.</p></div>

  const blockingFailures = (report.validationFailures || []).filter(f => f.severity === 'BLOCKING')
  const warnings = (report.validationFailures || []).filter(f => f.severity === 'WARNING')
  const hasBlocking = blockingFailures.length > 0

  const handleCommit = async () => {
    setCommitting(true); setError(null)
    try {
      const res = await createRun({ ...filter, simulationMode: false })
      navigate(`/runs/${res.data.id}`)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to start run')
    } finally {
      setCommitting(false)
    }
  }

  const fmt = (n) => n != null ? Number(n).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Simulation Results</h1>
          <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>Review before committing a real run.</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {/* Summary */}
      <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', marginBottom: 'var(--space-4)' }}>
        {[
          ['Total Customers', report.totalCustomers],
          ['Total Invoices', report.totalInvoices],
          ['Failed Customers', report.failedCustomers || 0],
        ].map(([label, val]) => (
          <div key={label} style={{ flex: '1', minWidth: 130, padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{label}</div>
            <div style={{ fontSize: 22, fontWeight: 600 }}>{val ?? '—'}</div>
          </div>
        ))}
        {[
          ['Net Total', fmt(report.totalNetAmount)],
          ['VAT Total', fmt(report.totalVatAmount)],
          ['Gross Total', fmt(report.totalGrossAmount)],
        ].map(([label, val]) => (
          <div key={label} style={{ flex: '1', minWidth: 160, padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{label}</div>
            <div style={{ fontSize: 22, fontWeight: 600 }}>{val}</div>
          </div>
        ))}
      </div>

      {/* Validation Failures */}
      {(report.validationFailures || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Validation Issues</h3>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Severity</th>
                <th>Customer</th>
                <th>Rule Type</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {[...blockingFailures, ...warnings].map((f, i) => (
                <tr key={i}>
                  <td>
                    <span className={`badge ${f.severity === 'BLOCKING' ? 'badge-red' : 'badge-amber'}`}>
                      {f.severity}
                    </span>
                  </td>
                  <td>{f.customerName || f.customerId}</td>
                  <td>{f.ruleType}</td>
                  <td>{f.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Sample Invoices */}
      {(report.sampleLineItems || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Sample Invoices (first 5)</h3>
          {report.sampleLineItems.map((entry, i) => (
            <details key={i} style={{ marginBottom: 'var(--space-2)', padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
              <summary style={{ cursor: 'pointer', fontWeight: 600 }}>
                {entry.customerName || `Customer ${entry.customerId}`} — {fmt(entry.grossAmount)} ({entry.lineItemCount} lines)
              </summary>
              <div style={{ marginTop: 'var(--space-2)', fontSize: 13, color: 'var(--color-text-secondary)' }}>
                Net: {fmt(entry.netAmount)} | Gross: {fmt(entry.grossAmount)}
              </div>
            </details>
          ))}
        </>
      )}

      {/* Commit */}
      <div style={{ marginTop: 'var(--space-5)' }}>
        {hasBlocking && (
          <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>
            Cannot start real run: {blockingFailures.length} blocking validation failure{blockingFailures.length !== 1 ? 's' : ''} must be resolved first.
          </div>
        )}
        {error && <div className="error-msg" style={{ marginBottom: 'var(--space-2)' }}>{error}</div>}
        <button className="btn-primary" onClick={handleCommit} disabled={hasBlocking || committing}>
          {committing ? 'Starting…' : 'Commit Real Run'}
        </button>
      </div>
    </div>
  )
}
