import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRun, simulate } from '../../api/invoiceRuns'
import RunFilterForm from './components/RunFilterForm'
import '../masterdata/VatRatesPage.css'

export default function InvoiceRunPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState({})
  const [mode, setMode] = useState('real') // 'real' or 'simulate'
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [batchAttachment, setBatchAttachment] = useState({
    attachmentIdentifier: '',
    filename: '',
    mimeType: 'application/pdf',
    securityClass: '',
  })

  const handleSubmit = async () => {
    setLoading(true); setError(null)
    try {
      if (mode === 'simulate') {
        const res = await simulate(filter)
        navigate('/runs/simulation-results', { state: { report: res.data, filter } })
      } else {
        const payload = { ...filter, simulationMode: false }
        if (batchAttachment.attachmentIdentifier.trim()) {
          payload.batchAttachment = {
            attachmentIdentifier: batchAttachment.attachmentIdentifier.trim(),
            filename: batchAttachment.filename.trim() || null,
            mimeType: batchAttachment.mimeType.trim() || 'application/pdf',
            securityClass: batchAttachment.securityClass.trim() || null,
          }
        }
        const res = await createRun(payload)
        navigate(`/runs/${res.data.id}`)
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to start run')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Run</h1>
          <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>Configure and start an invoice run or simulation.</p>
        </div>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <label style={{ marginRight: 'var(--space-3)', fontWeight: 600 }}>
          <input type="radio" value="real" checked={mode === 'real'} onChange={() => setMode('real')} style={{ marginRight: 4 }} />
          Real Run
        </label>
        <label style={{ fontWeight: 600 }}>
          <input type="radio" value="simulate" checked={mode === 'simulate'} onChange={() => setMode('simulate')} style={{ marginRight: 4 }} />
          Simulate First
        </label>
      </div>

      <RunFilterForm values={filter} onChange={setFilter} />

      {mode === 'real' && (
        <div style={{ marginTop: 'var(--space-4)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', background: 'var(--color-surface)' }}>
          <h3 style={{ margin: '0 0 var(--space-3) 0', fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>Batch PDF Attachment (optional)</h3>
          <p style={{ margin: '0 0 var(--space-3) 0', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            Attach a single PDF document to every invoice in this batch via the external invoicing broker identifier.
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)' }}>
            <div>
              <label className="form-label">Attachment Identifier</label>
              <input
                className="form-input"
                placeholder="e.g. FI-ATT-2026-001"
                value={batchAttachment.attachmentIdentifier}
                onChange={e => setBatchAttachment(prev => ({ ...prev, attachmentIdentifier: e.target.value }))}
              />
            </div>
            <div>
              <label className="form-label">Filename</label>
              <input
                className="form-input"
                placeholder="e.g. waste-terms-2026.pdf"
                value={batchAttachment.filename}
                onChange={e => setBatchAttachment(prev => ({ ...prev, filename: e.target.value }))}
              />
            </div>
            <div>
              <label className="form-label">MIME Type</label>
              <input
                className="form-input"
                placeholder="application/pdf"
                value={batchAttachment.mimeType}
                onChange={e => setBatchAttachment(prev => ({ ...prev, mimeType: e.target.value }))}
              />
            </div>
            <div>
              <label className="form-label">Security Class</label>
              <input
                className="form-input"
                placeholder="e.g. PUBLIC"
                value={batchAttachment.securityClass}
                onChange={e => setBatchAttachment(prev => ({ ...prev, securityClass: e.target.value }))}
              />
            </div>
          </div>
        </div>
      )}

      {error && <div className="error-msg" style={{ marginTop: 'var(--space-3)' }}>{error}</div>}

      <div style={{ marginTop: 'var(--space-4)' }}>
        <button className="btn-primary" onClick={handleSubmit} disabled={loading}>
          {loading ? 'Processing…' : mode === 'simulate' ? 'Run Simulation' : 'Start Real Run'}
        </button>
      </div>
    </div>
  )
}
