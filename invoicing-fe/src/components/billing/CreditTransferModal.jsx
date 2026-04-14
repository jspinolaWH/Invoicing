import { useState } from 'react'
import { creditTransferBillingEvent } from '../../api/billingEvents'

export default function CreditTransferModal({ event, onSuccess, onClose }) {
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
      const result = await creditTransferBillingEvent(event.id, {
        targetCustomerNumber,
        targetPropertyId: targetPropertyId || undefined,
        reason,
      })
      onSuccess(result.data)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Credit & Transfer failed.')
    } finally {
      setLoading(false)
    }
  }

  const isValid = /^\d{6,9}$/.test(targetCustomerNumber) && reason.trim()

  const total = ((event.wasteFeePrice ?? 0) + (event.transportFeePrice ?? 0) + (event.ecoFeePrice ?? 0)).toFixed(2)

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 560 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Credit &amp; Transfer Billing Event</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <div style={{ padding: 'var(--space-3)', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 'var(--radius-md)', marginBottom: 16 }}>
            <strong style={{ display: 'block', marginBottom: 4 }}>Warning</strong>
            This will issue a credit for the original invoice and create a new event for the target customer.
            The original event will not be modified.
          </div>

          <div style={{ padding: 'var(--space-3)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', marginBottom: 16 }}>
            <strong style={{ display: 'block', marginBottom: 8 }}>Original Event</strong>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-2)', fontSize: 13 }}>
              <span className="muted">Event #</span><span>{event.id}</span>
              <span className="muted">Customer</span><span><code>{event.customerNumber}</code></span>
              <span className="muted">Product</span><span>{event.product?.name ?? event.product?.code ?? '—'}</span>
              <span className="muted">Total fees</span><span>{total}</span>
            </div>
          </div>

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
              placeholder="Required — will appear in audit trail"
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
            {loading ? 'Processing…' : 'Confirm Credit & Transfer'}
          </button>
        </div>
      </div>
    </div>
  )
}
