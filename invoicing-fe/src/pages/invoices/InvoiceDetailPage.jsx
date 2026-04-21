import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice } from '../../api/invoices'
import InvoiceHeader from './components/InvoiceHeader'
import InvoiceLineItemsTable from './components/InvoiceLineItemsTable'
import InvoiceAmountSummary from './components/InvoiceAmountSummary'
import InvoiceCustomTextPanel from './components/InvoiceCustomTextPanel'
import InvoiceAttachmentsPanel from './components/InvoiceAttachmentsPanel'
import InvoiceTransmitPanel from '../integration/InvoiceTransmitPanel'
import '../masterdata/VatRatesPage.css'

export default function InvoiceDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [invoice, setInvoice] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true); setError(null)
    try { setInvoice((await getInvoice(id)).data) }
    catch { setError('Invoice not found.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [id])

  if (loading) return <div className="loading">Loading invoice...</div>
  if (error) return <div className="error-msg">{error}</div>
  if (!invoice) return null

  return (
    <div className="page">
      {invoice.creditNotes?.length > 0 && (
        <div style={{ background: '#fefce8', border: '1px solid #fcd34d', borderRadius: 8, padding: 'var(--space-3)', marginBottom: 'var(--space-4)' }}>
          <strong style={{ color: '#92400e' }}>This invoice has been (partially) credited.</strong>
          <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
            {invoice.creditNotes.map(cn => (
              <li key={cn.creditNoteId}>
                <a
                  href={`/invoices/${cn.creditNoteId}`}
                  onClick={e => { e.preventDefault(); navigate(`/invoices/${cn.creditNoteId}`) }}
                  style={{ color: 'var(--color-brand-primary)', textDecoration: 'underline', cursor: 'pointer' }}
                >
                  {cn.creditNoteNumber || `Credit Note #${cn.creditNoteId}`}
                </a>
                {cn.issuedAt ? ` — issued ${cn.issuedAt}` : ''}
                {cn.netAmount != null ? ` · net ${cn.netAmount}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice {invoice.invoiceNumber || `#${invoice.id}`}</h1>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
          {(invoice.status === 'SENT' || invoice.status === 'COMPLETED') && (
            <>
              <button className="btn-secondary" onClick={() => navigate(`/invoices/${invoice.id}/credit`)}>
                Issue Credit Note
              </button>
              <button className="btn-secondary" onClick={() => navigate(`/invoices/${invoice.id}/correct`)}>
                Correct Invoice
              </button>
            </>
          )}
          <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
        </div>
      </div>

      <InvoiceHeader invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Line Items</h3>
      <InvoiceLineItemsTable lineItems={invoice.lineItems} />

      <InvoiceAmountSummary invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Custom Text</h3>
      <InvoiceCustomTextPanel invoice={invoice} onUpdated={setInvoice} />

      <div style={{ marginTop: 'var(--space-5)' }}>
        <InvoiceAttachmentsPanel invoiceId={invoice.id} invoiceNumber={invoice.invoiceNumber} />
      </div>

      <InvoiceTransmitPanel
        invoiceId={invoice.id}
        invoiceStatus={invoice.status}
        externalReference={invoice.externalReference}
        onStatusChange={(newStatus) => setInvoice(prev => ({ ...prev, status: newStatus }))}
      />
    </div>
  )
}
