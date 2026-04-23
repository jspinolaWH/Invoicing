import { useState, useEffect } from 'react'
import { downloadAttachment } from '../../../api/invoiceAttachments'

export default function AttachmentsList({ invoiceId, attachments = [] }) {
  const [previews, setPreviews] = useState({})
  const [previewErrors, setPreviewErrors] = useState({})
  const [downloadErrors, setDownloadErrors] = useState({})

  useEffect(() => {
    const urls = [];
    setPreviewErrors({})
    attachments.forEach(att => {
      if (att.mimeType && (att.mimeType === 'application/pdf' || att.mimeType.startsWith('image/'))) {
        downloadAttachment(invoiceId, att.id)
          .then(resp => {
            const url = URL.createObjectURL(new Blob([resp.data], { type: att.mimeType }))
            urls.push(url)
            setPreviews(prev => ({ ...prev, [att.id]: url }))
          })
          .catch(() => {
            setPreviewErrors(prev => ({ ...prev, [att.id]: true }))
          })
      }
    })
    return () => urls.forEach(url => URL.revokeObjectURL(url))
  }, [invoiceId, attachments])

  const handleDownload = async (att) => {
    setDownloadErrors(prev => ({ ...prev, [att.id]: null }))
    try {
      const resp = await downloadAttachment(invoiceId, att.id)
      const url = URL.createObjectURL(new Blob([resp.data], { type: att.mimeType || 'application/octet-stream' }))
      const a = document.createElement('a')
      a.href = url; a.download = att.filename; a.click()
      URL.revokeObjectURL(url)
    } catch {
      setDownloadErrors(prev => ({ ...prev, [att.id]: `Download failed for "${att.filename}". The file may be unavailable — try again or contact your administrator.` }))
    }
  }

  if (!attachments.length) return <div className="muted" style={{ padding: 'var(--space-2)' }}>No attachments.</div>

  return (
    <div>
      {attachments.map(a => {
        const previewUrl = previews[a.id]
        const isImage = a.mimeType && a.mimeType.startsWith('image/')
        const isPdf = a.mimeType === 'application/pdf'
        const hasPreviewError = previewErrors[a.id]
        const downloadError = downloadErrors[a.id]
        const expectsPreview = a.mimeType && (isPdf || isImage)
        return (
          <div key={a.id} style={{ border: '1px solid #e5e7eb', borderRadius: 6, marginBottom: 10, overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>{a.filename}</span>
              <span style={{ fontSize: 12, color: '#6b7280' }}>{(a.sizeBytes / 1024).toFixed(1)} KB</span>
              {a.securityClass && <span style={{ fontSize: 11, color: '#6b7280' }}>{a.securityClass}</span>}
              {a.attachmentIdentifier
                ? (
                  <span title={`SHA1: ${a.attachmentIdentifier}`} style={{
                    display: 'inline-flex', alignItems: 'center', gap: 3,
                    fontSize: 11, color: '#166534', background: '#f0fdf4',
                    border: '1px solid #bbf7d0', borderRadius: 4, padding: '1px 6px',
                  }}>
                    ✓ SHA1 <code style={{ fontFamily: 'monospace', fontSize: 10 }}>{a.attachmentIdentifier.slice(0, 8)}…</code>
                  </span>
                )
                : (
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: 3,
                    fontSize: 11, color: '#92400e', background: '#fff7ed',
                    border: '1px solid #fed7aa', borderRadius: 4, padding: '1px 6px',
                  }}>
                    ⚠ No checksum
                  </span>
                )
              }
              <button className="btn-secondary" style={{ marginLeft: 'auto' }} onClick={() => handleDownload(a)}>Download</button>
            </div>

            {downloadError && (
              <div style={{
                display: 'flex', alignItems: 'flex-start', gap: 8,
                padding: '8px 12px', background: '#fef2f2', borderBottom: '1px solid #fecaca',
                fontSize: 12, color: '#991b1b',
              }}>
                <span style={{ fontWeight: 600 }}>✕</span>
                <span style={{ flex: 1 }}>{downloadError}</span>
                <button
                  onClick={() => setDownloadErrors(prev => ({ ...prev, [a.id]: null }))}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#991b1b', fontSize: 14, lineHeight: 1, padding: 0 }}
                  aria-label="Dismiss"
                >×</button>
              </div>
            )}

            <div style={{ padding: '8px 12px', background: '#fff', fontSize: 12, color: '#374151', borderBottom: (previewUrl || hasPreviewError) ? '1px solid #e5e7eb' : 'none' }}>
              <span style={{ fontWeight: 600, color: '#6b7280', marginRight: 8 }}>FINVOICE AttachmentDetails</span>
              <span style={{ marginRight: 16 }}>
                <span style={{ color: '#9ca3af' }}>Identifier: </span>
                <code style={{ fontFamily: 'monospace', fontSize: 11 }}>{a.attachmentIdentifier || '—'}</code>
              </span>
              <span style={{ marginRight: 16 }}>
                <span style={{ color: '#9ca3af' }}>Name: </span>
                {a.filename}
              </span>
              <span style={{ marginRight: 16 }}>
                <span style={{ color: '#9ca3af' }}>MIME: </span>
                {a.mimeType || '—'}
              </span>
              {a.securityClass && (
                <span>
                  <span style={{ color: '#9ca3af' }}>Security: </span>
                  {a.securityClass}
                </span>
              )}
            </div>

            {expectsPreview && hasPreviewError && (
              <div style={{
                padding: '10px 12px', background: '#fef2f2',
                fontSize: 12, color: '#991b1b', fontWeight: 600,
              }}>
                Preview unavailable — the file could not be loaded. Use the Download button to retrieve it.
              </div>
            )}
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
