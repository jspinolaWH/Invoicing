import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice, correctInvoice } from '../../api/invoices'
import '../masterdata/VatRatesPage.css'

export default function CorrectInvoiceForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [invoice, setInvoice] = useState(null)
  const [sameCustomer, setSameCustomer] = useState(true)
  const [targetCustomerId, setTargetCustomerId] = useState('')
  const [allLines, setAllLines] = useState(true)
  const [selectedLines, setSelectedLines] = useState([])
  const [customText, setCustomText] = useState('')
  const [internalComment, setInternalComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)

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
    setSubmitting(true); setError(null)
    try {
      const data = {
        targetCustomerId: sameCustomer ? null : (targetCustomerId ? Number(targetCustomerId) : null),
        lineItemIds: allLines ? null : selectedLines,
        customText: customText || undefined,
        internalComment,
      }
      const res = await correctInvoice(id, data)
      setResult(res.data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to apply correction')
    } finally {
      setSubmitting(false)
    }
  }

  if (!invoice) return <div className="loading">Loading…</div>

  if (result) {
    return (
      <div className="page">
        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
          <strong style={{ color: '#166534' }}>Credit note issued.</strong>{' '}
          {result.message}
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <button className="btn-primary" onClick={() => navigate(`/invoices/${result.creditNoteId}`)}>
            View Credit Note
          </button>
          {result.copiedEventIds?.length > 0 && (
            <button className="btn-secondary" onClick={() => navigate('/billing-events')}>
              View Copied Events
            </button>
          )}
          <button className="btn-secondary" onClick={() => navigate(-1)}>Back to Invoice</button>
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Correct Invoice</h1>
          <p style={{ margin: 0, color: 'var(--color-text-secondary)' }}>
            Invoice {invoice.invoiceNumber || `#${invoice.id}`}
          </p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <h4>Target Customer</h4>
        <label style={{ marginRight: 'var(--space-3)' }}>
          <input type="radio" checked={sameCustomer} onChange={() => setSameCustomer(true)} style={{ marginRight: 4 }} />
          Same customer ({invoice.customerName || invoice.customerId})
        </label>
        <label>
          <input type="radio" checked={!sameCustomer} onChange={() => setSameCustomer(false)} style={{ marginRight: 4 }} />
          Different customer
        </label>
        {!sameCustomer && (
          <div className="form-group" style={{ marginTop: 'var(--space-2)' }}>
            <label>Target Customer ID</label>
            <input type="number" value={targetCustomerId} onChange={e => setTargetCustomerId(e.target.value)} placeholder="Customer ID" />
          </div>
        )}
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <h4>Lines to Correct</h4>
        <label style={{ marginRight: 'var(--space-3)' }}>
          <input type="radio" checked={allLines} onChange={() => setAllLines(true)} style={{ marginRight: 4 }} />
          All lines
        </label>
        <label>
          <input type="radio" checked={!allLines} onChange={() => setAllLines(false)} style={{ marginRight: 4 }} />
          Select specific lines
        </label>
        {!allLines && (
          <table className="table" style={{ marginTop: 'var(--space-2)' }}>
            <thead><tr><th></th><th>Description</th><th>Net</th></tr></thead>
            <tbody>
              {(invoice.lineItems || []).map(li => (
                <tr key={li.id}>
                  <td><input type="checkbox" checked={selectedLines.includes(li.id)} onChange={() => toggleLine(li.id)} /></td>
                  <td>{li.description}</td>
                  <td>{li.netAmount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="form-group">
        <label>Customer-facing message (optional)</label>
        <textarea value={customText} onChange={e => setCustomText(e.target.value)} rows={2} style={{ width: '100%' }} />
      </div>

      <div className="form-group">
        <label>Internal reason <span style={{ color: 'red' }}>*</span></label>
        <textarea value={internalComment} onChange={e => setInternalComment(e.target.value)}
          rows={3} style={{ width: '100%' }} placeholder="Required — reason for correction…" />
      </div>

      {error && <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>{error}</div>}

      <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
        {submitting ? 'Processing…' : 'Issue Credit & Copy Events'}
      </button>
    </div>
  )
}
