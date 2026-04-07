import { useState } from 'react'
import { transitionBillingEventStatus } from '../../api/billingEvents'

const NEXT_ACTIONS = {
  IN_PROGRESS: [],
  SENT:        ['COMPLETED', 'ERROR'],
  ERROR:       ['SENT'],
  COMPLETED:   [],
}

export default function StatusTransitionPanel({ eventId, currentStatus, onTransitioned }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const actions = NEXT_ACTIONS[currentStatus] ?? []

  if (actions.length === 0) return null

  const handleTransition = async (targetStatus) => {
    setLoading(true)
    setError(null)
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
      <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
        {actions.map((target) => (
          <button
            key={target}
            disabled={loading}
            onClick={() => handleTransition(target)}
            className={target === 'ERROR' ? 'btn-danger' : 'btn-primary'}
          >
            Mark as {target === 'IN_PROGRESS' ? 'In Progress' : target.charAt(0) + target.slice(1).toLowerCase()}
          </button>
        ))}
      </div>
    </div>
  )
}
