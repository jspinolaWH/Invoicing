import { useEffect, useState } from 'react'
import { listAttachments, getFinvoiceXml } from '../../../api/invoiceAttachments'
import AttachmentsList from './AttachmentsList'
import AttachmentUploadZone from './AttachmentUploadZone'

const ALLOWED_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const MAX_TOTAL_BYTES = 1_048_576
const MAX_COUNT = 10

function AttachmentComplianceBanner({ attachments }) {
  if (!attachments.length) return null

  const violations = []
  const totalBytes = attachments.reduce((s, a) => s + (a.sizeBytes || 0), 0)

  if (attachments.length > MAX_COUNT)
    violations.push(`Exceeds maximum of ${MAX_COUNT} attachments (currently ${attachments.length})`)
  if (totalBytes > MAX_TOTAL_BYTES)
    violations.push(`Total size exceeds 1 MB limit (${(totalBytes / 1024).toFixed(0)} KB used)`)
  attachments.forEach(a => {
    if (a.mimeType && !ALLOWED_MIME_TYPES.includes(a.mimeType))
      violations.push(`"${a.filename}" has unsupported type ${a.mimeType}`)
    if (!a.attachmentIdentifier)
      violations.push(`"${a.filename}" is missing SHA1 checksum — re-upload required`)
  })

  const passes = violations.length === 0

  return (
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      gap: 8,
      padding: '8px 12px',
      borderRadius: 6,
      marginBottom: 'var(--space-3)',
      background: passes ? '#f0fdf4' : '#fff7ed',
      border: `1px solid ${passes ? '#bbf7d0' : '#fed7aa'}`,
      fontSize: 13,
    }}>
      <span style={{ fontSize: 16, lineHeight: 1.4 }}>{passes ? '✓' : '⚠'}</span>
      <div>
        <span style={{ fontWeight: 600, color: passes ? '#166534' : '#92400e' }}>
          {passes
            ? 'All attachments are FINVOICE-compliant and ready for transmission'
            : `${violations.length} compliance issue${violations.length > 1 ? 's' : ''} — resolve before transmitting`}
        </span>
        {!passes && (
          <ul style={{ margin: '4px 0 0 0', paddingLeft: 16, color: '#92400e' }}>
            {violations.map((v, i) => <li key={i}>{v}</li>)}
          </ul>
        )}
      </div>
    </div>
  )
}

function BatchAttachmentDetailsSummary({ attachments }) {
  const batchAttachments = attachments.filter(a => a.attachmentIdentifier)
  if (!batchAttachments.length) return null

  return (
    <div style={{ marginBottom: 'var(--space-3)', padding: '10px 14px', background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 6 }}>
      <div style={{ fontWeight: 600, fontSize: 12, color: '#0369a1', marginBottom: 6 }}>
        FINVOICE AttachmentDetails — {batchAttachments.length} batch-linked attachment{batchAttachments.length > 1 ? 's' : ''}
      </div>
      {batchAttachments.map(a => (
        <div key={a.id} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 6, fontSize: 12, color: '#0c4a6e', marginBottom: batchAttachments.length > 1 ? 8 : 0 }}>
          <div><span style={{ color: '#0369a1' }}>AttachmentIdentifier: </span><code style={{ fontFamily: 'monospace', fontSize: 11 }}>{a.attachmentIdentifier}</code></div>
          <div><span style={{ color: '#0369a1' }}>AttachmentName: </span>{a.filename}</div>
          <div><span style={{ color: '#0369a1' }}>AttachmentMimeType: </span>{a.mimeType || '—'}</div>
          {a.securityClass && <div><span style={{ color: '#0369a1' }}>AttachmentSecurityClass: </span>{a.securityClass}</div>}
          {a.createdAt && <div><span style={{ color: '#0369a1' }}>Linked at: </span>{new Date(a.createdAt).toLocaleString()}</div>}
        </div>
      ))}
    </div>
  )
}

function InlineErrorBanner({ message, onDismiss }) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      gap: 8,
      padding: '8px 12px',
      borderRadius: 6,
      marginBottom: 'var(--space-3)',
      background: '#fef2f2',
      border: '1px solid #fecaca',
      fontSize: 13,
    }}>
      <span style={{ fontSize: 16, lineHeight: 1.4, color: '#991b1b' }}>✕</span>
      <div style={{ flex: 1, color: '#991b1b', fontWeight: 600 }}>{message}</div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#991b1b', fontSize: 16, lineHeight: 1, padding: 0 }}
          aria-label="Dismiss"
        >×</button>
      )}
    </div>
  )
}

export default function InvoiceAttachmentsPanel({ invoiceId, invoiceNumber }) {
  const [attachments, setAttachments] = useState([])
  const [loading, setLoading] = useState(false)
  const [xmlError, setXmlError] = useState(null)

  const load = async () => {
    setLoading(true)
    try { setAttachments((await listAttachments(invoiceId)).data) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [invoiceId])

  const handleViewXml = async () => {
    setXmlError(null)
    try {
      const resp = await getFinvoiceXml(invoiceId)
      const blob = new Blob([resp.data], { type: 'application/xml' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `invoice-${invoiceNumber || invoiceId}.xml`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setXmlError('FINVOICE XML is not available for this invoice. The invoice may not have been transmitted yet or the external system is unreachable.')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-3)' }}>
        <h3 style={{ margin: 0 }}>Attachments</h3>
        <button className="btn-secondary" onClick={handleViewXml} style={{ fontSize: 12 }}>
          ↓ FINVOICE XML
        </button>
      </div>

      {xmlError && <InlineErrorBanner message={xmlError} onDismiss={() => setXmlError(null)} />}

      {loading ? <div className="loading">Loading…</div> : (
        <>
          <BatchAttachmentDetailsSummary attachments={attachments} />
          <AttachmentComplianceBanner attachments={attachments} />
          <AttachmentsList invoiceId={invoiceId} attachments={attachments} />
          <AttachmentUploadZone invoiceId={invoiceId} attachments={attachments} onUploaded={load} />
        </>
      )}
    </div>
  )
}
