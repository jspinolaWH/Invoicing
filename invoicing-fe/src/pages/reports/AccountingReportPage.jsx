import { useState } from 'react'
import { fetchAccountingReport } from '../../api/accountingReport'
import '../masterdata/VatRatesPage.css'

export default function AccountingReportPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleGenerate(e) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setReport(null)
    try {
      const data = await fetchAccountingReport({ from: from || undefined, to: to || undefined })
      setReport(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fmt = (n) =>
    Number(n).toLocaleString('fi-FI', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 style={{ margin: 0 }}>Accounting Report</h1>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: 4 }}>
            Revenue and cost breakdown by accounting account and cost center for a given date range.
          </p>
        </div>
      </div>

      <form onSubmit={handleGenerate} style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 24 }}>
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 500, marginBottom: 4, color: 'var(--color-text-secondary)' }}>
            From
          </label>
          <input
            type="date"
            value={from}
            onChange={e => setFrom(e.target.value)}
            style={{ padding: '6px 10px', border: '1px solid var(--color-border)', borderRadius: 6 }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 500, marginBottom: 4, color: 'var(--color-text-secondary)' }}>
            To
          </label>
          <input
            type="date"
            value={to}
            onChange={e => setTo(e.target.value)}
            style={{ padding: '6px 10px', border: '1px solid var(--color-border)', borderRadius: 6 }}
          />
        </div>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Generating…' : 'Generate Report'}
        </button>
      </form>

      {error && (
        <div style={{ color: 'var(--color-danger)', background: 'var(--color-danger-bg)', padding: '10px 14px', borderRadius: 6, marginBottom: 16 }}>
          {error}
        </div>
      )}

      {report && (
        <>
          <div style={{ marginBottom: 8, fontSize: 12, color: 'var(--color-text-secondary)' }}>
            Period: {report.from ?? 'all'} — {report.to ?? 'all'}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginTop: 8 }}>
            <div>
              <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>By Accounting Account</h2>
              {report.byAccount.length === 0 ? (
                <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>No data for selected period.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Account Code</th>
                      <th>Account Name</th>
                      <th style={{ textAlign: 'right' }}>Net Amount (€)</th>
                      <th style={{ textAlign: 'right' }}>Lines</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.byAccount.map(row => (
                      <tr key={row.accountCode}>
                        <td><span className="code-badge">{row.accountCode}</span></td>
                        <td>{row.accountName}</td>
                        <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{fmt(row.totalNetAmount)}</td>
                        <td style={{ textAlign: 'right' }}>{row.lineItemCount}</td>
                      </tr>
                    ))}
                    <tr style={{ fontWeight: 600, borderTop: '2px solid var(--color-border)' }}>
                      <td colSpan={2}>Total</td>
                      <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                        {fmt(report.byAccount.reduce((sum, r) => sum + Number(r.totalNetAmount), 0))}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {report.byAccount.reduce((sum, r) => sum + r.lineItemCount, 0)}
                      </td>
                    </tr>
                  </tbody>
                </table>
              )}
            </div>

            <div>
              <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>By Cost Center</h2>
              {report.byCostCenter.length === 0 ? (
                <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>No data for selected period.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Cost Center</th>
                      <th>Description</th>
                      <th style={{ textAlign: 'right' }}>Net Amount (€)</th>
                      <th style={{ textAlign: 'right' }}>Lines</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.byCostCenter.map(row => (
                      <tr key={row.compositeCode}>
                        <td><span className="code-badge">{row.compositeCode}</span></td>
                        <td>{row.description ?? <span className="muted">—</span>}</td>
                        <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{fmt(row.totalNetAmount)}</td>
                        <td style={{ textAlign: 'right' }}>{row.lineItemCount}</td>
                      </tr>
                    ))}
                    <tr style={{ fontWeight: 600, borderTop: '2px solid var(--color-border)' }}>
                      <td colSpan={2}>Total</td>
                      <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                        {fmt(report.byCostCenter.reduce((sum, r) => sum + Number(r.totalNetAmount), 0))}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {report.byCostCenter.reduce((sum, r) => sum + r.lineItemCount, 0)}
                      </td>
                    </tr>
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
