import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { previewInvoice } from '../../api/invoices'
import SimulationPreviewPanel from './components/SimulationPreviewPanel'
import '../masterdata/VatRatesPage.css'

export default function GenerateInvoicePage() {
  const navigate = useNavigate()
  const [customerId, setCustomerId] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [previewing, setPreviewing] = useState(false)
  const [report, setReport] = useState(null)
  const [error, setError] = useState(null)

  const blockingCount = report?.validationFailures?.filter(f => f.severity === 'BLOCKING').length ?? 0

  const handlePreview = async () => {
    if (!customerId || !dateFrom || !dateTo) { setError('Customer ID, from and to dates are required.'); return }
    setPreviewing(true); setError(null); setReport(null)
    try {
      const resp = await previewInvoice({
        customerId: parseInt(customerId),
        billingPeriodFrom: dateFrom,
        billingPeriodTo: dateTo,
      })
      setReport(resp.data)
    } catch (e) {
      setError(e?.response?.data || 'Preview failed.')
    } finally { setPreviewing(false) }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Generate Invoice</h1>
          <p>Preview then confirm to generate an invoice</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 'var(--space-4)' }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Customer ID *</label>
          <input type="number" value={customerId} onChange={e => setCustomerId(e.target.value)} style={{ width: 160 }} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Billing Period From *</label>
          <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Billing Period To *</label>
          <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} />
        </div>
        <button className="btn-secondary" onClick={handlePreview} disabled={previewing}>
          {previewing ? 'Previewing…' : 'Preview Invoice'}
        </button>
      </div>

      {report && (
        <>
          <SimulationPreviewPanel report={report} />

          <div style={{ marginTop: 'var(--space-4)' }}>
            {blockingCount > 0 && (
              <div className="error-msg">
                {blockingCount} blocking validation failure(s) — cannot generate invoice.
              </div>
            )}
            <button
              className="btn-primary"
              disabled={blockingCount > 0}
              onClick={() => alert('Confirm & Generate: connect to POST /api/v1/invoice-runs (step 38)')}
              style={{ marginTop: 'var(--space-3)' }}
            >
              Confirm & Generate
            </button>
          </div>
        </>
      )}
    </div>
  )
}
