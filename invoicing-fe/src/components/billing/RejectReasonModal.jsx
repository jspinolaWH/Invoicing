import { useState } from 'react'
import { rejectBillingEvent } from '../../api/billingEvents'

export default function RejectReasonModal({ eventId, onSuccess, onClose }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    if (!reason.trim()) return
    setLoading(true)
    setError(null)
    try {
      await rejectBillingEvent(eventId, reason)
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Rejection failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 480 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Reject Event</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <p className="muted" style={{ marginBottom: 12 }}>
            Rejecting this event will exclude it from billing. Provide a clear reason.
          </p>
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
          <textarea
            value={reason}
            onChange={e => setReason(e.target.value)}
            rows={4}
            style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
            placeholder="Required rejection reason"
          />
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button
            onClick={handleSubmit}
            disabled={!reason.trim() || loading}
            className="btn-danger"
          >
            {loading ? 'Rejecting…' : 'Reject Event'}
          </button>
        </div>
      </div>
    </div>
  )
}
