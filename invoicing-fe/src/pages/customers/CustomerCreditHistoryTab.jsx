import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCreditHistory } from '../../api/invoices'

export default function CustomerCreditHistoryTab({ customerId }) {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!customerId) return
    setLoading(true)
    getCreditHistory(customerId, page)
      .then(r => setData(r.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [customerId, page])

  if (loading) return <div className="loading">Loading credit history…</div>
  if (!data || !data.content || data.content.length === 0) return <p style={{ color: 'var(--color-text-secondary)' }}>No credit notes.</p>

  const fmt = (n) => n != null ? Number(n).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'

  return (
    <div>
      <table className="table">
        <thead>
          <tr>
            <th>Credit Note #</th>
            <th>Date</th>
            <th>Original Invoice #</th>
            <th>Net</th>
            <th>Gross</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {data.content.map(inv => (
            <tr key={inv.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/invoices/${inv.id}`)}>
              <td>{inv.invoiceNumber || `#${inv.id}`}</td>
              <td>{inv.invoiceDate || '—'}</td>
              <td>{inv.originalInvoiceId ? `#${inv.originalInvoiceId}` : '—'}</td>
              <td style={{ color: 'red' }}>{fmt(inv.netAmount)}</td>
              <td style={{ color: 'red' }}>{fmt(inv.grossAmount)}</td>
              <td><span className="badge badge-grey">{inv.status}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
      {data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 'var(--space-2)', marginTop: 'var(--space-3)' }}>
          <button className="btn-secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span style={{ lineHeight: '32px', fontSize: 13, color: 'var(--color-text-secondary)' }}>
            Page {page + 1} / {data.totalPages}
          </span>
          <button className="btn-secondary" disabled={page >= data.totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}
    </div>
  )
}
