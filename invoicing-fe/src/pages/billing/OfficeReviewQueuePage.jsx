import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPendingReview, approveBillingEvent } from '../../api/billingEvents'
import RelatedTasks from '../../components/RelatedTasks'
import RejectReasonModal from '../../components/billing/RejectReasonModal'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-228', label: '3.4.4 Driver event review', href: 'https://ioteelab.atlassian.net/browse/PD-228' },
]

export default function OfficeReviewQueuePage() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [rejectModal, setRejectModal] = useState(null) // eventId or null
  const [actionLoading, setActionLoading] = useState({})

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getPendingReview()
      setEvents(res.data ?? [])
    } catch {
      setError('Failed to load review queue.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    const interval = setInterval(load, 60000)
    return () => clearInterval(interval)
  }, [])

  const handleApprove = async (id) => {
    setActionLoading(prev => ({ ...prev, [id]: 'approving' }))
    try {
      await approveBillingEvent(id)
      setEvents(prev => prev.filter(e => e.id !== id))
    } catch (err) {
      setError(err.response?.data?.message ?? 'Approval failed.')
    } finally {
      setActionLoading(prev => ({ ...prev, [id]: null }))
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Office Review Queue</h1>
          <p>Driver-submitted events pending approval before invoicing.</p>
        </div>
        <button className="btn-secondary" onClick={load}>Refresh</button>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading review queue…</div>
      ) : events.length === 0 ? (
        <div className="empty" style={{ marginTop: 'var(--space-6)' }}>
          No events waiting for review.
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Driver</th>
              <th>Vehicle</th>
              <th>Customer #</th>
              <th>Product</th>
              <th>Waste Fee</th>
              <th>Qty</th>
              <th>Weight</th>
              <th>Location</th>
              <th>Comments</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {events.map(evt => (
              <tr key={evt.id}>
                <td>{evt.eventDate}</td>
                <td>{evt.driverId ?? '—'}</td>
                <td>{evt.vehicleId ?? '—'}</td>
                <td><code>{evt.customerNumber}</code></td>
                <td>{evt.product?.name ?? evt.product?.code ?? '—'}</td>
                <td>{evt.wasteFeePrice?.toFixed(2)}</td>
                <td>{evt.quantity}</td>
                <td>{evt.weight}</td>
                <td>{evt.locationId ?? '—'}</td>
                <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={evt.comments}>{evt.comments ?? '—'}</td>
                <td>
                  <div className="actions">
                    <button
                      className="btn-secondary"
                      onClick={() => navigate(`/billing-events/${evt.id}`)}
                    >
                      View
                    </button>
                    <button
                      className="btn-primary"
                      disabled={actionLoading[evt.id] === 'approving'}
                      onClick={() => handleApprove(evt.id)}
                      style={{ background: '#15803d', borderColor: '#15803d' }}
                    >
                      {actionLoading[evt.id] === 'approving' ? '…' : 'Approve'}
                    </button>
                    <button
                      className="btn-danger"
                      onClick={() => setRejectModal(evt.id)}
                    >
                      Reject
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {rejectModal && (
        <RejectReasonModal
          eventId={rejectModal}
          onSuccess={() => { setRejectModal(null); load() }}
          onClose={() => setRejectModal(null)}
        />
      )}
    </div>
  )
}
