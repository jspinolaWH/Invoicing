import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice, removeSurcharge, getFinvoiceXml } from '../../api/invoices'
import { getReportingAuditForInvoice } from '../../api/auditLogs'
import { listAttachments } from '../../api/invoiceAttachments'
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
  const [attachmentAudit, setAttachmentAudit] = useState([])
  const [attachmentAuditLoading, setAttachmentAuditLoading] = useState(false)
  const [finvoiceXml, setFinvoiceXml] = useState(null)
  const [finvoiceLoading, setFinvoiceLoading] = useState(false)
  const [finvoiceError, setFinvoiceError] = useState(null)
  const [showFinvoice, setShowFinvoice] = useState(false)

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

  const loadAttachmentAudit = async () => {
    setAttachmentAuditLoading(true)
    try {
      const res = await listAttachments(id)
      setAttachmentAudit((res.data ?? []).filter(a => a.attachmentIdentifier))
    } catch {
      setAttachmentAudit([])
    } finally {
      setAttachmentAuditLoading(false)
    }
  }

  const loadFinvoiceXml = async () => {
    setFinvoiceLoading(true)
    setFinvoiceError(null)
    try {
      const res = await getFinvoiceXml(id)
      setFinvoiceXml(res.data)
    } catch {
      setFinvoiceError('Could not load FINVOICE XML.')
    } finally {
      setFinvoiceLoading(false)
    }
  }

  const handleToggleFinvoice = () => {
    if (!showFinvoice && finvoiceXml === null && !finvoiceLoading) {
      loadFinvoiceXml()
    }
    setShowFinvoice(v => !v)
  }

  const handleDownloadFinvoice = () => {
    if (!finvoiceXml) return
    const blob = new Blob([finvoiceXml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `finvoice-${id}.xml`
    a.click()
    URL.revokeObjectURL(url)
  }

  useEffect(() => { load() }, [id])
  useEffect(() => { loadReportingAudit() }, [id])
  useEffect(() => { loadAttachmentAudit() }, [id])

  if (loading) return <div className="loading">Loading invoice...</div>
  if (error) return <div className="error-msg">{error}</div>
  if (!invoice) return null

  return (
    <div className="page">
      {invoice.invoiceType === 'CREDIT_NOTE' && invoice.originalInvoiceId && (
        <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 8, padding: 'var(--space-3)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ color: '#1d4ed8', fontSize: 20 }}>ⓘ</span>
          <span style={{ color: '#1e40af', fontSize: 13 }}>
            This credit note carries the same number as the original invoice (
            <a
              href={`/invoices/${invoice.originalInvoiceId}`}
              onClick={e => { e.preventDefault(); navigate(`/invoices/${invoice.originalInvoiceId}`) }}
              style={{ color: '#1d4ed8', textDecoration: 'underline', cursor: 'pointer' }}
            >
              view original invoice
            </a>
            ) per the credit note numbering policy.
          </span>
        </div>
      )}
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
          <h1>
            {invoice.invoiceType === 'CREDIT_NOTE' ? 'Credit Note' : 'Invoice'}{' '}
            {invoice.invoiceNumber || `#${invoice.id}`}
            {invoice.invoiceType === 'CREDIT_NOTE' && (
              <span
                title="Credit note numbers are assigned from the original invoice number per numbering policy."
                style={{ marginLeft: 8, fontSize: 13, fontWeight: 400, color: '#6b7280', cursor: 'default', verticalAlign: 'middle' }}
              >
                ⓘ Number matches credited invoice
              </span>
            )}
          </h1>
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

      <div style={{ marginTop: 'var(--space-5)', border: '1px solid #e5e7eb', borderRadius: 8, padding: 'var(--space-3)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: showFinvoice ? 'var(--space-3)' : 0 }}>
          <h3 style={{ margin: 0 }}>FINVOICE 3.0 Compliance</h3>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            {finvoiceXml && (
              <button className="btn-secondary" onClick={handleDownloadFinvoice}>
                Download XML
              </button>
            )}
            <button className="btn-secondary" onClick={handleToggleFinvoice}>
              {showFinvoice ? 'Hide' : 'Preview'} FINVOICE XML
            </button>
          </div>
        </div>
        {showFinvoice && (
          <div style={{ marginTop: 'var(--space-3)' }}>
            {finvoiceLoading && <div className="loading">Loading FINVOICE XML...</div>}
            {finvoiceError && <div className="error-msg">{finvoiceError}</div>}
            {finvoiceXml && !finvoiceLoading && (
              <>
                <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 6, padding: 'var(--space-2)', marginBottom: 'var(--space-2)', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ color: '#15803d', fontWeight: 600, fontSize: 13 }}>FINVOICE 3.0 compliant</span>
                  <span style={{ color: '#6b7280', fontSize: 12 }}>This invoice was serialised according to the FINVOICE 3.0 standard.</span>
                </div>
                <pre style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 6, padding: 'var(--space-3)', fontSize: 11, lineHeight: 1.5, overflowX: 'auto', maxHeight: 400, margin: 0 }}>
                  {finvoiceXml}
                </pre>
              </>
            )}
          </div>
        )}
      </div>

      <div style={{ marginTop: 'var(--space-5)' }}>
        <h3 style={{ marginBottom: 'var(--space-3)' }}>Attachment Association Audit Trail</h3>
        {attachmentAuditLoading ? (
          <div className="loading">Loading attachment audit…</div>
        ) : attachmentAudit.length === 0 ? (
          <p style={{ color: '#6b7280', fontSize: 13 }}>No batch attachment associations recorded for this invoice.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>AttachmentIdentifier</th>
                <th>Filename</th>
                <th>MIME Type</th>
                <th>Security Class</th>
                <th>Linked At</th>
              </tr>
            </thead>
            <tbody>
              {attachmentAudit.map(a => (
                <tr key={a.id}>
                  <td><code style={{ fontFamily: 'monospace', fontSize: 12 }}>{a.attachmentIdentifier}</code></td>
                  <td>{a.filename}</td>
                  <td>{a.mimeType || '—'}</td>
                  <td>{a.securityClass || '—'}</td>
                  <td>{a.createdAt ? new Date(a.createdAt).toLocaleString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

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
