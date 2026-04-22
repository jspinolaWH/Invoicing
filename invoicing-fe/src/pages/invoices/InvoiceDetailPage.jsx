import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice, removeSurcharge } from '../../api/invoices'
import { getReportingAuditForInvoice } from '../../api/auditLogs'
import InvoiceHeader from './components/InvoiceHeader'
import InvoiceLineItemsTable from './components/InvoiceLineItemsTable'
import InvoiceAmountSummary from './components/InvoiceAmountSummary'
import InvoiceCustomTextPanel from './components/InvoiceCustomTextPanel'
import InvoiceAttachmentsPanel from './components/InvoiceAttachmentsPanel'
import InvoiceTransmitPanel from '../integration/InvoiceTransmitPanel'
import PaymentRemindersPanel from './PaymentRemindersPanel'
import '../masterdata/VatRatesPage.css'

export default function InvoiceDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [invoice, setInvoice] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [removingSurcharge, setRemovingSurcharge] = useState(false)
  const [reportingAudit, setReportingAudit] = useState([])
  const [reportingAuditLoading, setReportingAuditLoading] = useState(false)

  const hasSurchargeLines = () =>
    invoice?.lineItems?.some(li => li.description?.startsWith('Invoicing surcharge'))

  const handleRemoveSurcharge = async () => {
    if (!window.confirm('Remove all surcharge line items from this invoice?')) return
    setRemovingSurcharge(true)
    try {
      const res = await removeSurcharge(invoice.id)
      setInvoice(res.data)
    } catch {
      setError('Failed to remove surcharge.')
    } finally {
      setRemovingSurcharge(false)
    }
  }

  const load = async () => {
    setLoading(true); setError(null)
    try { setInvoice((await getInvoice(id)).data) }
    catch { setError('Invoice not found.') }
    finally { setLoading(false) }
  }

  const loadReportingAudit = async () => {
    setReportingAuditLoading(true)
    try {
      const res = await getReportingAuditForInvoice(id)
      setReportingAudit(res.data ?? [])
    } catch {
      setReportingAudit([])
    } finally {
      setReportingAuditLoading(false)
    }
  }

  useEffect(() => { load() }, [id])
  useEffect(() => { loadReportingAudit() }, [id])

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
          {hasSurchargeLines() && (
            <button className="btn-danger" onClick={handleRemoveSurcharge} disabled={removingSurcharge}>
              {removingSurcharge ? 'Removing…' : 'Remove Surcharge'}
            </button>
          )}
          <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
        </div>
      </div>

      <InvoiceHeader invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Line Items</h3>
      <InvoiceLineItemsTable lineItems={invoice.lineItems} invoicingMode={invoice.invoicingMode} />

      <InvoiceAmountSummary invoice={invoice} />

      <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Custom Text</h3>
      <InvoiceCustomTextPanel invoice={invoice} onUpdated={setInvoice} />

      {invoice.internalComment && (
        <div style={{ marginTop: 'var(--space-5)', background: '#f9fafb', border: '1px solid #e5e7eb', borderLeft: '4px solid #6b7280', borderRadius: 8, padding: 'var(--space-3)' }}>
          <h3 style={{ margin: '0 0 var(--space-2)', fontSize: 14, color: '#374151' }}>
            Internal Comment <span style={{ fontWeight: 400, fontSize: 12, color: '#6b7280' }}>(not sent to customer)</span>
          </h3>
          <p style={{ margin: 0, whiteSpace: 'pre-wrap', color: '#374151', fontSize: 14 }}>{invoice.internalComment}</p>
        </div>
      )}

      <div style={{ marginTop: 'var(--space-5)' }}>
        <InvoiceAttachmentsPanel invoiceId={invoice.id} invoiceNumber={invoice.invoiceNumber} />
      </div>

      <InvoiceTransmitPanel
        invoiceId={invoice.id}
        invoiceStatus={invoice.status}
        externalReference={invoice.externalReference}
        allowExternalRecall={invoice.allowExternalRecall}
        onStatusChange={(newStatus) => setInvoice(prev => ({ ...prev, status: newStatus }))}
      />

      <PaymentRemindersPanel invoiceId={invoice.id} invoiceStatus={invoice.status} />

      <div style={{ marginTop: 'var(--space-5)' }}>
        <h3 style={{ marginBottom: 'var(--space-3)' }}>Reporting Data Audit Trail</h3>
        {reportingAuditLoading ? (
          <div className="loading">Loading audit trail…</div>
        ) : reportingAudit.length === 0 ? (
          <p style={{ color: '#6b7280', fontSize: 13 }}>No reporting data audit entries for this invoice.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Line Item</th>
                <th>Field</th>
                <th>Assigned Value</th>
                <th>Logged At</th>
                <th>Logged By</th>
              </tr>
            </thead>
            <tbody>
              {reportingAudit.map(entry => (
                <tr key={entry.id}>
                  <td>{entry.lineItemId ?? '—'}</td>
                  <td><code>{entry.field}</code></td>
                  <td>{entry.assignedValue ?? '—'}</td>
                  <td>{entry.loggedAt ? new Date(entry.loggedAt).toLocaleString() : '—'}</td>
                  <td>{entry.loggedBy ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
