import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice } from '../../api/invoices'
import InvoiceHeader from './components/InvoiceHeader'
import InvoiceLineItemsTable from './components/InvoiceLineItemsTable'
import InvoiceAmountSummary from './components/InvoiceAmountSummary'
import InvoiceCustomTextPanel from './components/InvoiceCustomTextPanel'
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
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice {invoice.invoiceNumber || `#${invoice.id}`}</h1>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      <InvoiceHeader invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Line Items</h3>
      <InvoiceLineItemsTable lineItems={invoice.lineItems} />

      <InvoiceAmountSummary invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Custom Text</h3>
      <InvoiceCustomTextPanel invoice={invoice} onUpdated={setInvoice} />

      {invoice.attachments && invoice.attachments.length > 0 && (
        <>
          <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Attachments</h3>
          <table className="data-table">
            <thead><tr><th>Filename</th><th>Type</th><th>Size</th><th>Identifier</th></tr></thead>
            <tbody>
              {invoice.attachments.map(a => (
                <tr key={a.id}>
                  <td>{a.filename}</td>
                  <td><code>{a.mimeType}</code></td>
                  <td>{(a.sizeBytes / 1024).toFixed(1)} KB</td>
                  <td><code>{a.attachmentIdentifier}</code></td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
