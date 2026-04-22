import { useState, useEffect } from 'react'
import { downloadAttachment } from '../../../api/invoiceAttachments'

export default function AttachmentsList({ invoiceId, attachments = [] }) {
  const [previews, setPreviews] = useState({})

  useEffect(() => {
    const urls = [];
    attachments.forEach(att => {
      if (att.mimeType && (att.mimeType === 'application/pdf' || att.mimeType.startsWith('image/'))) {
        downloadAttachment(invoiceId, att.id)
          .then(resp => {
            const url = URL.createObjectURL(new Blob([resp.data], { type: att.mimeType }))
            urls.push(url)
            setPreviews(prev => ({ ...prev, [att.id]: url }))
          })
          .catch(() => {})
      }
    })
    return () => urls.forEach(url => URL.revokeObjectURL(url))
  }, [invoiceId, attachments])

  const handleDownload = async (att) => {
    try {
      const resp = await downloadAttachment(invoiceId, att.id)
      const url = URL.createObjectURL(new Blob([resp.data], { type: att.mimeType || 'application/octet-stream' }))
      const a = document.createElement('a')
      a.href = url; a.download = att.filename; a.click()
      URL.revokeObjectURL(url)
    } catch {
      alert('Download failed.')
    }
  }

  if (!attachments.length) return <div className="muted" style={{ padding: 'var(--space-2)' }}>No attachments.</div>

  return (
    <div>
      {attachments.map(a => {
        const previewUrl = previews[a.id]
        const isImage = a.mimeType && a.mimeType.startsWith('image/')
        const isPdf = a.mimeType === 'application/pdf'
        return (
          <div key={a.id} style={{ border: '1px solid #e5e7eb', borderRadius: 6, marginBottom: 10, overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: '#f9fafb', borderBottom: previewUrl ? '1px solid #e5e7eb' : 'none' }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>{a.filename}</span>
              <span style={{ fontSize: 12, color: '#6b7280' }}>{(a.sizeBytes / 1024).toFixed(1)} KB</span>
              {a.securityClass && <span style={{ fontSize: 11, color: '#6b7280' }}>{a.securityClass}</span>}
              {a.attachmentIdentifier && <code style={{ fontSize: 11, color: '#6b7280' }}>{a.attachmentIdentifier}</code>}
              <button className="btn-secondary" style={{ marginLeft: 'auto' }} onClick={() => handleDownload(a)}>Download</button>
            </div>
            {previewUrl && isPdf && (
              <iframe src={previewUrl} title={a.filename} style={{ width: '100%', height: 400, border: 'none', display: 'block' }} />
            )}
            {previewUrl && isImage && (
              <img src={previewUrl} alt={a.filename} style={{ maxWidth: '100%', display: 'block' }} />
            )}
          </div>
        )
      })}
    </div>
  )
}
