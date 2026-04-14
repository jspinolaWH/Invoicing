import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getBillingEvent, getCreditTransferLink } from '../../api/billingEvents'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import StatusTransitionPanel from '../../components/billing/StatusTransitionPanel'
import AuditTrailTab from '../../components/billing/AuditTrailTab'
import ExclusionModal from '../../components/billing/ExclusionModal'
import TransferEventModal from '../../components/billing/TransferEventModal'
import CreditTransferModal from '../../components/billing/CreditTransferModal'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-299', label: '3.4.13 Billing event details', href: 'https://ioteelab.atlassian.net/browse/PD-299' },
  { id: 'PD-297', label: '3.4.15 Billing event status',  href: 'https://ioteelab.atlassian.net/browse/PD-297' },
  { id: 'PD-277', label: '3.4.36 Manual editing',        href: 'https://ioteelab.atlassian.net/browse/PD-277' },
  { id: 'PD-318', label: '3.3.18 Editing billing events', href: 'https://ioteelab.atlassian.net/browse/PD-318' },
  { id: 'PD-275', label: '3.4.x Credit & Transfer',      href: 'https://ioteelab.atlassian.net/browse/PD-275' },
]

const TABS = ['Details', 'Audit Trail', 'Status']

function formatTs(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fi-FI')
}

