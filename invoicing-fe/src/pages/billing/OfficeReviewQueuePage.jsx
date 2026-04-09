import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPendingReview, approveBillingEvent, seedReviewEvent } from '../../api/billingEvents'
import RelatedTasks from '../../components/RelatedTasks'
import RejectReasonModal from '../../components/billing/RejectReasonModal'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'
import './OfficeReviewQueuePage.css'

const RELATED_TASKS = [
  { id: 'PD-228', label: '3.4.4 Driver event review', href: 'https://ioteelab.atlassian.net/browse/PD-228' },
]

/* Fields covered by the seed event, mapped to PD-177 requirement language.
   Sent via POST /driver/events (DriverEventRequest).
   Fields marked "auto-resolved" are derived by the BE from product / customer / allocation rules. */
const PD177_FIELDS = [
  { label: 'Product group / type of waste', value: 'WASTE-COLLECTION-240L  (productId 1)', field: 'productId', how: 'sent' },
  { label: 'Place / place of receipt',      value: 'LOC-MANNERHEIMINTIE-01',               field: 'locationId', how: 'sent' },
  { label: 'Municipality',                  value: 'MUN-HELSINKI',                          field: 'municipalityId', how: 'sent' },
  { label: 'Vehicle',                       value: 'VEH-SOUTH-04',                          field: 'vehicleId', how: 'sent' },
  { label: 'Accounting account',            value: 'Resolved via AllocationRule (product → account)', field: 'accountingAccount', how: 'auto' },
  { label: 'Place of cost (cost centre)',   value: 'Resolved via CostCenter composition config',      field: 'costCenter', how: 'auto' },
  { label: 'Area / service responsibility', value: 'Resolved via CostCenter responsibility segment',  field: 'costCenter.responsibilitySegment', how: 'auto' },
  { label: 'Project code',                  value: 'Not in DriverEventRequest — add to event after review via Edit', field: 'projectId', how: 'manual' },
]

export default function OfficeReviewQueuePage() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [rejectModal, setRejectModal] = useState(null)
  const [actionLoading, setActionLoading] = useState({})

  const [seeding, setSeeding] = useState(false)
  const [seedResult, setSeedResult] = useState(null) // { ok: bool, text: string }
  const [showFields, setShowFields] = useState(false)

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

  const handleSeed = async () => {
    setSeeding(true)
    setSeedResult(null)
    try {
      await seedReviewEvent()
      setSeedResult({ ok: true, text: 'Test event created — it should appear in the queue below.' })
      load()
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message ?? 'Request failed.'
      setSeedResult({ ok: false, text: `Seed failed: ${msg}` })
    } finally {
      setSeeding(false)
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

      {/* ── PD-177 Test Panel ── */}
      <div className="orq-seed-panel">
        <div className="orq-seed-panel__header">
          <div className="orq-seed-panel__title-row">
            <span className="orq-seed-panel__badge">TEST</span>
            <span className="orq-seed-panel__title">PD-177 — Seed a review event</span>
            <span className="orq-seed-panel__desc">
              Creates a billing event with all required reporting fields populated,
              then routes it to this queue for approval testing.
            </span>
          </div>
          <div className="orq-seed-panel__actions">
            <button
              className="orq-seed-panel__toggle"
              onClick={() => setShowFields(p => !p)}
            >
              {showFields ? 'Hide fields ▲' : 'Show fields ▼'}
            </button>
            <button
              className="orq-seed-btn"
              onClick={handleSeed}
              disabled={seeding}
            >
              {seeding ? 'Creating…' : '＋ Seed test event'}
            </button>
          </div>
        </div>

        {seedResult && (
          <div className={`orq-seed-panel__result${seedResult.ok ? ' orq-seed-panel__result--ok' : ' orq-seed-panel__result--err'}`}>
            {seedResult.text}
          </div>
        )}

        {showFields && (
          <div className="orq-seed-panel__fields">
            <p className="orq-seed-panel__fields-label">
              Fields sent with the test event (mapping to PD-177 reporting requirement):
            </p>
            <table className="orq-seed-fields-table">
              <thead>
                <tr>
                  <th>PD-177 requirement term</th>
                  <th>API field</th>
                  <th>Value / source</th>
                  <th>How</th>
                </tr>
              </thead>
              <tbody>
                {PD177_FIELDS.map((f, i) => (
                  <tr key={i}>
                    <td>{f.label}</td>
                    <td><code>{f.field}</code></td>
                    <td>{f.value}</td>
                    <td>
                      <span className={`orq-how-badge orq-how-badge--${f.how}`}>
                        {f.how === 'sent' ? 'sent' : f.how === 'auto' ? 'auto-resolved' : 'manual'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

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
              <th>Municipality</th>
              <th>Location</th>
              <th>Project</th>
              <th>Cost Centre</th>
              <th>Contractor</th>
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
                <td>{evt.municipalityId ?? '—'}</td>
                <td>{evt.locationId ?? '—'}</td>
                <td>{evt.projectId ?? '—'}</td>
                <td>{evt.costCenter?.compositeCode ?? evt.costCenterId ?? '—'}</td>
                <td>{evt.contractor ?? '—'}</td>
                <td
                  style={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                  title={evt.comments}
                >
                  {evt.comments ?? '—'}
                </td>
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
