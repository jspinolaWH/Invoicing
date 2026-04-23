import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  getBillingEvent, getCreditTransferLink, simulateTransmissionOutcome,
  transitionBillingEventStatus,
  getBillingEventAttachments, uploadBillingEventAttachment,
  downloadBillingEventAttachment, deleteBillingEventAttachment,
  confirmTransferBillingEvent, cancelTransferBillingEvent,
  updateBillingEventComponents, recordContractorPayment,
  overrideValidation, validateBillingEvents, getBillingEventValidationFailures,
  approveCorrectionBillingEvent, getBillingEventParentInvoice,
} from '../../api/billingEvents'
import { getClassificationRules } from '../../api/classificationRules'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import StatusTransitionPanel from '../../components/billing/StatusTransitionPanel'
import AuditTrailTab from '../../components/billing/AuditTrailTab'
import ExclusionModal from '../../components/billing/ExclusionModal'
import TransferEventModal from '../../components/billing/TransferEventModal'
import CreditTransferModal from '../../components/billing/CreditTransferModal'
import ValidationFailuresList from '../invoices/components/ValidationFailuresList'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-299', label: '3.4.13 Billing event details', href: 'https://ioteelab.atlassian.net/browse/PD-299' },
  { id: 'PD-297', label: '3.4.15 Billing event status',  href: 'https://ioteelab.atlassian.net/browse/PD-297' },
  { id: 'PD-277', label: '3.4.36 Manual editing',        href: 'https://ioteelab.atlassian.net/browse/PD-277' },
  { id: 'PD-318', label: '3.3.18 Editing billing events', href: 'https://ioteelab.atlassian.net/browse/PD-318' },
  { id: 'PD-275', label: '3.4.x Credit & Transfer',      href: 'https://ioteelab.atlassian.net/browse/PD-275' },
]

