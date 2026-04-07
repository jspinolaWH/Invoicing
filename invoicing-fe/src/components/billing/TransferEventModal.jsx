import { useState } from 'react'
import { transferBillingEvent } from '../../api/billingEvents'

export default function TransferEventModal({ eventId, currentCustomerNumber, onSuccess, onClose }) {
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')
  const [targetPropertyId, setTargetPropertyId] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    if (!targetCustomerNumber.trim() || !reason.trim()) return
    setLoading(true)
    setError(null)
    try {
      await transferBillingEvent(eventId, {
        targetCustomerNumber,
        targetPropertyId: targetPropertyId || undefined,
        reason,
      })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Transfer failed.')
    } finally {
      setLoading(false)
    }
  }

  const isValid = /^\d{6,9}$/.test(targetCustomerNumber) && reason.trim()

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Transfer Billing Event</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <p className="muted" style={{ marginBottom: 16 }}>
            Currently assigned to customer <strong>{currentCustomerNumber}</strong>.
          </p>
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}

          <div className="field" style={{ marginBottom: 12 }}>
            <label>Target Customer Number <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
            <input
              type="text"
              value={targetCustomerNumber}
              onChange={e => setTargetCustomerNumber(e.target.value)}
              placeholder="6–9 digit customer number"
            />
          </div>

          <div className="field" style={{ marginBottom: 12 }}>
            <label>Target Property ID <span className="muted">(optional)</span></label>
            <input
              type="text"
              value={targetPropertyId}
              onChange={e => setTargetPropertyId(e.target.value)}
              placeholder="Leave blank if not a location transfer"
            />
          </div>

          <div className="field">
            <label>Reason <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
            <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              rows={3}
              style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
              placeholder="Required"
            />
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button
            onClick={handleSubmit}
            disabled={!isValid || loading}
            className="btn-primary"
          >
            {loading ? 'Transferring…' : 'Transfer Event'}
          </button>
        </div>
      </div>
    </div>
  )
}
