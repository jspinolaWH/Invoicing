import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getBillingEvent, updateBillingEvent } from '../../api/billingEvents'
import { getProducts } from '../../api/products'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-277', label: '3.4.36 Manual editing of events', href: 'https://ioteelab.atlassian.net/browse/PD-277' },
  { id: 'PD-318', label: '3.3.18 Editing billing events',   href: 'https://ioteelab.atlassian.net/browse/PD-318' },
]

export default function EditBillingEventPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [event, setEvent] = useState(null)
  const [products, setProducts] = useState([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  const [form, setForm] = useState({
    eventDate: '', productId: '', wasteFeePrice: '', transportFeePrice: '',
    ecoFeePrice: '', quantity: '', weight: '', customerNumber: '',
    vehicleId: '', driverId: '', locationId: '', municipalityId: '', comments: '', reason: '',
  })

  useEffect(() => {
    Promise.all([getBillingEvent(id), getProducts()])
      .then(([evtRes, prodRes]) => {
        const e = evtRes.data
        setEvent(e)
        setProducts(prodRes.data)
        setForm({
          eventDate: e.eventDate ?? '',
          productId: e.product?.id ?? '',
          wasteFeePrice: e.wasteFeePrice ?? '',
          transportFeePrice: e.transportFeePrice ?? '',
          ecoFeePrice: e.ecoFeePrice ?? '',
          quantity: e.quantity ?? '',
          weight: e.weight ?? '',
          customerNumber: e.customerNumber ?? '',
          vehicleId: e.vehicleId ?? '',
          driverId: e.driverId ?? '',
          locationId: e.locationId ?? '',
          municipalityId: e.municipalityId ?? '',
          comments: e.comments ?? '',
          reason: '',
        })
      })
      .catch(() => setError('Failed to load event.'))
  }, [id])

  const set = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }))
    setFieldErrors(fe => ({ ...fe, [field]: null }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.reason.trim()) { setFieldErrors({ reason: 'A reason is required for every edit' }); return }
    setSaving(true)
    setError(null)
    try {
      const payload = {}
      if (form.reason) payload.reason = form.reason
      if (form.eventDate !== event.eventDate) payload.eventDate = form.eventDate
      if (String(form.productId) !== String(event.product?.id)) payload.productId = Number(form.productId)
      if (String(form.wasteFeePrice) !== String(event.wasteFeePrice)) payload.wasteFeePrice = Number(form.wasteFeePrice)
      if (String(form.transportFeePrice) !== String(event.transportFeePrice)) payload.transportFeePrice = Number(form.transportFeePrice)
      if (String(form.ecoFeePrice) !== String(event.ecoFeePrice)) payload.ecoFeePrice = Number(form.ecoFeePrice)
      if (String(form.quantity) !== String(event.quantity)) payload.quantity = Number(form.quantity)
      if (String(form.weight) !== String(event.weight)) payload.weight = Number(form.weight)
      if (form.customerNumber !== event.customerNumber) payload.customerNumber = form.customerNumber
      if (form.vehicleId !== (event.vehicleId ?? '')) payload.vehicleId = form.vehicleId
      if (form.driverId !== (event.driverId ?? '')) payload.driverId = form.driverId
      if (form.locationId !== (event.locationId ?? '')) payload.locationId = form.locationId
      if (form.municipalityId !== (event.municipalityId ?? '')) payload.municipalityId = form.municipalityId
      if (form.comments !== (event.comments ?? '')) payload.comments = form.comments
      await updateBillingEvent(id, payload)
      navigate(`/billing-events/${id}`)
    } catch (err) {
      if (err.response?.status === 409) {
        setError('This event has already been sent and cannot be edited. Use the credit-and-re-invoice flow.')
      } else {
        setError(err.response?.data?.message ?? 'Failed to update billing event.')
      }
    } finally {
      setSaving(false)
    }
  }

  if (!event) return <div className="loading">Loading event…</div>

  const isMutable = event.status === 'IN_PROGRESS' || event.status === 'ERROR'

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Edit Billing Event #{id}</h1>
          <p>All changes are logged with a mandatory reason.</p>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'center' }}>
          <StatusBadge status={event.status} />
          <button className="btn-secondary" onClick={() => navigate(`/billing-events/${id}`)}>← Back</button>
        </div>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {!isMutable && (
        <div className="error-msg">
          This event is <strong>{event.status}</strong> and cannot be edited. Use the credit-and-re-invoice flow for corrections.
        </div>
      )}

      {error && <div className="error-msg">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-section">
          <div className="form-section-title">Event Details</div>
          <div className="form-row">
            <div className="field">
              <label>Date</label>
              <input type="date" value={form.eventDate} onChange={set('eventDate')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Product</label>
              <select value={form.productId} onChange={set('productId')} disabled={!isMutable}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}>
                {products.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.translations?.find(t => t.locale === 'en')?.name ?? p.code}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-section-title">Read-only Fields</div>
          <div className="resolved-preview">
            <div className="resolved-item">
              <label>Accounting Account</label>
              <span>{event.accountingAccount ? `${event.accountingAccount.code} — ${event.accountingAccount.name}` : '—'}</span>
            </div>
            <div className="resolved-item">
              <label>Cost Center</label>
              <span>{event.costCenter?.compositeCode ?? '—'}</span>
            </div>
            <div className="resolved-item">
              <label>Classification</label>
              <span>{event.legalClassification ?? '—'}</span>
            </div>
            <div className="resolved-item">
              <label>VAT 0%</label>
              <span>{event.vatRate0}</span>
            </div>
            <div className="resolved-item">
              <label>VAT 24%</label>
              <span>{event.vatRate24}</span>
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-section-title">Pricing</div>
          <div className="form-row-3">
            <div className="field">
              <label>Waste Fee</label>
              <input type="number" step="0.01" value={form.wasteFeePrice} onChange={set('wasteFeePrice')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Transport Fee</label>
              <input type="number" step="0.01" value={form.transportFeePrice} onChange={set('transportFeePrice')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Eco Fee</label>
              <input type="number" step="0.01" value={form.ecoFeePrice} onChange={set('ecoFeePrice')} disabled={!isMutable} />
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-section-title">Quantity & Weight</div>
          <div className="form-row">
            <div className="field">
              <label>Quantity</label>
              <input type="number" step="0.01" value={form.quantity} onChange={set('quantity')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Weight</label>
              <input type="number" step="0.001" value={form.weight} onChange={set('weight')} disabled={!isMutable} />
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-section-title">Customer & Location</div>
          <div className="form-row">
            <div className="field">
              <label>Customer Number</label>
              <input value={form.customerNumber} onChange={set('customerNumber')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Municipality</label>
              <input value={form.municipalityId} onChange={set('municipalityId')} disabled={!isMutable} />
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Vehicle ID</label>
              <input value={form.vehicleId} onChange={set('vehicleId')} disabled={!isMutable} />
            </div>
            <div className="field">
              <label>Driver ID</label>
              <input value={form.driverId} onChange={set('driverId')} disabled={!isMutable} />
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-section-title">Comments</div>
          <div className="field">
            <textarea value={form.comments} onChange={set('comments')} disabled={!isMutable}
              style={{ width: '100%', minHeight: 80, padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical' }} />
          </div>
        </div>

        {isMutable && (
          <div className="form-section" style={{ borderColor: 'var(--color-icon-danger)' }}>
            <div className="form-section-title">Reason for Edit <span className="required">*</span></div>
            <div className="field">
              <textarea
                value={form.reason}
                onChange={set('reason')}
                placeholder="Mandatory: explain why this change is being made…"
                className={fieldErrors.reason ? 'input-error' : ''}
                style={{ width: '100%', minHeight: 80, padding: 'var(--space-3)', border: `1px solid ${fieldErrors.reason ? 'var(--color-icon-danger)' : 'var(--color-border-input)'}`, borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical' }}
              />
              {fieldErrors.reason && <span className="field-error">{fieldErrors.reason}</span>}
            </div>
          </div>
        )}

        <div className="modal-footer" style={{ borderTop: 'none', padding: '0 0 var(--space-6)' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate(`/billing-events/${id}`)}>Cancel</button>
          {isMutable && (
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
          )}
        </div>
      </form>
    </div>
  )
}
