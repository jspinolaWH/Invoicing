import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createManualBillingEvent } from '../../api/billingEvents'
import { getProducts } from '../../api/products'
import { useResolvedVatRate } from '../../hooks/useResolvedVatRate'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-283', label: '3.4.30 Manual creation of billing events', href: 'https://ioteelab.atlassian.net/browse/PD-283' },
  { id: 'PD-299', label: '3.4.13 Billing event details',             href: 'https://ioteelab.atlassian.net/browse/PD-299' },
]

export default function CreateBillingEventPage() {
  const navigate = useNavigate()
  const [products, setProducts] = useState([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const [form, setForm] = useState({
    eventDate: '', productId: '', wasteFeePrice: '', transportFeePrice: '',
    ecoFeePrice: '', quantity: '', weight: '', customerNumber: '',
    vehicleId: '', driverId: '', locationId: '', municipalityId: '', comments: '',
  })
  const [fieldErrors, setFieldErrors] = useState({})

  const vatRates = useResolvedVatRate(form.productId, form.eventDate)

  useEffect(() => {
    getProducts().then(r => setProducts(r.data)).catch(() => {})
  }, [])

  const set = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }))
    setFieldErrors(fe => ({ ...fe, [field]: null }))
  }

  const validate = () => {
    const errs = {}
    if (!form.eventDate) errs.eventDate = 'Required'
    if (!form.productId) errs.productId = 'Required'
    if (!form.wasteFeePrice) errs.wasteFeePrice = 'Required'
    if (!form.transportFeePrice) errs.transportFeePrice = 'Required'
    if (!form.ecoFeePrice) errs.ecoFeePrice = 'Required'
    if (!form.quantity) errs.quantity = 'Required'
    if (!form.weight) errs.weight = 'Required'
    if (!form.customerNumber) errs.customerNumber = 'Required'
    else if (!/^\d{6,9}$/.test(form.customerNumber)) errs.customerNumber = 'Must be 6-9 digits'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setFieldErrors(errs); return }
    setSaving(true)
    setError(null)
    try {
      const res = await createManualBillingEvent({
        ...form,
        productId: Number(form.productId),
        wasteFeePrice: Number(form.wasteFeePrice),
        transportFeePrice: Number(form.transportFeePrice),
        ecoFeePrice: Number(form.ecoFeePrice),
        quantity: Number(form.quantity),
        weight: Number(form.weight),
      })
      navigate(`/billing-events/${res.data.id}`)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to create billing event.')
    } finally {
      setSaving(false)
    }
  }

  const activeRate = vatRates?.find(r => r.rate > 0)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Create Billing Event</h1>
          <p>Manual creation — all financial metadata is auto-resolved from the selected product and date.</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate('/billing-events')}>← Back</button>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      <form onSubmit={handleSubmit}>
        {/* Section 1 — Event Details */}
        <div className="form-section">
          <div className="form-section-title">Event Details</div>
          <div className="form-row">
            <div className="field">
              <label>Date <span className="required">*</span></label>
              <input type="date" value={form.eventDate} onChange={set('eventDate')}
                className={fieldErrors.eventDate ? 'input-error' : ''} />
              {fieldErrors.eventDate && <span className="field-error">{fieldErrors.eventDate}</span>}
            </div>
            <div className="field">
              <label>Product <span className="required">*</span></label>
              <select value={form.productId} onChange={set('productId')}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}
                className={fieldErrors.productId ? 'input-error' : ''}>
                <option value="">Select product…</option>
                {products.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.translations?.find(t => t.locale === 'en')?.name ?? p.code}
                  </option>
                ))}
              </select>
              {fieldErrors.productId && <span className="field-error">{fieldErrors.productId}</span>}
            </div>
          </div>
        </div>

        {/* Auto-resolved preview */}
        {form.productId && form.eventDate && (
          <div className="form-section">
            <div className="form-section-title">Auto-Resolved Values <span className="optional">(read-only)</span></div>
            <div className="resolved-preview">
              <div className="resolved-item">
                <label>VAT Rate</label>
                <span>{activeRate ? `${activeRate.rate}%` : 'Not found'}</span>
              </div>
            </div>
            <p className="readonly-info">Accounting account, cost center, and legal classification are resolved automatically at save time.</p>
          </div>
        )}

        {/* Section 2 — Pricing */}
        <div className="form-section">
          <div className="form-section-title">Pricing</div>
          <div className="form-row-3">
            {['wasteFeePrice', 'transportFeePrice', 'ecoFeePrice'].map(field => (
              <div className="field" key={field}>
                <label>{field === 'wasteFeePrice' ? 'Waste Fee' : field === 'transportFeePrice' ? 'Transport Fee' : 'Eco Fee'} <span className="required">*</span></label>
                <input type="number" min="0" step="0.01" value={form[field]} onChange={set(field)}
                  className={fieldErrors[field] ? 'input-error' : ''} />
                {fieldErrors[field] && <span className="field-error">{fieldErrors[field]}</span>}
              </div>
            ))}
          </div>
        </div>

        {/* Section 3 — Quantity */}
        <div className="form-section">
          <div className="form-section-title">Quantity & Weight</div>
          <div className="form-row">
            <div className="field">
              <label>Quantity <span className="required">*</span></label>
              <input type="number" min="0" step="0.01" value={form.quantity} onChange={set('quantity')}
                className={fieldErrors.quantity ? 'input-error' : ''} />
              {fieldErrors.quantity && <span className="field-error">{fieldErrors.quantity}</span>}
            </div>
            <div className="field">
              <label>Weight <span className="required">*</span></label>
              <input type="number" min="0" step="0.001" value={form.weight} onChange={set('weight')}
                className={fieldErrors.weight ? 'input-error' : ''} />
              {fieldErrors.weight && <span className="field-error">{fieldErrors.weight}</span>}
            </div>
          </div>
        </div>

        {/* Section 4 — Customer & Location */}
        <div className="form-section">
          <div className="form-section-title">Customer & Location</div>
          <div className="form-row">
            <div className="field">
              <label>Customer Number <span className="required">*</span></label>
              <input value={form.customerNumber} onChange={set('customerNumber')} placeholder="6-9 digits"
                className={fieldErrors.customerNumber ? 'input-error' : ''} />
              {fieldErrors.customerNumber && <span className="field-error">{fieldErrors.customerNumber}</span>}
            </div>
            <div className="field">
              <label>Municipality <span className="optional">(optional)</span></label>
              <input value={form.municipalityId} onChange={set('municipalityId')} />
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Vehicle ID <span className="optional">(optional)</span></label>
              <input value={form.vehicleId} onChange={set('vehicleId')} />
            </div>
            <div className="field">
              <label>Driver ID <span className="optional">(optional)</span></label>
              <input value={form.driverId} onChange={set('driverId')} />
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Location ID <span className="optional">(optional)</span></label>
              <input value={form.locationId} onChange={set('locationId')} />
            </div>
          </div>
        </div>

        {/* Section 5 — Notes */}
        <div className="form-section">
          <div className="form-section-title">Notes</div>
          <div className="field">
            <label>Comments <span className="optional">(optional)</span></label>
            <textarea value={form.comments} onChange={set('comments')}
              style={{ width: '100%', minHeight: 80, padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical' }} />
          </div>
        </div>

        <div className="modal-footer" style={{ borderTop: 'none', padding: '0 0 var(--space-6)' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate('/billing-events')}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? 'Creating…' : 'Create Event'}
          </button>
        </div>
      </form>
    </div>
  )
}
