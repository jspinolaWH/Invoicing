import { useState } from 'react'
import { updateInvoiceText } from '../../../api/invoices'

export default function InvoiceCustomTextPanel({ invoice, onUpdated }) {
  const [text, setText] = useState(invoice.customText ?? '')
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState(null)

  const handleSave = async () => {
    setSaving(true)
    try {
      const updated = (await updateInvoiceText(invoice.id, text)).data
      setMsg('Saved.')
      setTimeout(() => setMsg(null), 2000)
      if (onUpdated) onUpdated(updated)
    } catch {
      setMsg('Save failed.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ marginTop: 'var(--space-4)' }}>
      <div className="field">
        <label>Custom Text (visible to customer)</label>
        <textarea
          value={text}
          onChange={e => setText(e.target.value)}
          rows={4}
          style={{ width: '100%' }}
          placeholder="Optional invoice message or notice..."
        />
      </div>
      <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
        <button className="btn-primary" onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Text'}</button>
        {msg && <span className="muted">{msg}</span>}
      </div>
    </div>
  )
}
