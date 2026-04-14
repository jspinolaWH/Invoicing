import { useState, useCallback } from 'react'
import { creditTransferBillingEvent } from '../../api/billingEvents'
import { searchCustomers } from '../../api/customers'
import { searchProperties } from '../../api/properties'
import SearchableAutocomplete from '../SearchableAutocomplete'

export default function CreditTransferModal({ event, onSuccess, onClose }) {
  // Customer field
  const [customerDisplay, setCustomerDisplay]         = useState('')
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')

  // Property field
  const [propertyDisplay, setPropertyDisplay]   = useState('')
  const [targetPropertyId, setTargetPropertyId] = useState('')

  const [reason, setReason]   = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  // ── Search functions ──────────────────────────────────────────────────
  const doCustomerSearch = useCallback(async (q) => {
    const res = await searchCustomers(q)
    return res.data
  }, [])

  const doPropertySearch = useCallback(async (q) => {
    const res = await searchProperties(q)
    return res.data
  }, [])

  // ── Selection handlers ────────────────────────────────────────────────
  const handleCustomerSelect = (option) => {
    setTargetCustomerNumber(option.customerNumber)
    setCustomerDisplay(option.name)
  }

  const handlePropertySelect = (option) => {
    setTargetPropertyId(option.propertyId)
    setPropertyDisplay(option.streetAddress)
  }

  // ── Submit ────────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    if (!targetCustomerNumber || !reason.trim()) return
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

  const isValid = targetCustomerNumber && reason.trim()

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
            <label>
              Target Customer <span style={{ color: 'var(--color-icon-danger)' }}>*</span>
            </label>
            <SearchableAutocomplete
              value={customerDisplay}
              onChange={(raw) => {
                setCustomerDisplay(raw)
                setTargetCustomerNumber('')
              }}
              onSelect={handleCustomerSelect}
              onSearch={doCustomerSearch}
              placeholder="Search by name, number, or address…"
              required
              renderOption={(c) => (
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{c.name}</div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                    {c.customerNumber}
                    {c.streetAddress ? ` · ${c.streetAddress}` : ''}
                    {c.city ? `, ${c.city}` : ''}
                  </div>
                </div>
              )}
            />
            {targetCustomerNumber && (
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
                Customer number: <code>{targetCustomerNumber}</code>
              </div>
            )}
          </div>

          <div className="field" style={{ marginBottom: 12 }}>
            <label>Target Property <span className="muted">(optional)</span></label>
            <SearchableAutocomplete
              value={propertyDisplay}
              onChange={(raw) => {
                setPropertyDisplay(raw)
                setTargetPropertyId('')
              }}
              onSelect={handlePropertySelect}
              onSearch={doPropertySearch}
              placeholder="Search by address or property ID…"
              renderOption={(p) => (
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{p.streetAddress}</div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                    {p.propertyId}
                    {p.city ? ` · ${p.city}` : ''}
                  </div>
                </div>
              )}
            />
            {targetPropertyId && (
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
                Property ID: <code>{targetPropertyId}</code>
              </div>
            )}
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