export default function BillingEventDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('Details')
  const [exclusionModal, setExclusionModal] = useState(null) // 'exclude' | 'reinstate' | null
  const [transferModal, setTransferModal] = useState(false)
  const [creditTransferModal, setCreditTransferModal] = useState(false)
  const [creditTransferLink, setCreditTransferLink] = useState(null)

  const load = () => {
    setLoading(true)
    getBillingEvent(id)
      .then(r => setEvent(r.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  const loadLink = () => {
    getCreditTransferLink(id)
      .then(r => setCreditTransferLink(r.data))
      .catch(() => setCreditTransferLink(null))
  }

  useEffect(() => { load(); loadLink() }, [id])

  if (loading) return <div className="loading">Loading event…</div>
  if (!event) return <div className="error-msg">Event not found.</div>

  const canEdit = event.status === 'IN_PROGRESS' || event.status === 'ERROR'
  const canExclude = !event.excluded && event.status !== 'SENT' && event.status !== 'COMPLETED'
  const canTransfer = !event.excluded && event.status === 'IN_PROGRESS'
  const canCreditTransfer = !event.excluded && (event.status === 'SENT' || event.status === 'COMPLETED')

  // Determine the role of this event in any credit-transfer chain
  const isOriginal = creditTransferLink && creditTransferLink.originalEventId === event.id
  const isCredit   = creditTransferLink && creditTransferLink.creditEventId   === event.id
  const isTransfer = creditTransferLink && creditTransferLink.newEventId      === event.id

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-1)' }}>
            <h1 style={{ margin: 0 }}>Event #{event.id}</h1>
            <StatusBadge status={event.status} />
            {event.excluded && <span className="origin-badge" style={{ borderColor: 'var(--color-icon-danger)', color: 'var(--color-icon-danger)' }}>EXCLUDED</span>}
          </div>
          <p>{event.customerNumber} · {event.eventDate} · {event.product?.name ?? event.product?.code}</p>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap' }}>
          {canExclude && (
            <button className="btn-danger" onClick={() => setExclusionModal('exclude')}>
              Exclude
            </button>
          )}
          {event.excluded && (
            <button className="btn-secondary" onClick={() => setExclusionModal('reinstate')}>
              Reinstate
            </button>
          )}
          {canTransfer && (
            <button className="btn-secondary" onClick={() => setTransferModal(true)}>
              Transfer
            </button>
          )}
          {canCreditTransfer && !isOriginal && (
            <button className="btn-secondary" onClick={() => setCreditTransferModal(true)}>
              Credit &amp; Transfer
            </button>
          )}
          <button
            className="btn-secondary"
            disabled={!canEdit}
            title={!canEdit ? 'Event is ' + event.status + ' and cannot be edited' : undefined}
            onClick={() => navigate(`/billing-events/${id}/edit`)}
          >
            Edit
          </button>
          <button className="btn-secondary" onClick={() => navigate('/billing-events')}>← Back</button>
        </div>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {/* Credit-transfer chain banners */}
      {isOriginal && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <span>A credit &amp; transfer was issued for this event.</span>
          <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }}
            onClick={() => navigate(`/billing-events/${creditTransferLink.creditEventId}`)}>
            View Credit Event #{creditTransferLink.creditEventId}
          </button>
          <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }}
            onClick={() => navigate(`/billing-events/${creditTransferLink.newEventId}`)}>
            View Transfer Event #{creditTransferLink.newEventId}
          </button>
        </div>
      )}

      {isCredit && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#fef9c3', border: '1px solid #fde047', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <span>This event is a credit reversal.</span>
          <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }}
            onClick={() => navigate(`/billing-events/${creditTransferLink.originalEventId}`)}>
            View Original Event #{creditTransferLink.originalEventId}
          </button>
        </div>
      )}

      {isTransfer && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <span>This event was created via Credit &amp; Transfer.</span>
          <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }}
            onClick={() => navigate(`/billing-events/${creditTransferLink.originalEventId}`)}>
            View Original Event #{creditTransferLink.originalEventId}
          </button>
          <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }}
            onClick={() => navigate(`/billing-events/${creditTransferLink.creditEventId}`)}>
            View Credit Event #{creditTransferLink.creditEventId}
          </button>
        </div>
      )}

      <div className="tab-bar">
        {TABS.map(t => (
          <button key={t} className={`tab-item ${activeTab === t ? 'tab-item--active' : ''}`}
            onClick={() => setActiveTab(t)}>{t}</button>
        ))}
      </div>

      {activeTab === 'Details' && (
        <div>
          <div className="detail-grid">
            <div className="detail-field"><label>Date</label><span>{event.eventDate}</span></div>
            <div className="detail-field"><label>Customer #</label><span><code>{event.customerNumber}</code></span></div>
            <div className="detail-field"><label>Product</label><span>{event.product?.name ?? event.product?.code}</span></div>
            <div className="detail-field"><label>Origin</label><span><span className="origin-badge">{event.origin ?? '—'}</span></span></div>
            <div className="detail-field"><label>Waste Fee</label><span>{event.wasteFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Transport Fee</label><span>{event.transportFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Eco Fee</label><span>{event.ecoFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Quantity</label><span>{event.quantity}</span></div>
            <div className="detail-field"><label>Weight</label><span>{event.weight}</span></div>
            <div className="detail-field"><label>VAT 0%</label><span>{event.vatRate0}</span></div>
            <div className="detail-field"><label>VAT 24%</label><span>{event.vatRate24}</span></div>
            <div className="detail-field"><label>Classification</label><span>{event.legalClassification ?? '—'}</span></div>
            <div className="detail-field"><label>Vehicle</label><span>{event.vehicleId ?? '—'}</span></div>
            <div className="detail-field"><label>Driver</label><span>{event.driverId ?? '—'}</span></div>
            <div className="detail-field"><label>Location</label><span>{event.locationId ?? '—'}</span></div>
            <div className="detail-field"><label>Municipality</label><span>{event.municipalityId ?? '—'}</span></div>
            <div className="detail-field"><label>Contractor</label><span>{event.contractor ?? '—'}</span></div>
            <div className="detail-field"><label>Direction</label><span>{event.direction ?? '—'}</span></div>
            <div className="detail-field"><label>Shared Collection Group %</label><span>{event.sharedServiceGroupPercentage != null ? `${event.sharedServiceGroupPercentage}%` : '—'}</span></div>
            <div className="detail-field"><label>Accounting Account</label><span>{event.accountingAccount ? `${event.accountingAccount.code} — ${event.accountingAccount.name}` : '—'}</span></div>
            <div className="detail-field"><label>Cost Center</label><span>{event.costCenter?.compositeCode ?? '—'}</span></div>
            <div className="detail-field"><label>Resolved Cost Centre</label><span>{event.resolvedCostCenterCode || '—'}</span></div>
            <div className="detail-field"><label>Project</label><span>{event.projectId ?? '—'}</span></div>
            <div className="detail-field"><label>Non-billable</label><span>{event.nonBillable ? 'Yes' : 'No'}</span></div>
            {event.officeReviewRequired && (
              <div className="detail-field"><label>Review</label><span style={{ color: 'var(--color-warning, #d97706)', fontWeight: 500 }}>Pending office review</span></div>
            )}
            {event.reviewedAt && (
              <div className="detail-field"><label>Reviewed</label><span>{formatTs(event.reviewedAt)} by {event.reviewedBy}</span></div>
            )}
            <div className="detail-field"><label>Created</label><span>{formatTs(event.createdAt)} {event.createdBy ? `by ${event.createdBy}` : ''}</span></div>
            <div className="detail-field"><label>Last Modified</label><span>{formatTs(event.lastModifiedAt)} {event.lastModifiedBy ? `by ${event.lastModifiedBy}` : ''}</span></div>
          </div>
          {(event.resolvedVatRateCode || event.calculatedAmountNet != null) && (
            <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
              <strong style={{ display: 'block', marginBottom: 'var(--space-3)' }}>VAT Details</strong>
              <div className="detail-grid">
                <div className="detail-field"><label>VAT Rate Code</label><span>{event.resolvedVatRateCode ?? '—'}</span></div>
                <div className="detail-field"><label>Effective Rate</label><span>{event.resolvedVatRatePercent != null ? `${event.resolvedVatRatePercent}%` : '—'}</span></div>
                <div className="detail-field"><label>Reverse Charge</label><span>{event.reverseCharge ? <span style={{ color: 'var(--color-warning, #d97706)', fontWeight: 500 }}>Yes</span> : 'No'}</span></div>
                {event.buyerVatNumber && (
                  <div className="detail-field"><label>Buyer VAT #</label><span><code>{event.buyerVatNumber}</code></span></div>
                )}
                <div className="detail-field"><label>Net Amount</label><span>{event.calculatedAmountNet?.toFixed(2) ?? '—'}</span></div>
                <div className="detail-field"><label>VAT Amount</label><span>{event.calculatedAmountVat?.toFixed(2) ?? '—'}</span></div>
                <div className="detail-field"><label>Gross Amount</label><span>{event.calculatedAmountGross?.toFixed(2) ?? '—'}</span></div>
              </div>
            </div>
          )}
          {event.comments && (
            <div style={{ marginTop: 'var(--space-6)' }}>
              <div className="detail-field"><label>Comments</label><span>{event.comments}</span></div>
            </div>
          )}
          {event.excluded && (
            <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: '#fff5f5', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)' }}>
              <strong>Excluded</strong> — {event.exclusionReason ?? 'No reason provided'}
              {event.excludedBy && <span className="muted"> · by {event.excludedBy} at {formatTs(event.excludedAt)}</span>}
            </div>
          )}
          {event.rejectionReason && (
            <div style={{ marginTop: 'var(--space-4)', padding: 'var(--space-4)', background: '#fff5f5', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)' }}>
              <strong>Rejected</strong> — {event.rejectionReason}
            </div>
          )}
        </div>
      )}

      {activeTab === 'Audit Trail' && <AuditTrailTab eventId={id} />}

      {activeTab === 'Status' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
            <span className="muted">Current status:</span>
            <StatusBadge status={event.status} />
          </div>
          <StatusTransitionPanel
            eventId={id}
            currentStatus={event.status}
            onTransitioned={load}
          />
          {!['SENT', 'ERROR'].includes(event.status) && (
            <p className="muted">No manual transitions available for this status.</p>
          )}
        </div>
      )}

      {exclusionModal && (
        <ExclusionModal
          eventId={id}
          mode={exclusionModal}
          onSuccess={() => { setExclusionModal(null); load() }}
          onClose={() => setExclusionModal(null)}
        />
      )}

      {transferModal && (
        <TransferEventModal
          eventId={id}
          currentCustomerNumber={event.customerNumber}
          onSuccess={() => { setTransferModal(false); load() }}
          onClose={() => setTransferModal(false)}
        />
      )}

      {creditTransferModal && (
        <CreditTransferModal
          event={event}
          onSuccess={() => { setCreditTransferModal(false); load(); loadLink() }}
          onClose={() => setCreditTransferModal(false)}
        />
      )}
    </div>
  )
}
