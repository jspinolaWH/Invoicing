import { downloadAttachment } from '../../../api/invoiceAttachments'

export default function AttachmentsList({ invoiceId, attachments = [] }) {
  const handleDownload = async (att) => {
    try {
      const resp = await downloadAttachment(invoiceId, att.id)
      const url = URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }))
      const a = document.createElement('a')
      a.href = url; a.download = att.filename; a.click()
      URL.revokeObjectURL(url)
    } catch {
      alert('Download failed.')
    }
  }

  if (!attachments.length) return <div className="muted" style={{ padding: 'var(--space-2)' }}>No attachments.</div>

  return (
    <table className="data-table">
      <thead>
        <tr><th>Filename</th><th>Size</th><th>Security</th><th>Identifier</th><th>Actions</th></tr>
      </thead>
      <tbody>
        {attachments.map(a => (
          <tr key={a.id}>
            <td>{a.filename}</td>
            <td>{(a.sizeBytes / 1024).toFixed(1)} KB</td>
            <td>{a.securityClass || '—'}</td>
            <td><code style={{ fontSize: 11 }}>{a.attachmentIdentifier}</code></td>
            <td className="actions">
              <button className="btn-secondary" onClick={() => handleDownload(a)}>Download</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
