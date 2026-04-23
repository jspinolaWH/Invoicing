import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRun, simulate, verifyAttachmentIdentifier } from '../../api/invoiceRuns'
import RunFilterForm from './components/RunFilterForm'
import '../masterdata/VatRatesPage.css'

const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/pdf/a',
  'image/jpeg',
  'image/png',
  'image/tiff',
]
const IDENTIFIER_PATTERN = /^[A-Za-z0-9\-_.]{1,100}$/

export default function InvoiceRunPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState({})
  const [splitByClassification, setSplitByClassification] = useState(false)
  const [mode, setMode] = useState('real') // 'real' or 'simulate'
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [batchAttachment, setBatchAttachment] = useState({
    attachmentIdentifier: '',
    filename: '',
    mimeType: 'application/pdf',
    securityClass: '',
  })
  const [attachmentErrors, setAttachmentErrors] = useState({})
  const [verifyState, setVerifyState] = useState(null) // null | 'checking' | 'valid' | 'invalid'

  const handleVerifyIdentifier = async () => {
    const identifier = batchAttachment.attachmentIdentifier.trim()
    if (!identifier) return
    setVerifyState('checking')
    try {
      const res = await verifyAttachmentIdentifier(identifier)
      setVerifyState(res.data?.valid ? 'valid' : 'invalid')
    } catch {
      setVerifyState('invalid')
    }
  }

  const validateAttachment = () => {
    const errors = {}
    const { attachmentIdentifier, mimeType } = batchAttachment
    if (attachmentIdentifier.trim()) {
      if (!IDENTIFIER_PATTERN.test(attachmentIdentifier.trim())) {
        errors.attachmentIdentifier = 'Identifier must be 1–100 alphanumeric characters, hyphens, underscores, or dots (e.g. FI-ATT-2026-001)'
      }
      if (mimeType.trim() && !ALLOWED_MIME_TYPES.includes(mimeType.trim())) {
        errors.mimeType = `MIME type must be one of: ${ALLOWED_MIME_TYPES.join(', ')}`
      }
    }
    setAttachmentErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async () => {
    if (mode === 'real' && batchAttachment.attachmentIdentifier.trim() && !validateAttachment()) return
    setLoading(true); setError(null)
    try {
      if (mode === 'simulate') {
        const res = await simulate(filter)
        navigate('/runs/simulation-results', { state: { report: res.data, filter } })
      } else {
        const buildPayload = (extraFilter = {}) => {
          const payload = { ...filter, ...extraFilter, simulationMode: false }
          if (batchAttachment.attachmentIdentifier.trim()) {
            payload.batchAttachment = {
              attachmentIdentifier: batchAttachment.attachmentIdentifier.trim(),
              filename: batchAttachment.filename.trim() || null,
              mimeType: batchAttachment.mimeType.trim() || 'application/pdf',
              securityClass: batchAttachment.securityClass.trim() || null,
            }
          }
          return payload
        }
        if (splitByClassification) {
          const [publicRes] = await Promise.all([
            createRun(buildPayload({ filterServiceResponsibility: 'PUBLIC_LAW' })),
          ])
          await createRun(buildPayload({ filterServiceResponsibility: 'PRIVATE_LAW' }))
          navigate(`/runs/${publicRes.data.id}`)
        } else {
          const res = await createRun(buildPayload())
          navigate(`/runs/${res.data.id}`)
        }
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

      <div style={{ marginTop: 'var(--space-4)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', background: 'var(--color-surface)' }}>
        <h3 style={{ margin: '0 0 var(--space-2) 0', fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>Legal Classification Splitting</h3>
        <p style={{ margin: '0 0 var(--space-3) 0', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          When enabled, this run automatically generates two separate invoice groups — one for PUBLIC_LAW events and one for PRIVATE_LAW events — without requiring two manual runs.
        </p>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: 'var(--font-size-sm)', fontWeight: 500 }}>
          <input
            type="checkbox"
            checked={splitByClassification}
            onChange={e => setSplitByClassification(e.target.checked)}
          />
          Split by legal classification (auto-separate PUBLIC_LAW and PRIVATE_LAW into separate runs)
        </label>
        {splitByClassification && (
          <p style={{ marginTop: 8, color: '#b45309', fontSize: 12 }}>
            Two runs will be created: one filtered to PUBLIC_LAW events and one to PRIVATE_LAW events. The Service Responsibility filter above will be overridden.
          </p>
        )}
      </div>

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
                onChange={e => {
                  setBatchAttachment(prev => ({ ...prev, attachmentIdentifier: e.target.value }))
                  setAttachmentErrors(prev => ({ ...prev, attachmentIdentifier: undefined }))
                  setVerifyState(null)
                }}
                style={attachmentErrors.attachmentIdentifier ? { borderColor: '#ef4444' } : {}}
              />
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                <button
                  type="button"
                  className="btn-secondary"
                  style={{ fontSize: 11, padding: '2px 10px' }}
                  disabled={!batchAttachment.attachmentIdentifier.trim() || verifyState === 'checking'}
                  onClick={handleVerifyIdentifier}
                >
                  {verifyState === 'checking' ? 'Checking…' : 'Verify Identifier'}
                </button>
                {verifyState === 'valid' && (
                  <span style={{ fontSize: 12, color: '#166534', fontWeight: 500 }}>Found in external service</span>
                )}
                {verifyState === 'invalid' && (
                  <span style={{ fontSize: 12, color: '#b91c1c', fontWeight: 500 }}>Not found in external service</span>
                )}
              </div>
              <div style={{ fontSize: 11, color: '#6b7280', marginTop: 2 }}>
                Alphanumeric, hyphens, underscores, dots (e.g. FI-ATT-2026-001)
              </div>
              {attachmentErrors.attachmentIdentifier && (
                <div style={{ color: '#ef4444', fontSize: 12, marginTop: 2 }}>{attachmentErrors.attachmentIdentifier}</div>
              )}
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
              <select
                className="form-input"
                value={batchAttachment.mimeType}
                onChange={e => {
                  setBatchAttachment(prev => ({ ...prev, mimeType: e.target.value }))
                  setAttachmentErrors(prev => ({ ...prev, mimeType: undefined }))
                }}
                style={attachmentErrors.mimeType ? { borderColor: '#ef4444' } : {}}
              >
                {ALLOWED_MIME_TYPES.map(m => <option key={m} value={m}>{m}</option>)}
              </select>
              {attachmentErrors.mimeType && (
                <div style={{ color: '#ef4444', fontSize: 12, marginTop: 2 }}>{attachmentErrors.mimeType}</div>
              )}
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
