import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice, issueCreditNote } from '../../api/invoices'
import { useEffect } from 'react'
import '../masterdata/VatRatesPage.css'

export default function CreditNoteForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [invoice, setInvoice] = useState(null)
  const [creditType, setCreditType] = useState('FULL')
  const [selectedLines, setSelectedLines] = useState([])
  const [customText, setCustomText] = useState('')
  const [internalComment, setInternalComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    getInvoice(id).then(r => setInvoice(r.data)).catch(() => setError('Invoice not found'))
  }, [id])

  const toggleLine = (lineId) => {
    setSelectedLines(prev =>
      prev.includes(lineId) ? prev.filter(x => x !== lineId) : [...prev, lineId]
    )
  }

  const handleSubmit = async () => {
    if (!internalComment.trim()) { setError('Internal comment is required'); return }
    if (creditType === 'PARTIAL' && selectedLines.length === 0) {
      setError('Select at least one line item for partial credit'); return
    }
    setSubmitting(true); setError(null)
    try {
      const data = {
        creditType,
        lineItemIds: creditType === 'PARTIAL' ? selectedLines : undefined,
        customText: customText || undefined,
        internalComment,
      }
      const res = await issueCreditNote(id, data)
      navigate(`/invoices/${res.data.creditNoteId}`)
    } catch (e) {
      setError(e.response?.data?.message || e.response?.data?.error || 'Failed to issue credit note')
    } finally {
      setSubmitting(false)
    }
  }

  if (!invoice) return <div className="loading">Loading…</div>

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Issue Credit Note</h1>
          <p style={{ margin: 0, color: 'var(--color-text-secondary)' }}>
            for Invoice {invoice.invoiceNumber || `#${invoice.id}`}
          </p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <label style={{ marginRight: 'var(--space-3)', fontWeight: 600 }}>
          <input type="radio" value="FULL" checked={creditType === 'FULL'}
            onChange={() => setCreditType('FULL')} style={{ marginRight: 4 }} />
          Full Credit
        </label>
        <label style={{ fontWeight: 600 }}>
          <input type="radio" value="PARTIAL" checked={creditType === 'PARTIAL'}
            onChange={() => setCreditType('PARTIAL')} style={{ marginRight: 4 }} />
          Partial Credit
        </label>
      </div>

      {creditType === 'PARTIAL' && (
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <h4>Select Line Items to Credit</h4>
          <table className="table">
            <thead><tr><th></th><th>Description</th><th>Qty</th><th>Net</th><th>Gross</th></tr></thead>
            <tbody>
              {(invoice.lineItems || []).map(li => (
                <tr key={li.id}>
                  <td><input type="checkbox" checked={selectedLines.includes(li.id)}
                    onChange={() => toggleLine(li.id)} /></td>
                  <td>{li.description}</td>
                  <td>{li.quantity}</td>
                  <td>{li.netAmount}</td>
                  <td>{li.grossAmount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="form-group">
        <label>Customer-facing message (optional)</label>
        <textarea value={customText} onChange={e => setCustomText(e.target.value)}
          rows={3} style={{ width: '100%' }} placeholder="Visible on the credit note…" />
      </div>

      <div className="form-group">
        <label>Internal reason <span style={{ color: 'red' }}>*</span></label>
        <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>
          This will NOT be sent to the customer. It is for internal records only.
        </div>
        <textarea value={internalComment} onChange={e => setInternalComment(e.target.value)}
          rows={3} style={{ width: '100%', borderColor: !internalComment.trim() && error ? 'red' : undefined }}
          placeholder="Required — reason for credit note…" />
      </div>

      <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 8, padding: 'var(--space-3)', marginBottom: 'var(--space-3)', display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <span style={{ color: '#1d4ed8', fontSize: 18, lineHeight: 1 }}>ⓘ</span>
        <span style={{ color: '#1e40af', fontSize: 13 }}>
          The credit note will be assigned the same number as this invoice ({invoice.invoiceNumber || `#${invoice.id}`}) per the credit note numbering policy.
        </span>
      </div>

      {error && <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>{error}</div>}

      <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
        {submitting ? 'Issuing…' : 'Issue Credit Note'}
      </button>
    </div>
  )
}
