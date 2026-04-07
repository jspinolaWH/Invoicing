import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { batchCredit } from '../../api/invoices'
import axios from 'axios'
import '../masterdata/VatRatesPage.css'

export default function InvoiceListPage() {
  const navigate = useNavigate()
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState([])
  const [showModal, setShowModal] = useState(false)
  const [batchComment, setBatchComment] = useState('')
  const [batchCustomText, setBatchCustomText] = useState('')
  const [batchResult, setBatchResult] = useState(null)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    axios.get('/api/v1/invoices?page=0&size=50')
      .then(r => setInvoices(r.data.content || r.data || []))
      .catch(() => setInvoices([]))
      .finally(() => setLoading(false))
  }, [])

  const creditableStatuses = ['SENT', 'COMPLETED']
  const toggleSelect = (id) => setSelected(prev =>
    prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
  )

  const handleBatchCredit = async () => {
    if (!batchComment.trim()) { setError('Internal comment is required'); return }
    setSubmitting(true); setError(null)
    try {
      const res = await batchCredit({ invoiceIds: selected, customText: batchCustomText, internalComment: batchComment })
      setBatchResult(res.data)
      setSelected([])
    } catch (e) {
      setError(e.response?.data?.message || 'Batch credit failed')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="loading">Loading invoices…</div>

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text"><h1>Invoices</h1></div>
      </div>

      {batchResult && (
        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: 'var(--space-3)', marginBottom: 'var(--space-3)' }}>
          Batch credit: <strong>{batchResult.succeeded}</strong> succeeded, <strong>{batchResult.failed}</strong> failed.
          {batchResult.failureReasons?.length > 0 && (
            <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
              {batchResult.failureReasons.map((r, i) => <li key={i} style={{ color: 'red', fontSize: 13 }}>{r}</li>)}
            </ul>
          )}
        </div>
      )}

      {selected.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-3)', padding: 'var(--space-2) var(--space-3)', background: 'var(--color-bg-surface)', border: '1px solid var(--color-border-subtle)', borderRadius: 'var(--radius-md)' }}>
          <span>{selected.length} selected</span>
          <button className="btn-danger" onClick={() => setShowModal(true)}>Batch Credit</button>
          <button className="btn-secondary" onClick={() => setSelected([])}>Clear</button>
        </div>
      )}

      <table className="table">
        <thead>
          <tr><th></th><th>Invoice #</th><th>Customer</th><th>Date</th><th>Net</th><th>Gross</th><th>Status</th></tr>
        </thead>
        <tbody>
          {invoices.map(inv => (
            <tr key={inv.id} style={{ cursor: 'pointer' }}>
              <td onClick={e => e.stopPropagation()}>
                <input type="checkbox"
                  disabled={!creditableStatuses.includes(inv.status)}
                  checked={selected.includes(inv.id)}
                  onChange={() => toggleSelect(inv.id)} />
              </td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}>{inv.invoiceNumber || `#${inv.id}`}</td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}>{inv.customerName || '—'}</td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}>{inv.invoiceDate || '—'}</td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}>{inv.netAmount != null ? Number(inv.netAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'}</td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}>{inv.grossAmount != null ? Number(inv.grossAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'}</td>
              <td onClick={() => navigate(`/invoices/${inv.id}`)}><span className="badge badge-grey">{inv.status}</span></td>
            </tr>
          ))}
        </tbody>
      </table>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Batch Credit — {selected.length} invoices</h3>
            <div className="form-group">
              <label>Customer-facing message (optional)</label>
              <textarea value={batchCustomText} onChange={e => setBatchCustomText(e.target.value)} rows={2} style={{ width: '100%' }} />
            </div>
            <div className="form-group">
              <label>Internal reason <span style={{ color: 'red' }}>*</span></label>
              <textarea value={batchComment} onChange={e => setBatchComment(e.target.value)} rows={3} style={{ width: '100%' }} placeholder="Required…" />
            </div>
            {error && <div className="error-msg">{error}</div>}
            <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end', marginTop: 'var(--space-3)' }}>
              <button className="btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn-danger" onClick={handleBatchCredit} disabled={submitting}>
                {submitting ? 'Processing…' : 'Issue Credits'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
