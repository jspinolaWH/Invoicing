import { useEffect, useState } from 'react'
import { listAttachments, getFinvoiceXml } from '../../../api/invoiceAttachments'
import AttachmentsList from './AttachmentsList'
import AttachmentUploadZone from './AttachmentUploadZone'

export default function InvoiceAttachmentsPanel({ invoiceId, invoiceNumber }) {
  const [attachments, setAttachments] = useState([])
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try { setAttachments((await listAttachments(invoiceId)).data) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [invoiceId])

  const handleViewXml = async () => {
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
      alert('FINVOICE XML not available for this invoice.')
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

      {loading ? <div className="loading">Loading…</div> : (
        <>
          <AttachmentsList invoiceId={invoiceId} attachments={attachments} />
          <AttachmentUploadZone invoiceId={invoiceId} attachments={attachments} onUploaded={load} />
        </>
      )}
    </div>
  )
}
