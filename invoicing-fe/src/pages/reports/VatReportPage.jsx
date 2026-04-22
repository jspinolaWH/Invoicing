import { useState } from 'react'
import { fetchVatReport } from '../../api/vatReport'
import '../masterdata/VatRatesPage.css'

export default function VatReportPage() {
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
      const data = await fetchVatReport({ from: from || undefined, to: to || undefined })
      setReport(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 style={{ margin: 0 }}>VAT Report</h1>
          <p style={{ color: 'var(--color-text-secondary)', marginTop: 4 }}>
            Standard VAT totals and reverse charge VAT base for a given date range.
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
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
          <div style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '20px 24px' }}>
            <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>
              Standard VAT Total
            </div>
            <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--color-text-primary)' }}>
              {Number(report.standardVatTotal).toLocaleString('fi-FI', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €
            </div>
            <div style={{ fontSize: 12, color: 'var(--color-text-tertiary, var(--color-text-secondary))', marginTop: 4 }}>
              VAT collected on standard invoices
            </div>
          </div>

          <div style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '20px 24px' }}>
            <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>
              Reverse Charge Base Total
            </div>
            <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--color-text-primary)' }}>
              {Number(report.reverseChargeBaseTotal).toLocaleString('fi-FI', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €
            </div>
            <div style={{ fontSize: 12, color: 'var(--color-text-tertiary, var(--color-text-secondary))', marginTop: 4 }}>
              Net base subject to reverse charge VAT
            </div>
          </div>

          <div style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '20px 24px' }}>
            <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>
              Reverse Charge Invoices
            </div>
            <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--color-text-primary)' }}>
              {report.reverseChargeInvoiceCount}
            </div>
            <div style={{ fontSize: 12, color: 'var(--color-text-tertiary, var(--color-text-secondary))', marginTop: 4 }}>
              Invoices with reverse charge VAT applied
            </div>
          </div>
        </div>
      )}

      {report && (
        <div style={{ marginTop: 12, fontSize: 12, color: 'var(--color-text-secondary)' }}>
          Period: {report.from ?? 'all'} — {report.to ?? 'all'}
        </div>
      )}
    </div>
  )
}
