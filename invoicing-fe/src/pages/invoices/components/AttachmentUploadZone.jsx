import { useRef, useState } from 'react'
import { uploadAttachment } from '../../../api/invoiceAttachments'
import SecurityClassSelector from './SecurityClassSelector'

const MAX_TOTAL_BYTES = 1_048_576
const MAX_COUNT = 10

export default function AttachmentUploadZone({ invoiceId, attachments = [], onUploaded }) {
  const [securityClass, setSecurityClass] = useState('')
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const inputRef = useRef(null)

  const currentBytes = attachments.reduce((s, a) => s + (a.sizeBytes || 0), 0)
  const remaining = MAX_TOTAL_BYTES - currentBytes
  const atLimit = attachments.length >= MAX_COUNT

  const ALLOWED_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png']
  const ALLOWED_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png']

  const handleFile = async (file) => {
    setError(null)
    const lowerName = file.name.toLowerCase()
    const hasAllowedExt = ALLOWED_EXTENSIONS.some(ext => lowerName.endsWith(ext))
    if (!hasAllowedExt && !ALLOWED_MIME_TYPES.includes(file.type)) {
      setError('Only PDF/A, JPEG, and PNG files are accepted.'); return
    }
    if (attachments.length >= MAX_COUNT) {
      setError('Maximum 10 attachments reached.'); return
    }
    if (currentBytes + file.size > MAX_TOTAL_BYTES) {
      setError(`File too large. Only ${(remaining / 1024).toFixed(0)} KB remaining.`); return
    }
    setUploading(true)
    try {
      await uploadAttachment(invoiceId, file, securityClass || undefined)
      if (onUploaded) onUploaded()
    } catch (e) {
      setError(e?.response?.data || 'Upload failed.')
    } finally { setUploading(false) }
  }

  return (
    <div style={{ marginTop: 'var(--space-3)' }}>
      <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <SecurityClassSelector value={securityClass} onChange={setSecurityClass} />
        <div>
          <div className="label">{attachments.length} / {MAX_COUNT} files &nbsp;·&nbsp; {(remaining / 1024).toFixed(0)} KB remaining</div>
          <button className="btn-secondary" disabled={atLimit || uploading}
            onClick={() => inputRef.current?.click()}>
            {uploading ? 'Uploading…' : '+ Upload File'}
          </button>
        </div>
        <input ref={inputRef} type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" style={{ display: 'none' }}
          onChange={e => { if (e.target.files[0]) handleFile(e.target.files[0]); e.target.value = '' }} />
      </div>
      {atLimit && <div className="error-msg" style={{ marginTop: 'var(--space-2)' }}>Maximum 10 attachments reached.</div>}
      {error && <div className="error-msg" style={{ marginTop: 'var(--space-2)' }}>{error}</div>}
    </div>
  )
}
