import { useState } from 'react'
import { excludeBillingEvent, reinstateBillingEvent } from '../../api/billingEvents'

export default function ExclusionModal({ eventId, mode, onSuccess, onClose }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    if (!reason.trim()) return
    setLoading(true)
    setError(null)
    try {
      if (mode === 'exclude') {
        await excludeBillingEvent(eventId, reason)
      } else {
        await reinstateBillingEvent(eventId, reason)
      }
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Operation failed.')
    } finally {
      setLoading(false)
    }
  }

  const title = mode === 'exclude' ? 'Exclude Event' : 'Reinstate Event'
  const label = mode === 'exclude' ? 'Exclusion Reason' : 'Reason for Reinstatement'

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 480 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
          <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
            {label} <span style={{ color: 'var(--color-icon-danger)' }}>*</span>
          </label>
          <textarea
            value={reason}
            onChange={e => setReason(e.target.value)}
            rows={4}
            style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
            placeholder="Required"
          />
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button
            onClick={handleSubmit}
            disabled={!reason.trim() || loading}
            className={mode === 'exclude' ? 'btn-danger' : 'btn-primary'}
          >
            {loading ? 'Saving…' : (mode === 'exclude' ? 'Exclude' : 'Reinstate')}
          </button>
        </div>
      </div>
    </div>
  )
}
