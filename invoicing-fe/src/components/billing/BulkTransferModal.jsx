import { useState, useCallback } from 'react'
import { bulkTransferBillingEvents } from '../../api/billingEvents'
import { searchCustomers, getCustomerProperties } from '../../api/customers'
import SearchableAutocomplete from '../SearchableAutocomplete'

export default function BulkTransferModal({ eventIds, onSuccess, onClose }) {
  const [step, setStep] = useState(1)

  const [customerDisplay, setCustomerDisplay]           = useState('')
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')
  const [targetCustomerName, setTargetCustomerName]     = useState('')

  const [propertyDisplay, setPropertyDisplay]   = useState('')
  const [targetPropertyId, setTargetPropertyId] = useState('')

  const [reason, setReason]   = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  const doCustomerSearch = useCallback(async (q) => {
    const res = await searchCustomers(q)
    return res.data
  }, [])

  const doPropertySearch = useCallback(async (q) => {
    if (!targetCustomerNumber) return []
    const res = await getCustomerProperties(targetCustomerNumber, q || undefined)
    return res.data
  }, [targetCustomerNumber])

  const handleCustomerSelect = (option) => {
    setTargetCustomerNumber(option.customerNumber)
    setTargetCustomerName(option.name)
    setCustomerDisplay(option.name)
    setPropertyDisplay('')
    setTargetPropertyId('')
  }

  const handlePropertySelect = (option) => {
    setTargetPropertyId(option.propertyId)
    setPropertyDisplay(option.streetAddress)
  }

  const handleConfirm = async () => {
    setLoading(true)
    setError(null)
    try {
      await bulkTransferBillingEvents({
        eventIds,
        targetCustomerNumber,
        targetPropertyId: targetPropertyId || undefined,
        reason,
      })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Bulk transfer failed.')
      setStep(1)
    } finally {
      setLoading(false)
    }
  }

  const isFormValid = targetCustomerNumber && reason.trim()

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 560 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Transfer {eventIds.length} Events{step === 2 ? ' — Review' : ''}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        {step === 1 && (
          <>
            <div className="modal-body">
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
                    setTargetCustomerName('')
                    setPropertyDisplay('')
                    setTargetPropertyId('')
                  }}
                  onSelect={handleCustomerSelect}
                  onSearch={doCustomerSearch}
                  placeholder="Search by name, number, or address…"
                  renderOption={(c) => (
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{c.name}</div>
                      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                        {c.customerNumber}{c.streetAddress ? ` · ${c.streetAddress}` : ''}{c.city ? `, ${c.city}` : ''}
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
                <label>
                  Target Property <span className="muted">(optional{!targetCustomerNumber ? ' — select a customer first' : ''})</span>
                </label>
                <SearchableAutocomplete
                  key={targetCustomerNumber}
                  value={propertyDisplay}
                  onChange={(raw) => { setPropertyDisplay(raw); setTargetPropertyId('') }}
                  onSelect={handlePropertySelect}
                  onSearch={doPropertySearch}
                  placeholder={targetCustomerNumber ? 'Search by address or property ID…' : 'Select a customer first'}
                  disabled={!targetCustomerNumber}
                  renderOption={(p) => (
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{p.streetAddress}</div>
                      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                        {p.propertyId}{p.city ? ` · ${p.city}` : ''}
                      </div>
                    </div>
                  )}
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
              <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>SENT or COMPLETED events will be skipped automatically.</p>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={onClose}>Cancel</button>
              <button onClick={() => setStep(2)} disabled={!isFormValid} className="btn-primary">
                Next: Review →
              </button>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <div className="modal-body">
              {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
              <div style={{ padding: 'var(--space-4)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                <div style={{ display: 'flex', gap: 'var(--space-4)', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Transferring {eventIds.length} events to</div>
                    <code style={{ fontWeight: 600 }}>{targetCustomerNumber}</code>
                    {targetCustomerName && <span style={{ marginLeft: 6, color: 'var(--color-text-secondary)', fontSize: 13 }}>{targetCustomerName}</span>}
                  </div>
                </div>
                {targetPropertyId && (
                  <div>
                    <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Target property</div>
                    <code>{targetPropertyId}</code> — {propertyDisplay}
                  </div>
                )}
                <div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Reason</div>
                  <span>{reason}</span>
                </div>
              </div>
              <p className="muted" style={{ marginTop: 12, fontSize: 13 }}>
                Events will move to <strong>Pending Transfer</strong> state and require a second confirmation to apply.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setStep(1)}>← Back</button>
              <button onClick={handleConfirm} disabled={loading} className="btn-primary">
                {loading ? 'Initiating…' : `Initiate Transfer for ${eventIds.length} Events`}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
