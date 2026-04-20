import { useState } from 'react'
import { transitionBillingEventStatus } from '../../api/billingEvents'

const NEXT_ACTIONS = {
  DRAFT:       ['IN_PROGRESS'],
  IN_PROGRESS: [],
  SENT:        ['COMPLETED'],
  ERROR:       ['SENT'],
  COMPLETED:   [],
}

export default function StatusTransitionPanel({ eventId, currentStatus, onTransitioned }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [confirmTarget, setConfirmTarget] = useState(null)
  const actions = NEXT_ACTIONS[currentStatus] ?? []

  if (actions.length === 0) return null

  const handleTransition = async (targetStatus) => {
    if (targetStatus === 'COMPLETED') {
      setConfirmTarget(targetStatus)
      return
    }
    await doTransition(targetStatus)
  }

  const doTransition = async (targetStatus) => {
    setLoading(true)
    setError(null)
    setConfirmTarget(null)
    try {
      await transitionBillingEventStatus(eventId, targetStatus)
      onTransitioned()
    } catch (e) {
      setError(e.response?.data?.message ?? 'Transition failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {error && <div className="error-msg">{error}</div>}
      {confirmTarget && (
        <div style={{ padding: 'var(--space-4)', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 'var(--radius-md)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <p style={{ margin: 0 }}>Mark as confirmed by external system? This action cannot be undone.</p>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            <button className="btn-primary" disabled={loading} onClick={() => doTransition(confirmTarget)}>Confirm</button>
            <button className="btn-secondary" disabled={loading} onClick={() => setConfirmTarget(null)}>Cancel</button>
          </div>
        </div>
      )}
      <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
        {actions.map((target) => (
          <button
            key={target}
            disabled={loading}
            onClick={() => handleTransition(target)}
            className="btn-primary"
          >
            Mark as {target === 'IN_PROGRESS' ? 'In Progress' : target.charAt(0) + target.slice(1).toLowerCase()}
          </button>
        ))}
      </div>
    </div>
  )
}