const TABS = ['Details', 'Audit Trail', 'Status', 'Attachments']

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
  const [simOutcome, setSimOutcome] = useState('SENT')
  const [simErrorReason, setSimErrorReason] = useState('')
  const [simLoading, setSimLoading] = useState(false)
  const [simError, setSimError] = useState(null)
  const [parentInvoiceId, setParentInvoiceId] = useState(null)

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

  const loadParentInvoice = () => {
    getBillingEventParentInvoice(id)
      .then(r => setParentInvoiceId(r.data?.invoiceId ?? null))
      .catch(() => setParentInvoiceId(null))
  }

  useEffect(() => { load(); loadLink(); loadParentInvoice() }, [id])

  if (loading) return <div className="loading">Loading event…</div>
  if (!event) return <div className="error-msg">Event not found.</div>

  const canEdit = event.status === 'DRAFT' || event.status === 'IN_PROGRESS' || event.status === 'FOR_CORRECTION' || event.status === 'ERROR'
  const canExclude = !event.excluded && event.status !== 'SENT' && event.status !== 'COMPLETED' && event.status !== 'PENDING_TRANSFER'
  const canTransfer = !event.excluded && event.status === 'IN_PROGRESS'
  const isPendingTransfer = event.status === 'PENDING_TRANSFER'
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
          {event.status === 'DRAFT' && (
            <button className="btn-primary" onClick={async () => {
              try {
                await transitionBillingEventStatus(id, 'IN_PROGRESS')
                load()
              } catch (e) {
                alert(e.response?.data?.message ?? 'Failed to submit draft.')
              }
            }}>
              Submit
            </button>
          )}
          {event.status === 'FOR_CORRECTION' && (
            <button className="btn-primary" onClick={async () => {
              try {
                await approveCorrectionBillingEvent(id)
                load()
              } catch (e) {
                alert(e.response?.data?.message ?? 'Failed to approve correction.')
              }
            }}>
              Approve Correction
            </button>
          )}
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
          {isPendingTransfer && (
            <>
              <button className="btn-primary" onClick={async () => {
                try {
                  await confirmTransferBillingEvent(id)
                  load()
                } catch (e) {
                  alert(e.response?.data?.message ?? 'Confirm transfer failed.')
                }
              }}>
                Confirm Transfer
              </button>
              <button className="btn-danger" onClick={async () => {
                if (!window.confirm('Cancel this pending transfer?')) return
                try {
                  await cancelTransferBillingEvent(id)
                  load()
                } catch (e) {
                  alert(e.response?.data?.message ?? 'Cancel transfer failed.')
                }
              }}>
                Cancel Transfer
              </button>
            </>
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

      {event.status === 'FOR_CORRECTION' && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#faf5ff', border: '1px solid #d8b4fe', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)' }}>
          <strong style={{ color: '#7c3aed' }}>Awaiting correction review</strong>
          <span style={{ color: '#6b21a8', marginLeft: 8, fontSize: 14 }}>
            This event was manually edited and is blocked from invoicing until an INVOICING user approves the correction.
          </span>
        </div>
      )}

      {isPendingTransfer && event.pendingTransferCustomerNumber && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          <span>Transfer pending: <strong>{event.customerNumber}</strong> → <strong>{event.pendingTransferCustomerNumber}</strong></span>
          {event.pendingTransferLocationId && <span>· Property: <code>{event.pendingTransferLocationId}</code></span>}
        </div>
      )}

      {event.priorCustomerNumber && (
        <div style={{ padding: 'var(--space-3) var(--space-4)', background: '#fefce8', border: '1px solid #fde047', borderRadius: 'var(--radius-md)', marginBottom: 'var(--space-4)' }}>
          Transferred from: <strong>{event.priorCustomerNumber}</strong>
          {event.priorLocationId && <span> · Prior property: <code>{event.priorLocationId}</code></span>}
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
            <div className="detail-field">
              <label>Billing Type</label>
              <span>
                {event.resolvedBillingType === 'IMMEDIATE'
                  ? <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #fcd34d', background: '#fffbeb', color: '#92400e', fontSize: 12, fontWeight: 600 }}>Immediate — processed in next run, not held for cycle</span>
                  : <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #bae6fd', background: '#eff6ff', color: '#0369a1', fontSize: 12, fontWeight: 600 }}>Cycle-based — held until billing cycle is due</span>}
              </span>
            </div>
            <div className="detail-field"><label>Waste Fee</label><span>{event.wasteFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Transport Fee</label><span>{event.transportFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Eco Fee</label><span>{event.ecoFeePrice?.toFixed(2)}</span></div>
            <div className="detail-field"><label>Quantity</label><span>{event.quantity}</span></div>
            <div className="detail-field"><label>Weight</label><span>{event.weight}</span></div>
            <div className="detail-field"><label>VAT 0%</label><span>{event.vatRate0}</span></div>
            <div className="detail-field"><label>VAT 24%</label><span>{event.vatRate24}</span></div>
            <div className="detail-field"><label>Classification</label><span>{event.legalClassification ?? '—'}</span></div>
            <div className="detail-field">
              <label>Enforcement Path</label>
              <span>
                {event.legalClassification === 'PUBLIC_LAW' && (
                  <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #bfdbfe', background: '#eff6ff', color: '#1d4ed8', fontSize: 12, fontWeight: 600 }}>
                    Statutory Authority — public-law enforcement applies
                  </span>
                )}
                {event.legalClassification === 'PRIVATE_LAW' && (
                  <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #fed7aa', background: '#fff7ed', color: '#c2410c', fontSize: 12, fontWeight: 600 }}>
                    General Debt Collection — private-law enforcement applies
                  </span>
                )}
                {!event.legalClassification && <span className="muted">Not determined</span>}
              </span>
            </div>
            <div className="detail-field"><label>Registration Number</label><span>{event.registrationNumber ?? '—'}</span></div>
            <div className="detail-field"><label>Vehicle</label><span>{event.vehicleId ?? '—'}</span></div>
            <div className="detail-field"><label>Driver</label><span>{event.driverId ?? '—'}</span></div>
            <div className="detail-field"><label>Location</label><span>{event.locationId ?? '—'}</span></div>
            <div className="detail-field"><label>Municipality</label><span>{event.municipalityId ?? '—'}</span></div>
            <div className="detail-field"><label>Contractor</label><span>{event.contractor ?? '—'}</span></div>
            <div className="detail-field"><label>Direction</label><span>{event.direction ?? '—'}</span></div>
            <div className="detail-field"><label>Shared Collection Group %</label><span>{event.sharedServiceGroupPercentage != null ? `${event.sharedServiceGroupPercentage}%` : '—'}</span></div>
            <div className="detail-field"><label>Waste Type</label><span>{event.wasteType ?? '—'}</span></div>
            <div className="detail-field"><label>Receiving Site</label><span>{event.receivingSite ?? '—'}</span></div>
            <div className="detail-field"><label>Product Group</label><span>{event.productGroup ?? '—'}</span></div>
            <div className="detail-field"><label>Responsibility Area</label><span>{event.responsibilityArea ?? '—'}</span></div>
            <div className="detail-field"><label>Service Responsibility</label><span>{event.serviceResponsibility ?? '—'}</span></div>
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
                {(() => {
                  const base = (Number(event.wasteFeePrice ?? 0) + Number(event.transportFeePrice ?? 0) + Number(event.ecoFeePrice ?? 0))
                  const totalWith0 = base * (1 + Number(event.vatRate0 ?? 0) / 100)
                  const totalWith24 = base * (1 + Number(event.vatRate24 ?? 0) / 100)
                  return (
                    <>
                      <div className="detail-field"><label>Total with 0% VAT</label><span>{totalWith0.toFixed(2)}</span></div>
                      <div className="detail-field"><label>Total with 24% VAT</label><span>{totalWith24.toFixed(2)}</span></div>
                    </>
                  )
                })()}
              </div>
              {parentInvoiceId != null && (
                <div style={{ marginTop: 'var(--space-3)', padding: 'var(--space-3)', background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 'var(--radius-md)', display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: 13 }}>This event is included in invoice <strong>#{parentInvoiceId}</strong>. An invoice can aggregate multiple events with different VAT rates and cost centres.</span>
                  <button className="btn-secondary" style={{ padding: '2px 10px', fontSize: 13 }} onClick={() => navigate(`/invoices/${parentInvoiceId}`)}>
                    View Invoice #{parentInvoiceId}
                  </button>
                </div>
              )}
            </div>
          )}
          {event.priceOverridden && (
            <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 'var(--radius-md)' }}>
              <strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Price Override</strong>
              <div className="detail-grid">
                {event.originalWasteFeePrice != null && (
                  <div className="detail-field">
                    <label>Waste Fee</label>
                    <span>Product default: €{Number(event.originalWasteFeePrice).toFixed(2)} → <strong>€{Number(event.wasteFeePrice).toFixed(2)}</strong></span>
                  </div>
                )}
                {event.originalTransportFeePrice != null && (
                  <div className="detail-field">
                    <label>Transport Fee</label>
                    <span>Product default: €{Number(event.originalTransportFeePrice).toFixed(2)} → <strong>€{Number(event.transportFeePrice).toFixed(2)}</strong></span>
                  </div>
                )}
                {event.originalEcoFeePrice != null && (
                  <div className="detail-field">
                    <label>Eco Fee</label>
                    <span>Product default: €{Number(event.originalEcoFeePrice).toFixed(2)} → <strong>€{Number(event.ecoFeePrice).toFixed(2)}</strong></span>
                  </div>
                )}
              </div>
            </div>
          )}
          <ClassificationReasonSection event={event} />
          <FinvoiceDataSection event={event} />
          <ComponentBillingSection event={event} canEdit={canEdit} onUpdated={load} />
          {event.contractor && (
            <ContractorPaymentSection event={event} onUpdated={load} />
          )}
          {(event.comments || event.internalComments) && (
            <div style={{ marginTop: 'var(--space-6)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              {event.comments && (
                <div className="detail-field"><label>Invoice Comments</label><span>{event.comments}</span></div>
              )}
              {event.internalComments && (
                <div className="detail-field"><label>Internal Notes</label><span>{event.internalComments}</span></div>
              )}
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
          <ValidationStatusSection event={event} onUpdated={load} />
        </div>
      )}

      {activeTab === 'Audit Trail' && <AuditTrailTab eventId={id} />}

      {activeTab === 'Status' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
            <span className="muted">Current status:</span>
            <StatusBadge status={event.status} />
          </div>
          {event.status === 'ERROR' && event.transmissionErrorReason && (
            <div style={{ padding: 'var(--space-4)', background: '#fff5f5', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)' }}>
              <strong>Transmission error:</strong> {event.transmissionErrorReason}
            </div>
          )}
          <StatusTransitionPanel
            eventId={id}
            currentStatus={event.status}
            onTransitioned={load}
          />
          {!['DRAFT', 'SENT', 'ERROR'].includes(event.status) && (
            <p className="muted">No manual transitions available for this status.</p>
          )}

          <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: '#f5f3ff', border: '1px solid #c4b5fd', borderRadius: 'var(--radius-md)' }}>
            <strong style={{ display: 'block', marginBottom: 'var(--space-3)', fontSize: 13 }}>Dev — Simulate Transmission Outcome</strong>
            <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12 }}>Outcome</label>
                <select value={simOutcome} onChange={e => setSimOutcome(e.target.value)}
                  style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid #ccc' }}>
                  <option value="SENT">SENT</option>
                  <option value="ERROR">ERROR</option>
                  <option value="COMPLETED">COMPLETED</option>
                </select>
              </div>
              {simOutcome === 'ERROR' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12 }}>Error reason</label>
                  <input
                    value={simErrorReason}
                    onChange={e => setSimErrorReason(e.target.value)}
                    placeholder="e.g. Operator rejected the file"
                    style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid #ccc', width: 260 }}
                  />
                </div>
              )}
              <button
                className="btn-secondary"
                disabled={simLoading}
                onClick={async () => {
                  setSimLoading(true)
                  setSimError(null)
                  try {
                    await simulateTransmissionOutcome(id, simOutcome, simOutcome === 'ERROR' ? simErrorReason : undefined)
                    load()
                  } catch (e) {
                    setSimError(e.response?.data?.message ?? 'Simulation failed')
                  } finally {
                    setSimLoading(false)
                  }
                }}
              >
                Apply
              </button>
            </div>
            {simError && <div className="error-msg" style={{ marginTop: 'var(--space-2)' }}>{simError}</div>}
          </div>
        </div>
      )}

      {activeTab === 'Attachments' && <AttachmentsTab eventId={id} />}

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

