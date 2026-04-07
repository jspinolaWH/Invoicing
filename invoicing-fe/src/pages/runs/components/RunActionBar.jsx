import { useState } from 'react'
import { cancelRun, scheduleSend, triggerSend } from '../../../api/invoiceRuns'

const TERMINAL = ['COMPLETED', 'COMPLETED_WITH_ERRORS', 'ERROR', 'CANCELLED', 'SENT']

export default function RunActionBar({ run, onUpdated }) {
  const [showCancel, setShowCancel] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [scheduleAt, setScheduleAt] = useState('')
  const [error, setError] = useState(null)

  if (!run || TERMINAL.includes(run.status)) {
    if (run?.status === 'COMPLETED' || run?.status === 'COMPLETED_WITH_ERRORS') {
      // Show send actions for completed runs
    } else {
      return null
    }
  }

  const handleCancel = async () => {
    if (!cancelReason.trim()) return
    try {
      await cancelRun(run.id, cancelReason)
      setShowCancel(false)
      onUpdated()
    } catch (e) {
      setError(e.response?.data?.message || 'Cancel failed')
    }
  }

  const handleScheduleSend = async () => {
    if (!scheduleAt) return
    try {
      await scheduleSend(run.id, new Date(scheduleAt).toISOString())
      onUpdated()
    } catch (e) {
      setError(e.response?.data?.message || 'Schedule failed')
    }
  }

  const handleSendNow = async () => {
    if (!window.confirm('Send all invoices now?')) return
    try {
      await triggerSend(run.id)
      onUpdated()
    } catch (e) {
      setError(e.response?.data?.message || 'Send failed')
    }
  }

  const isCompleted = run.status === 'COMPLETED' || run.status === 'COMPLETED_WITH_ERRORS'
  const canCancel = !['SENT', 'CANCELLED', 'ERROR'].includes(run.status)

  return (
    <div style={{ marginBottom: 'var(--space-4)' }}>
      {error && <div className="error-msg" style={{ marginBottom: 'var(--space-2)' }}>{error}</div>}
      <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap', alignItems: 'center' }}>
        {isCompleted && (
          <>
            <input type="datetime-local" value={scheduleAt} onChange={e => setScheduleAt(e.target.value)}
              style={{ padding: '6px 8px', border: '1px solid var(--color-border-default)', borderRadius: 'var(--radius-sm)' }} />
            <button className="btn-secondary" onClick={handleScheduleSend} disabled={!scheduleAt}>
              Schedule Send
            </button>
            <button className="btn-primary" onClick={handleSendNow}>Send Now</button>
          </>
        )}
        {canCancel && (
          <button className="btn-danger" onClick={() => setShowCancel(true)}>Cancel Run</button>
        )}
      </div>
      {run.scheduledSendAt && (
        <div style={{ marginTop: 'var(--space-2)', fontSize: 13, color: 'var(--color-text-secondary)' }}>
          Scheduled for {new Date(run.scheduledSendAt).toLocaleString('fi-FI')}
        </div>
      )}
      {showCancel && (
        <div className="modal-overlay" onClick={() => setShowCancel(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Cancel Run</h3>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>
              This will revert all invoices and release invoice numbers. Enter a reason:
            </p>
            <div className="form-group">
              <textarea value={cancelReason} onChange={e => setCancelReason(e.target.value)}
                rows={3} style={{ width: '100%' }} placeholder="Reason for cancellation..." />
            </div>
            <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
              <button className="btn-secondary" onClick={() => setShowCancel(false)}>Close</button>
              <button className="btn-danger" onClick={handleCancel} disabled={!cancelReason.trim()}>
                Confirm Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
