import { useState, useCallback } from 'react'
import { transferBillingEvent } from '../../api/billingEvents'
import { searchCustomers } from '../../api/customers'
import { searchProperties } from '../../api/properties'
import SearchableAutocomplete from '../SearchableAutocomplete'

export default function TransferEventModal({ eventId, currentCustomerNumber, onSuccess, onClose }) {
  // Customer field
  const [customerDisplay, setCustomerDisplay]   = useState('')
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

  const isValid = targetCustomerNumber && reason.trim()

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