// -----------------------------------------------------------------------
// FINVOICE Data Section (PD-299 AC4)
// -----------------------------------------------------------------------
function FinvoiceDataSection({ event }) {
  const [open, setOpen] = useState(false)

  const rows = [
    { finvoice: 'InvoiceRow/ArticleName',           value: event.product?.name ?? event.product?.code ?? '—' },
    { finvoice: 'InvoiceRow/DeliveredQuantity',      value: event.quantity ?? '—' },
    { finvoice: 'InvoiceRow/UnitPriceAmount (waste)',value: event.wasteFeePrice?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/UnitPriceAmount (transport)', value: event.transportFeePrice?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/UnitPriceAmount (eco)',  value: event.ecoFeePrice?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/VatRatePercent',         value: event.resolvedVatRatePercent != null ? `${event.resolvedVatRatePercent}%` : '—' },
    { finvoice: 'InvoiceRow/VatCode',                value: event.resolvedVatRateCode ?? '—' },
    { finvoice: 'InvoiceRow/RowVatAmount',           value: event.calculatedAmountVat?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/RowAmount (net)',        value: event.calculatedAmountNet?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/RowAmount (gross)',      value: event.calculatedAmountGross?.toFixed(2) ?? '—' },
    { finvoice: 'InvoiceRow/DeliveryDate',           value: event.eventDate ?? '—' },
    { finvoice: 'InvoiceRow/EanCode / ArticleIdentifier', value: event.product?.code ?? '—' },
    { finvoice: 'BuyerPartyIdentifier',              value: event.customerNumber ?? '—' },
    { finvoice: 'BuyerVatRegistrationText',          value: event.buyerVatNumber ?? '—' },
    { finvoice: 'AgreementIdentifier (registration)',value: event.registrationNumber ?? '—' },
    { finvoice: 'InvoiceRow/AccountDimensionText',   value: event.accountingAccount ? `${event.accountingAccount.code} — ${event.accountingAccount.name}` : '—' },
    { finvoice: 'InvoiceRow/CostObjectText',         value: event.costCenter?.compositeCode ?? event.resolvedCostCenterCode ?? '—' },
    { finvoice: 'RowIdentifier IdentifierType="ServiceResponsibility"', value: event.legalClassification ?? '—' },
    { finvoice: 'FreeText (internal ref)',            value: event.comments ?? '—' },
  ]

  const transmitted = ['SENT', 'COMPLETED'].includes(event.status)
  const pending = ['IN_PROGRESS', 'FOR_CORRECTION'].includes(event.status)

  return (
    <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', cursor: 'pointer' }} onClick={() => setOpen(o => !o)}>
        <strong>FINVOICE Material</strong>
        {transmitted && <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #bbf7d0', background: '#f0fdf4', color: '#15803d', fontSize: 12, fontWeight: 500 }}>Transmitted</span>}
        {pending && <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #fde047', background: '#fefce8', color: '#854d0e', fontSize: 12, fontWeight: 500 }}>Pending transmission</span>}
        {!transmitted && !pending && <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #e5e7eb', background: '#f9fafb', color: '#6b7280', fontSize: 12 }}>Not yet queued</span>}
        <span className="muted" style={{ marginLeft: 'auto', fontSize: 13 }}>{open ? 'Hide' : 'Show field mapping'}</span>
      </div>
      <p className="muted" style={{ marginTop: 'var(--space-2)', fontSize: 13 }}>
        When this event is included in an invoice, these field values are written into the FINVOICE 3.0 XML transmitted to the operator.
      </p>
      {open && (
        <table className="data-table" style={{ marginTop: 'var(--space-3)' }}>
          <thead>
            <tr>
              <th style={{ width: '45%' }}>FINVOICE 3.0 Element</th>
              <th>Value from this event</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.finvoice}>
                <td><code style={{ fontSize: 12 }}>{r.finvoice}</code></td>
                <td>{r.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

// -----------------------------------------------------------------------
// Selective Component Billing (AC3)
// -----------------------------------------------------------------------
function ComponentBillingSection({ event, canEdit, onUpdated }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [components, setComponents] = useState({
    includeWasteFee: event.includeWasteFee ?? true,
    includeTransportFee: event.includeTransportFee ?? true,
    includeEcoFee: event.includeEcoFee ?? true,
  })

  const handleToggle = (field) => {
    if (!canEdit) return
    setComponents(c => ({ ...c, [field]: !c[field] }))
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    try {
      await updateBillingEventComponents(event.id, components)
      onUpdated()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to update components.')
    } finally {
      setSaving(false)
    }
  }

  const changed = components.includeWasteFee !== (event.includeWasteFee ?? true)
    || components.includeTransportFee !== (event.includeTransportFee ?? true)
    || components.includeEcoFee !== (event.includeEcoFee ?? true)

  return (
    <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
      <strong style={{ display: 'block', marginBottom: 'var(--space-3)' }}>Component Billing</strong>
      <p className="muted" style={{ marginBottom: 'var(--space-3)', fontSize: 13 }}>
        Select which fee components should be included when this event is invoiced.
      </p>
      {error && <div className="error-msg" style={{ marginBottom: 8 }}>{error}</div>}
      <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap', marginBottom: 'var(--space-3)' }}>
        {[
          { key: 'includeWasteFee', label: 'Waste Fee', amount: event.wasteFeePrice },
          { key: 'includeTransportFee', label: 'Transport Fee', amount: event.transportFeePrice },
          { key: 'includeEcoFee', label: 'Eco Fee', amount: event.ecoFeePrice },
        ].map(({ key, label, amount }) => (
          <label key={key} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: canEdit ? 'pointer' : 'default', padding: 'var(--space-3)', border: `1px solid ${components[key] ? 'var(--color-border)' : '#fecaca'}`, borderRadius: 'var(--radius-md)', background: components[key] ? 'white' : '#fff5f5' }}>
            <input
              type="checkbox"
              checked={components[key]}
              onChange={() => handleToggle(key)}
              disabled={!canEdit}
            />
            <span>
              <span style={{ fontWeight: 500 }}>{label}</span>
              {amount != null && <span className="muted"> — €{Number(amount).toFixed(2)}</span>}
            </span>
          </label>
        ))}
      </div>
      {canEdit && changed && (
        <button className="btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save Component Selection'}
        </button>
      )}
    </div>
  )
}

// -----------------------------------------------------------------------
// Contractor Payment (AC5)
// -----------------------------------------------------------------------
function ContractorPaymentSection({ event, onUpdated }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [notes, setNotes] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [pendingStatus, setPendingStatus] = useState('PAID')

  const handleRecord = async () => {
    setSaving(true)
    setError(null)
    try {
      await recordContractorPayment(event.id, { status: pendingStatus, notes })
      setShowForm(false)
      setNotes('')
      onUpdated()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to record contractor payment.')
    } finally {
      setSaving(false)
    }
  }

  const statusColor = {
    PAID: { color: 'var(--color-status-active-text)', bg: 'var(--color-status-active-bg)', border: 'var(--color-status-active-border)' },
    NOT_REQUIRED: { color: '#475569', bg: '#f8fafc', border: '#cbd5e1' },
    PENDING: { color: '#92400e', bg: '#fffbeb', border: '#fcd34d' },
  }

  const current = event.contractorPaymentStatus ?? 'PENDING'
  const style = statusColor[current] ?? statusColor.PENDING

  return (
    <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 'var(--radius-md)' }}>
      <strong style={{ display: 'block', marginBottom: 'var(--space-3)' }}>Contractor Payment</strong>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
        <span className="muted">Contractor:</span>
        <span>{event.contractor}</span>
        <span style={{ padding: '2px 10px', borderRadius: 12, border: `1px solid ${style.border}`, background: style.bg, color: style.color, fontSize: 13, fontWeight: 500 }}>
          {current.replace('_', ' ')}
        </span>
      </div>
      {event.contractorPaymentNotes && (
        <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>{event.contractorPaymentNotes}</p>
      )}
      {event.contractorPaymentRecordedBy && (
        <p className="muted" style={{ fontSize: 12 }}>
          Recorded by {event.contractorPaymentRecordedBy} at {formatTs(event.contractorPaymentRecordedAt)}
        </p>
      )}
      {!showForm && current === 'PENDING' && (
        <button className="btn-secondary" style={{ marginTop: 'var(--space-3)' }} onClick={() => setShowForm(true)}>
          Record Payment Decision
        </button>
      )}
      {showForm && (
        <div style={{ marginTop: 'var(--space-3)', display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
          {error && <div className="error-msg">{error}</div>}
          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
            <select value={pendingStatus} onChange={e => setPendingStatus(e.target.value)}
              style={{ padding: '6px 12px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)' }}>
              <option value="PAID">Paid</option>
              <option value="NOT_REQUIRED">Not Required</option>
            </select>
          </div>
          <textarea
            value={notes}
            onChange={e => setNotes(e.target.value)}
            rows={2}
            placeholder="Notes (optional)"
            style={{ width: '100%', padding: 'var(--space-2) var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', resize: 'vertical', boxSizing: 'border-box' }}
          />
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            <button className="btn-primary" onClick={handleRecord} disabled={saving}>
              {saving ? 'Saving…' : 'Confirm'}
            </button>
            <button className="btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  )
}

function ValidationStatusSection({ event, onUpdated }) {
  const [overrideOpen, setOverrideOpen] = useState(false)
  const [overrideReason, setOverrideReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [validating, setValidating] = useState(false)
  const [error, setError] = useState(null)
  const [failures, setFailures] = useState([])

  useEffect(() => {
    if (event.validationStatus === 'FAILED') {
      getBillingEventValidationFailures(event.id)
        .then(r => setFailures(r.data))
        .catch(() => {})
    } else {
      setFailures([])
    }
  }, [event.id, event.validationStatus])

  const status = event.validationStatus ?? 'PENDING'
  const statusCfg = {
    PENDING:    { color: '#6b7280', bg: '#f8fafc', border: '#cbd5e1', label: 'Not yet validated' },
    PASSED:     { color: 'var(--color-status-active-text)', bg: 'var(--color-status-active-bg)', border: 'var(--color-status-active-border)', label: 'Passed' },
    FAILED:     { color: '#b91c1c', bg: '#fff1f2', border: '#fecdd3', label: 'Failed' },
    OVERRIDDEN: { color: '#b45309', bg: '#fffbeb', border: '#fcd34d', label: 'Overridden' },
  }[status] ?? { color: '#6b7280', bg: '#f8fafc', border: '#cbd5e1', label: status }

  const handleValidate = async () => {
    setValidating(true)
    setError(null)
    try {
      await validateBillingEvents([event.id])
      onUpdated()
    } catch {
      setError('Validation failed.')
    } finally {
      setValidating(false)
    }
  }

  const handleOverride = async () => {
    if (!overrideReason.trim()) return
    setSaving(true)
    setError(null)
    try {
      await overrideValidation(event.id, overrideReason)
      setOverrideOpen(false)
      setOverrideReason('')
      onUpdated()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Override failed.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
      <strong style={{ display: 'block', marginBottom: 'var(--space-3)' }}>Validation Status</strong>
      {error && <div className="error-msg" style={{ marginBottom: 8 }}>{error}</div>}
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
        <span style={{ padding: '2px 12px', borderRadius: 12, border: `1px solid ${statusCfg.border}`, background: statusCfg.bg, color: statusCfg.color, fontSize: 13, fontWeight: 500 }}>
          {statusCfg.label}
        </span>
        {event.lastValidatedAt && (
          <span className="muted" style={{ fontSize: 13 }}>Last validated: {new Date(event.lastValidatedAt).toLocaleString('fi-FI')}</span>
        )}
        <button className="btn-secondary" onClick={handleValidate} disabled={validating} style={{ marginLeft: 'auto' }}>
          {validating ? 'Validating…' : 'Run Validation'}
        </button>
      </div>
      {status === 'FAILED' && (
        <div style={{ marginTop: 'var(--space-3)', padding: 'var(--space-3)', background: '#fff1f2', border: '1px solid #fecdd3', borderRadius: 'var(--radius-md)' }}>
          <p style={{ marginBottom: 'var(--space-2)', fontSize: 13 }}>
            This event has validation failures. Fix the flagged fields, or override with a documented reason if the failures are acceptable.
          </p>
          <ValidationFailuresList failures={failures} />
          {!overrideOpen && (
            <button className="btn-secondary" style={{ marginTop: 'var(--space-2)' }} onClick={() => setOverrideOpen(true)}>Override Validation</button>
          )}
          {overrideOpen && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
              <label style={{ fontWeight: 500, fontSize: 13 }}>
                Override reason <span style={{ color: 'var(--color-icon-danger)' }}>*</span>
              </label>
              <textarea
                value={overrideReason}
                onChange={e => setOverrideReason(e.target.value)}
                rows={3}
                placeholder="Document why proceeding despite validation failures is acceptable…"
                style={{ width: '100%', padding: 'var(--space-2) var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', resize: 'vertical', boxSizing: 'border-box' }}
              />
              <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                <button className="btn-primary" onClick={handleOverride} disabled={!overrideReason.trim() || saving}>
                  {saving ? 'Saving…' : 'Confirm Override'}
                </button>
                <button className="btn-secondary" onClick={() => { setOverrideOpen(false); setOverrideReason('') }}>Cancel</button>
              </div>
            </div>
          )}
        </div>
      )}
      {status === 'OVERRIDDEN' && event.validationOverrideReason && (
        <div style={{ marginTop: 'var(--space-3)', padding: 'var(--space-3)', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 'var(--radius-md)' }}>
          <strong>Override reason:</strong> {event.validationOverrideReason}
          {event.validationOverriddenBy && (
            <span className="muted" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>
              by {event.validationOverriddenBy} at {event.validationOverriddenAt ? new Date(event.validationOverriddenAt).toLocaleString('fi-FI') : '—'}
            </span>
          )}
        </div>
      )}
    </div>
  )
}

function AttachmentsTab({ eventId }) {
  const [attachments, setAttachments] = useState([])
  const [uploading, setUploading]     = useState(false)
  const [error, setError]             = useState(null)

  const loadAttachments = () => {
    getBillingEventAttachments(eventId)
      .then(r => setAttachments(r.data))
      .catch(() => {})
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { loadAttachments() }, [eventId])

  const handleUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      await uploadBillingEventAttachment(eventId, file)
      loadAttachments()
    } catch (err) {
      setError(err.response?.data ?? 'Upload failed.')
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDelete = async (attachmentId) => {
    if (!window.confirm('Delete this attachment?')) return
    try {
      await deleteBillingEventAttachment(eventId, attachmentId)
      loadAttachments()
    } catch {
      setError('Delete failed.')
    }
  }

  const handleDownload = async (att) => {
    try {
      const res = await downloadBillingEventAttachment(eventId, att.id)
      const url = URL.createObjectURL(res.data)
      const a   = document.createElement('a')
      a.href = url
      a.download = att.fileName
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Download failed.')
    }
  }

  const fmt = (bytes) => {
    if (bytes < 1024)       return `${bytes} B`
    if (bytes < 1048576)    return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1048576).toFixed(1)} MB`
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
      {error && <div className="error-msg">{error}</div>}

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
        <label className="btn-secondary" style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          {uploading ? 'Uploading…' : '+ Upload File'}
          <input type="file" style={{ display: 'none' }} onChange={handleUpload} disabled={uploading} />
        </label>
        <span className="muted" style={{ fontSize: 13 }}>Max 10 MB per file · up to 10 attachments</span>
      </div>

      {attachments.length === 0 ? (
        <p className="muted">No attachments yet.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>File Name</th>
              <th>Type</th>
              <th>Size</th>
              <th>Uploaded</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {attachments.map(att => (
              <tr key={att.id}>
                <td>{att.fileName}</td>
                <td><code style={{ fontSize: 12 }}>{att.contentType}</code></td>
                <td>{fmt(att.fileSize)}</td>
                <td>{att.createdAt ? new Date(att.createdAt).toLocaleString('fi-FI') : '—'}</td>
                <td>
                  <div className="actions">
                    <button className="btn-secondary" onClick={() => handleDownload(att)}>Download</button>
                    <button className="btn-danger"    onClick={() => handleDelete(att.id)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

// -----------------------------------------------------------------------
// Classification Reason Section (PD-284 AC9)
// -----------------------------------------------------------------------
function ClassificationReasonSection({ event }) {
  const [rules, setRules] = useState([])
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (open) {
      getClassificationRules(1).then(r => setRules(r.data ?? [])).catch(() => {})
    }
  }, [open])

  if (!event.legalClassification) return null

  const matchedRule = rules.find(rule => {
    const productCode = event.product?.code ?? null
    const customerType = null
    const region = event.municipalityId ?? null
    if (rule.productCodeCondition && rule.productCodeCondition !== productCode) return false
    if (rule.regionCondition && rule.regionCondition !== region) return false
    if (rule.customerTypeCondition) return false
    return rule.resultClassification === event.legalClassification && rule.active
  }) ?? rules.find(rule => rule.resultClassification === event.legalClassification && rule.active)

  return (
    <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: '#f5f3ff', border: '1px solid #c4b5fd', borderRadius: 'var(--radius-md)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', cursor: 'pointer' }} onClick={() => setOpen(o => !o)}>
        <strong>Classification Compliance</strong>
        <span style={{ padding: '2px 10px', borderRadius: 12, border: '1px solid #c4b5fd', background: '#ede9fe', color: '#6d28d9', fontSize: 12, fontWeight: 500 }}>
          {event.legalClassification.replace('_', ' ')}
        </span>
        <span className="muted" style={{ marginLeft: 'auto', fontSize: 13 }}>{open ? 'Hide details' : 'Show matched rule'}</span>
      </div>
      <p className="muted" style={{ marginTop: 'var(--space-2)', fontSize: 13 }}>
        This event was automatically classified as <strong>{event.legalClassification.replace('_', ' ')}</strong> by the legal classification rule engine.
      </p>
      {open && (
        <div style={{ marginTop: 'var(--space-3)' }}>
          {rules.length === 0 ? (
            <p className="muted" style={{ fontSize: 13 }}>Loading classification rules…</p>
          ) : matchedRule ? (
            <div style={{ padding: 'var(--space-3)', background: '#ede9fe', border: '1px solid #c4b5fd', borderRadius: 'var(--radius-md)', fontSize: 13 }}>
              <strong>Matched rule:</strong>{' '}
              {matchedRule.label ? `"${matchedRule.label}" — ` : ''}
              Priority {matchedRule.priority}
              {matchedRule.customerTypeCondition && ` · Customer type: ${matchedRule.customerTypeCondition}`}
              {matchedRule.productCodeCondition && ` · Product: ${matchedRule.productCodeCondition}`}
              {matchedRule.regionCondition && ` · Region: ${matchedRule.regionCondition}`}
              {' '}→ <strong>{matchedRule.resultClassification.replace('_', ' ')}</strong>
            </div>
          ) : (
            <div style={{ padding: 'var(--space-3)', background: '#fef9c3', border: '1px solid #fde047', borderRadius: 'var(--radius-md)', fontSize: 13 }}>
              No specific rule match found — classification applied via company default or manual assignment.
              {rules.length > 0 && (
                <span className="muted"> ({rules.length} active rules loaded)</span>
              )}
            </div>
          )}
          <p style={{ color: '#6b7280', fontSize: 12, marginTop: 8 }}>
            Rules are evaluated in priority order. The first matching rule determines the classification.
          </p>
        </div>
      )}
    </div>
  )
}
