import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { transferBillingEvent, updateBillingEventComponents } from '../../api/billingEvents'
import { searchCustomers, getCustomerProperties } from '../../api/customers'
import SearchableAutocomplete from '../SearchableAutocomplete'

export default function TransferEventModal({ eventId, currentCustomerNumber, onSuccess, onClose }) {
  const navigate = useNavigate()
  const [step, setStep] = useState(1) // 1 = form, 2 = review

  const [customerDisplay, setCustomerDisplay]         = useState('')
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')
  const [targetCustomerName, setTargetCustomerName]   = useState('')

  const [propertyDisplay, setPropertyDisplay]         = useState('')
  const [targetPropertyId, setTargetPropertyId]       = useState('')
  const [targetPropertyDbId, setTargetPropertyDbId]   = useState(null)

  const [feeComponents, setFeeComponents] = useState({
    includeWasteFee: true,
    includeTransportFee: true,
    includeEcoFee: true,
  })

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
    setTargetPropertyDbId(null)
  }

  const handlePropertySelect = (option) => {
    setTargetPropertyId(option.propertyId)
    setTargetPropertyDbId(option.id)
    setPropertyDisplay(option.streetAddress)
  }

  const handleConfirm = async () => {
    setLoading(true)
    setError(null)
    try {
      await transferBillingEvent(eventId, {
        targetCustomerNumber,
        targetPropertyId: targetPropertyId || undefined,
        reason,
      })
      const allIncluded = feeComponents.includeWasteFee && feeComponents.includeTransportFee && feeComponents.includeEcoFee
      if (!allIncluded) {
        await updateBillingEventComponents(eventId, feeComponents)
      }
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Transfer failed.')
      setStep(1)
    } finally {
      setLoading(false)
    }
  }

  const isFormValid = targetCustomerNumber && reason.trim()

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Transfer Billing Event{step === 2 ? ' — Review' : ''}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        {step === 1 && (
          <>
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
                    setTargetCustomerName('')
                    setPropertyDisplay('')
                    setTargetPropertyId('')
                  }}
                  onSelect={handleCustomerSelect}
                  onSearch={doCustomerSearch}
                  placeholder="Search by name, number, or address…"
                  required
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
                  onChange={(raw) => {
                    setPropertyDisplay(raw)
                    setTargetPropertyId('')
                    setTargetPropertyDbId(null)
                  }}
                  onSelect={handlePropertySelect}
                  onSearch={doPropertySearch}
                  placeholder={targetCustomerNumber ? 'Search by address or property ID…' : 'Select a customer first'}
                  disabled={!targetCustomerNumber}
                  renderOption={(p) => (
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{p.streetAddress}</div>
                      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                        {p.propertyId}{p.city ? ` · ${p.city}` : ''}
                        {p.municipalityCode ? ` · ${p.municipalityCode}` : ''}
                      </div>
                    </div>
                  )}
                />
                {targetPropertyId && (
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
                    Property ID: <code>{targetPropertyId}</code>
                    <button type="button" className="btn-secondary"
                      style={{ padding: '1px 8px', fontSize: 11 }}
                      onClick={() => navigate(`/properties/${targetPropertyDbId}`)}>
                      View detail
                    </button>
                  </div>
                )}
              </div>

              <div className="field" style={{ marginBottom: 12 }}>
                <label>Reason <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
                <textarea
                  value={reason}
                  onChange={e => setReason(e.target.value)}
                  rows={3}
                  style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
                  placeholder="Required"
                />
              </div>

              <div className="field">
                <label>Fee Components to Transfer</label>
                <p style={{ margin: '4px 0 8px', fontSize: 13, color: 'var(--color-text-secondary)' }}>
                  Deselect any fee that should not follow the transfer. Excluded fees will remain at zero on the transferred event.
                </p>
                <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
                  {[
                    { key: 'includeWasteFee', label: 'Waste Fee' },
                    { key: 'includeTransportFee', label: 'Transport Fee' },
                    { key: 'includeEcoFee', label: 'Eco Fee' },
                  ].map(({ key, label }) => (
                    <label key={key} style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', padding: 'var(--space-2) var(--space-3)', border: `1px solid ${feeComponents[key] ? 'var(--color-border)' : '#fecaca'}`, borderRadius: 'var(--radius-md)', background: feeComponents[key] ? 'white' : '#fff5f5' }}>
                      <input
                        type="checkbox"
                        checked={feeComponents[key]}
                        onChange={() => setFeeComponents(f => ({ ...f, [key]: !f[key] }))}
                      />
                      <span style={{ fontWeight: 500, fontSize: 13 }}>{label}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={onClose}>Cancel</button>
              <button
                onClick={() => setStep(2)}
                disabled={!isFormValid}
                className="btn-primary"
              >
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
                    <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Current customer</div>
                    <code style={{ fontWeight: 600 }}>{currentCustomerNumber}</code>
                  </div>
                  <span style={{ color: 'var(--color-text-secondary)', fontSize: 20 }}>→</span>
                  <div>
                    <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>New customer</div>
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
                <div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Fee Components</div>
                  <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap', marginTop: 4 }}>
                    {[
                      { key: 'includeWasteFee', label: 'Waste Fee' },
                      { key: 'includeTransportFee', label: 'Transport Fee' },
                      { key: 'includeEcoFee', label: 'Eco Fee' },
                    ].map(({ key, label }) => (
                      <span key={key} style={{ padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 500, border: `1px solid ${feeComponents[key] ? '#bbf7d0' : '#fecaca'}`, background: feeComponents[key] ? '#f0fdf4' : '#fff5f5', color: feeComponents[key] ? '#15803d' : '#b91c1c' }}>
                        {label}: {feeComponents[key] ? 'Included' : 'Excluded'}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
              <p className="muted" style={{ marginTop: 12, fontSize: 13 }}>
                This will put the event into <strong>Pending Transfer</strong> state. A second confirmation step will be required to apply the change.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setStep(1)}>← Back</button>
              <button
                onClick={handleConfirm}
                disabled={loading}
                className="btn-primary"
              >
                {loading ? 'Initiating…' : 'Initiate Transfer'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
