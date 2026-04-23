import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { createManualBillingEvent, createDraftBillingEvent } from '../../api/billingEvents'
import { getContractsForCustomer, getProductsForContract } from '../../api/contracts'
import { getBillingEventTemplates, createBillingEventTemplate } from '../../api/billingEventTemplates'
import { getPropertyGroups } from '../../api/propertyGroups'
import { useResolvedVatRate } from '../../hooks/useResolvedVatRate'
import CustomerSearchInput from '../../components/billing/CustomerSearchInput'
import SearchableAutocomplete from '../../components/SearchableAutocomplete'
import RelatedTasks from '../../components/RelatedTasks'
import { searchVehicles } from '../../api/vehicles'
import { searchDrivers } from '../../api/drivers'
import { searchLocations, searchMunicipalities } from '../../api/locations'
import { searchWasteTypes } from '../../api/wasteTypes'
import { searchReceivingSites } from '../../api/receivingSites'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-283', label: '3.4.30 Manual creation of billing events', href: 'https://ioteelab.atlassian.net/browse/PD-283' },
  { id: 'PD-299', label: '3.4.13 Billing event details',             href: 'https://ioteelab.atlassian.net/browse/PD-299' },
]

const EMPTY_FORM = {
  eventDate: '', productId: '', wasteFeePrice: '', transportFeePrice: '',
  ecoFeePrice: '', quantity: '', weight: '', customerNumber: '',
  vehicleId: '', driverId: '', locationId: '', municipalityId: '', comments: '',
  contractor: '', direction: '', sharedServiceGroupId: '', sharedServiceGroupPercentage: '',
  wasteType: '', receivingSite: '',
}

export default function CreateBillingEventPage() {
  const navigate  = useNavigate()
  const location  = useLocation()

  // ── undo/redo history ────────────────────────────────────────────────────
  const [history, setHistory]   = useState([EMPTY_FORM])
  const [histIdx, setHistIdx]   = useState(0)
  const form = history[histIdx]

  // ── supporting state ─────────────────────────────────────────────────────
  const [products, setProducts]       = useState([])
  const [contracts, setContracts]     = useState([])
  const [contractId, setContractId]   = useState('')
  const [saving, setSaving]           = useState(false)
  const [error, setError]             = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [draftIntent, setDraftIntent] = useState(false)

  // ── property groups ───────────────────────────────────────────────────────
  const [propertyGroups, setPropertyGroups] = useState([])

  // ── templates ────────────────────────────────────────────────────────────
  const [templates, setTemplates]         = useState([])
  const [savingTemplate, setSavingTemplate] = useState(false)
  const [templateName, setTemplateName]   = useState('')
  const [showSaveTemplate, setShowSaveTemplate] = useState(false)

  const vatRates = useResolvedVatRate(form.productId, form.eventDate)
  const selectedProduct = products.find(p => String(p.id) === String(form.productId))

  // ── load templates and property groups on mount ───────────────────────────
  useEffect(() => {
    getBillingEventTemplates().then(r => setTemplates(r.data)).catch(() => {})
    getPropertyGroups().then(r => setPropertyGroups(r.data)).catch(() => {})
  }, [])

  // ── apply template from navigation state (from templates page "Use") ──────
  useEffect(() => {
    const tpl = location.state?.template
    if (!tpl) return
    applyTemplate(tpl)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ── keyboard shortcuts ────────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
        e.preventDefault()
        setHistIdx(i => Math.max(0, i - 1))
      }
      if ((e.ctrlKey || e.metaKey) && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) {
        e.preventDefault()
        setHistIdx(i => Math.min(history.length - 1, i + 1))
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [history.length])

  // ── push a new form state onto the history stack ──────────────────────────
  const pushForm = useCallback((updater) => {
    setHistory(prev => {
      const current = prev[histIdx]
      const next    = typeof updater === 'function' ? updater(current) : updater
      const trimmed = prev.slice(0, histIdx + 1)
      return [...trimmed, next]
    })
    setHistIdx(i => i + 1)
  }, [histIdx])

  const set = (field) => (e) => {
    pushForm(f => ({ ...f, [field]: e.target.value }))
    setFieldErrors(fe => ({ ...fe, [field]: null }))
  }

  const handleCustomerSelect = async (customerNumber) => {
    pushForm(f => ({ ...f, customerNumber, productId: '', wasteFeePrice: '', transportFeePrice: '', ecoFeePrice: '' }))
    setFieldErrors(fe => ({ ...fe, customerNumber: null }))
    setContractId('')
    setContracts([])
    setProducts([])
    if (customerNumber) {
      try {
        const res = await getContractsForCustomer(customerNumber)
        setContracts(res.data)
      } catch {}
    }
  }

  const handleContractChange = async (e) => {
    const newContractId = e.target.value
    setContractId(newContractId)
    pushForm(f => ({ ...f, productId: '', wasteFeePrice: '', transportFeePrice: '', ecoFeePrice: '' }))
    setProducts([])
    if (newContractId) {
      try {
        const res = await getProductsForContract(newContractId)
        setProducts(res.data)
      } catch {}
    }
  }

  const handleProductChange = (e) => {
    const productId = e.target.value
    const selected  = products.find(p => String(p.id) === String(productId))
    pushForm(f => ({
      ...f,
      productId,
      ...(selected && {
        wasteFeePrice:     selected.defaultWasteFee     != null ? String(selected.defaultWasteFee)     : f.wasteFeePrice,
        transportFeePrice: selected.defaultTransportFee != null ? String(selected.defaultTransportFee) : f.transportFeePrice,
        ecoFeePrice:       selected.defaultEcoFee       != null ? String(selected.defaultEcoFee)       : f.ecoFeePrice,
      }),
    }))
    setFieldErrors(fe => ({ ...fe, productId: null, wasteFeePrice: null, transportFeePrice: null, ecoFeePrice: null }))
  }

  const applyTemplate = async (tpl) => {
    const next = {
      ...EMPTY_FORM,
      customerNumber:              tpl.customerNumber              ?? '',
      productId:                   tpl.productId != null           ? String(tpl.productId) : '',
      wasteFeePrice:               tpl.wasteFeePrice != null       ? String(tpl.wasteFeePrice) : '',
      transportFeePrice:           tpl.transportFeePrice != null   ? String(tpl.transportFeePrice) : '',
      ecoFeePrice:                 tpl.ecoFeePrice != null         ? String(tpl.ecoFeePrice) : '',
      quantity:                    tpl.quantity != null            ? String(tpl.quantity) : '',
      weight:                      tpl.weight != null              ? String(tpl.weight) : '',
      vehicleId:                   tpl.vehicleId                   ?? '',
      driverId:                    tpl.driverId                    ?? '',
      locationId:                  tpl.locationId                  ?? '',
      municipalityId:              tpl.municipalityId              ?? '',
      comments:                    tpl.comments                    ?? '',
      contractor:                  tpl.contractor                  ?? '',
      direction:                   tpl.direction                   ?? '',
      sharedServiceGroupId:         tpl.sharedServiceGroupId != null
                                      ? String(tpl.sharedServiceGroupId) : '',
      sharedServiceGroupPercentage: tpl.sharedServiceGroupPercentage != null
                                      ? String(tpl.sharedServiceGroupPercentage) : '',
      wasteType:                   tpl.wasteType                   ?? '',
      receivingSite:               tpl.receivingSite               ?? '',
    }
    pushForm(next)
    setContractId('')
    setContracts([])
    setProducts([])

    if (tpl.customerNumber) {
      try {
        const res = await getContractsForCustomer(tpl.customerNumber)
        setContracts(res.data)
        if (tpl.contractId) {
          const cid = String(tpl.contractId)
          setContractId(cid)
          const pr = await getProductsForContract(cid)
          setProducts(pr.data)
        }
      } catch {}
    }
  }

  const validate = () => {
    const errs = {}
    if (!form.eventDate)        errs.eventDate        = 'Required'
    if (!form.productId)        errs.productId        = 'Required'
    if (!form.wasteFeePrice)    errs.wasteFeePrice     = 'Required'
    if (!form.transportFeePrice) errs.transportFeePrice = 'Required'
    if (!form.ecoFeePrice)      errs.ecoFeePrice      = 'Required'
    if (!form.quantity)         errs.quantity         = 'Required'
    if (!form.weight)           errs.weight           = 'Required'
    if (!form.customerNumber)   errs.customerNumber   = 'Required'
    else if (!/^\d{6,9}$/.test(form.customerNumber)) errs.customerNumber = 'Must be 6-9 digits'
    return errs
  }

  const validateForDraft = () => {
    const errs = {}
    if (!form.eventDate)      errs.eventDate      = 'Required'
    if (!form.customerNumber) errs.customerNumber = 'Required'
    else if (!/^\d{6,9}$/.test(form.customerNumber)) errs.customerNumber = 'Must be 6-9 digits'
    return errs
  }

  const buildDraftPayload = () => ({
    eventDate:                    form.eventDate || null,
    customerNumber:               form.customerNumber || null,
    productId:                    form.productId ? Number(form.productId) : null,
    wasteFeePrice:                form.wasteFeePrice !== '' ? Number(form.wasteFeePrice) : null,
    transportFeePrice:            form.transportFeePrice !== '' ? Number(form.transportFeePrice) : null,
    ecoFeePrice:                  form.ecoFeePrice !== '' ? Number(form.ecoFeePrice) : null,
    quantity:                     form.quantity !== '' ? Number(form.quantity) : null,
    weight:                       form.weight !== '' ? Number(form.weight) : null,
    vehicleId:                    form.vehicleId || null,
    driverId:                     form.driverId || null,
    locationId:                   form.locationId || null,
    municipalityId:               form.municipalityId || null,
    comments:                     form.comments || null,
    internalComments:             form.internalComments || null,
    registrationNumber:           form.registrationNumber || null,
    contractor:                   form.contractor || null,
    direction:                    form.direction || null,
    sharedServiceGroupId:         form.sharedServiceGroupId || null,
    sharedServiceGroupPercentage: form.sharedServiceGroupPercentage !== ''
      ? Number(form.sharedServiceGroupPercentage) : null,
    wasteType:                    form.wasteType || null,
    receivingSite:                form.receivingSite || null,
  })

  const buildPayload = () => ({
    ...form,
    productId:                    Number(form.productId),
    wasteFeePrice:                Number(form.wasteFeePrice),
    transportFeePrice:            Number(form.transportFeePrice),
    ecoFeePrice:                  Number(form.ecoFeePrice),
    quantity:                     Number(form.quantity),
    weight:                       Number(form.weight),
    contractor:                   form.contractor || null,
    direction:                    form.direction  || null,
    sharedServiceGroupId:         form.sharedServiceGroupId || null,
    sharedServiceGroupPercentage: form.sharedServiceGroupPercentage !== ''
      ? Number(form.sharedServiceGroupPercentage) : null,
  })

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setFieldErrors(errs); return }
    setSaving(true); setError(null)
    try {
      const res = await createManualBillingEvent(buildPayload())
      navigate(`/billing-events/${res.data.id}`)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to create billing event.')
    } finally { setSaving(false) }
  }

  const handleSaveDraft = async () => {
    const errs = validateForDraft()
    setFieldErrors(errs)
    if (Object.keys(errs).length > 0) return
    setSaving(true); setError(null)
    try {
      const res = await createDraftBillingEvent(buildDraftPayload())
      navigate(`/billing-events/${res.data.id}`)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to save draft.')
    } finally { setSaving(false) }
  }

  const handleSaveTemplate = async () => {
    if (!templateName.trim()) return
    setSavingTemplate(true)
    try {
      const payload = {
        name: templateName.trim(),
        customerNumber:              form.customerNumber || null,
        contractId:                  contractId ? Number(contractId) : null,
        productId:                   form.productId ? Number(form.productId) : null,
        wasteFeePrice:               form.wasteFeePrice ? Number(form.wasteFeePrice) : null,
        transportFeePrice:           form.transportFeePrice ? Number(form.transportFeePrice) : null,
        ecoFeePrice:                 form.ecoFeePrice ? Number(form.ecoFeePrice) : null,
        quantity:                    form.quantity ? Number(form.quantity) : null,
        weight:                      form.weight ? Number(form.weight) : null,
        vehicleId:                   form.vehicleId || null,
        driverId:                    form.driverId || null,
        locationId:                  form.locationId || null,
        municipalityId:              form.municipalityId || null,
        comments:                    form.comments || null,
        contractor:                  form.contractor || null,
        direction:                   form.direction || null,
        sharedServiceGroupId:        form.sharedServiceGroupId || null,
        sharedServiceGroupPercentage: form.sharedServiceGroupPercentage
          ? Number(form.sharedServiceGroupPercentage) : null,
        wasteType:                   form.wasteType || null,
        receivingSite:               form.receivingSite || null,
      }
      const res = await createBillingEventTemplate(payload)
      setTemplates(t => [...t, res.data])
      setTemplateName('')
      setShowSaveTemplate(false)
    } catch {
      setError('Failed to save template.')
    } finally {
      setSavingTemplate(false)
    }
  }

  const canUndo = histIdx > 0
  const canRedo = histIdx < history.length - 1
  const activeRate = vatRates?.find(r => r.rate > 0)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Create Billing Event</h1>
          <p>Manual creation — all financial metadata is auto-resolved from the selected product and date.</p>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)', alignItems: 'center' }}>
          <button
            className="btn-secondary"
            title="Undo (Ctrl+Z)"
            disabled={!canUndo}
            onClick={() => setHistIdx(i => i - 1)}
          >↩ Undo</button>
          <button
            className="btn-secondary"
            title="Redo (Ctrl+Y)"
            disabled={!canRedo}
            onClick={() => setHistIdx(i => i + 1)}
          >↪ Redo</button>
          <button className="btn-secondary" onClick={() => navigate('/billing-events')}>← Back</button>
        </div>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {/* Template loader */}
      {templates.length > 0 && (
        <div className="form-section" style={{ marginBottom: 'var(--space-4)' }}>
          <div className="form-section-title">Load Template</div>
          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
            <select
              defaultValue=""
              onChange={e => {
                const tpl = templates.find(t => String(t.id) === e.target.value)
                if (tpl) applyTemplate(tpl)
                e.target.value = ''
              }}
              style={{ height: 40, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)', minWidth: 260 }}
            >
              <option value="" disabled>Select a template to pre-fill…</option>
              {templates.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
            <button
              className="btn-secondary"
              onClick={() => navigate('/billing-events/templates')}
            >Manage templates</button>
          </div>
        </div>
      )}

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
          </div>
        </div>

        {/* Section 2 — Customer & Contract */}
        <div className="form-section">
          <div className="form-section-title">Customer &amp; Contract</div>
          <div className="form-row">
            <div className="field">
              <label>Customer <span className="required">*</span></label>
              <CustomerSearchInput
                customerNumber={form.customerNumber}
                onSelect={handleCustomerSelect}
                hasError={!!fieldErrors.customerNumber}
              />
              {fieldErrors.customerNumber && <span className="field-error">{fieldErrors.customerNumber}</span>}
            </div>
            <div className="field">
              <label>Contract</label>
              <select
                value={contractId}
                onChange={handleContractChange}
                disabled={!form.customerNumber || contracts.length === 0}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}
              >
                <option value="">
                  {!form.customerNumber
                    ? 'Select a customer first'
                    : contracts.length === 0
                      ? 'No active contracts'
                      : 'Select contract…'}
                </option>
                {contracts.map(c => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Product {!draftIntent && <span className="required">*</span>}</label>
              <select
                value={form.productId}
                onChange={handleProductChange}
                disabled={!contractId}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}
                className={fieldErrors.productId ? 'input-error' : ''}
              >
                <option value="">
                  {!contractId ? 'Select a contract first' : 'Select product…'}
                </option>
                {products.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.nameFi ?? p.nameEn ?? p.code}
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

        {/* Section 3 — Pricing */}
        <div className="form-section">
          <div className="form-section-title">Pricing</div>
          <div className="form-row-3">
            {['wasteFeePrice', 'transportFeePrice', 'ecoFeePrice'].map(field => (
              <div className="field" key={field}>
                <label>
                  {field === 'wasteFeePrice' ? 'Waste Fee' : field === 'transportFeePrice' ? 'Transport Fee' : 'Eco Fee'}
                  {' '}{!draftIntent && <span className="required">*</span>}
                </label>
                <input type="number" min="0" step="0.01" value={form[field]} onChange={set(field)}
                  className={fieldErrors[field] ? 'input-error' : ''} />
                {fieldErrors[field] && <span className="field-error">{fieldErrors[field]}</span>}
              </div>
            ))}
          </div>
        </div>

        {/* Section 4 — Quantity */}
        <div className="form-section">
          <div className="form-section-title">Quantity &amp; Weight</div>
          <div className="form-row">
            <div className="field">
              <label>
                Quantity{selectedProduct?.pricingUnit ? ` (${selectedProduct.pricingUnit})` : ''}
                {' '}{!draftIntent && <span className="required">*</span>}
              </label>
              <input type="number" min="0" step="0.01" value={form.quantity} onChange={set('quantity')}
                className={fieldErrors.quantity ? 'input-error' : ''} />
              {fieldErrors.quantity && <span className="field-error">{fieldErrors.quantity}</span>}
            </div>
            <div className="field">
              <label>Weight {!draftIntent && <span className="required">*</span>}</label>
              <input type="number" min="0" step="0.001" value={form.weight} onChange={set('weight')}
                className={fieldErrors.weight ? 'input-error' : ''} />
              {fieldErrors.weight && <span className="field-error">{fieldErrors.weight}</span>}
            </div>
          </div>
        </div>

        {/* Section 5 — Location */}
        <div className="form-section">
          <div className="form-section-title">Location</div>
          <div className="form-row">
            <div className="field">
              <label>Municipality <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.municipalityId}
                onChange={(v) => pushForm(f => ({ ...f, municipalityId: v }))}
                onSelect={(m) => pushForm(f => ({ ...f, municipalityId: m.municipalityId }))}
                onSearch={async (q) => { const r = await searchMunicipalities(q); return r.data }}
                renderOption={(m) => <span><strong>{m.municipalityId}</strong> — {m.municipalityName}</span>}
                placeholder="Type municipality name or code…"
              />
            </div>
            <div className="field">
              <label>Location ID <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.locationId}
                onChange={(v) => pushForm(f => ({ ...f, locationId: v }))}
                onSelect={(loc) => pushForm(f => ({ ...f, locationId: loc.locationId, municipalityId: loc.municipalityId }))}
                onSearch={async (q) => { const r = await searchLocations(q); return r.data }}
                renderOption={(loc) => <span><strong>{loc.locationId}</strong> — {loc.name}, {loc.municipalityName}</span>}
                placeholder="Type location ID or name…"
              />
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Vehicle ID <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.vehicleId}
                onChange={(v) => pushForm(f => ({ ...f, vehicleId: v }))}
                onSelect={(v) => pushForm(f => ({ ...f, vehicleId: v.vehicleId }))}
                onSearch={async (q) => { const r = await searchVehicles(q); return r.data }}
                renderOption={(v) => <span><strong>{v.vehicleId}</strong> · {v.registrationPlate} <em style={{color:'var(--color-text-muted)'}}>({v.vehicleType})</em></span>}
                placeholder="Type vehicle ID or plate…"
              />
            </div>
            <div className="field">
              <label>Driver ID <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.driverId}
                onChange={(v) => pushForm(f => ({ ...f, driverId: v }))}
                onSelect={(d) => pushForm(f => ({ ...f, driverId: d.driverId }))}
                onSearch={async (q) => { const r = await searchDrivers(q); return r.data }}
                renderOption={(d) => <span><strong>{d.driverId}</strong> · {d.name}</span>}
                placeholder="Type driver ID or name…"
              />
            </div>
          </div>
        </div>

        {/* Section 6 — Notes */}
        <div className="form-section">
          <div className="form-section-title">Notes</div>
          <div className="field">
            <label>Comments <span className="optional">(optional)</span></label>
            <textarea value={form.comments} onChange={set('comments')}
              style={{ width: '100%', minHeight: 80, padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical' }} />
          </div>
        </div>

        {/* Section 7 — Waste Classification */}
        <div className="form-section">
          <div className="form-section-title">Waste Classification</div>
          <div className="form-row">
            <div className="field">
              <label>Waste Type <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.wasteType}
                onChange={(v) => pushForm(f => ({ ...f, wasteType: v }))}
                onSelect={(wt) => pushForm(f => ({ ...f, wasteType: wt.code }))}
                onSearch={async (q) => { const r = await searchWasteTypes(q); return r.data }}
                renderOption={(wt) => <span><strong>{wt.code}</strong> — {wt.nameEn} <em style={{color:'var(--color-text-muted)'}}>({wt.category})</em></span>}
                placeholder="Type waste type code or name…"
              />
            </div>
            <div className="field">
              <label>Receiving Site <span className="optional">(optional)</span></label>
              <SearchableAutocomplete
                value={form.receivingSite}
                onChange={(v) => pushForm(f => ({ ...f, receivingSite: v }))}
                onSelect={(s) => pushForm(f => ({ ...f, receivingSite: s.name }))}
                onSearch={async (q) => { const r = await searchReceivingSites(q); return r.data }}
                renderOption={(s) => <span>{s.name}<em style={{color:'var(--color-text-muted)',marginLeft:8}}>{s.municipalityName}</em></span>}
                placeholder="Type site name…"
              />
            </div>
          </div>
        </div>

        {/* Section 8 — Additional Details */}
        <div className="form-section">
          <div className="form-section-title">Additional Details</div>
          <div className="form-row">
            <div className="field">
              <label>Contractor <span className="optional">(optional)</span></label>
              <input value={form.contractor} onChange={set('contractor')} />
            </div>
            <div className="field">
              <label>Direction <span className="optional">(optional)</span></label>
              <select value={form.direction} onChange={set('direction')}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}>
                <option value="">— Select —</option>
                <option value="INBOUND">INBOUND</option>
                <option value="OUTBOUND">OUTBOUND</option>
              </select>
            </div>
          </div>
          <div className="form-row" style={{ marginTop: 'var(--space-4)' }}>
            <div className="field">
              <label>Property Group (Shared Service) <span className="optional">(optional)</span></label>
              <select
                value={form.sharedServiceGroupId}
                onChange={set('sharedServiceGroupId')}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}
              >
                <option value="">— None —</option>
                {propertyGroups.map(g => (
                  <option key={g.id} value={String(g.id)}>{g.name}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Shared Collection Group % <span className="optional">(optional)</span></label>
              <input type="number" min="0" max="100" step="0.01" value={form.sharedServiceGroupPercentage} onChange={set('sharedServiceGroupPercentage')} />
            </div>
          </div>
        </div>

        {/* Save template inline panel */}
        {showSaveTemplate && (
          <div className="form-section" style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 'var(--radius-md)', padding: 'var(--space-4)' }}>
            <div className="form-section-title">Save as Template</div>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
              <input
                value={templateName}
                onChange={e => setTemplateName(e.target.value)}
                placeholder="Template name…"
                style={{ flex: 1, height: 40, padding: '0 var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'white', fontSize: 'var(--font-size-base)' }}
              />
              <button type="button" className="btn-primary" disabled={!templateName.trim() || savingTemplate} onClick={handleSaveTemplate}>
                {savingTemplate ? 'Saving…' : 'Save'}
              </button>
              <button type="button" className="btn-secondary" onClick={() => { setShowSaveTemplate(false); setTemplateName('') }}>Cancel</button>
            </div>
          </div>
        )}

        <div className="modal-footer" style={{ borderTop: 'none', padding: '0 0 var(--space-6)' }}>
          <button type="button" className="btn-secondary" onClick={() => navigate('/billing-events')}>Cancel</button>
          {!showSaveTemplate && (
            <button type="button" className="btn-secondary" onClick={() => setShowSaveTemplate(true)}>
              Save as Template
            </button>
          )}
          <button
            type="button"
            className="btn-secondary"
            disabled={saving}
            onClick={handleSaveDraft}
            onMouseEnter={() => { setDraftIntent(true); setFieldErrors({}) }}
            onMouseLeave={() => setDraftIntent(false)}
            onFocus={() => { setDraftIntent(true); setFieldErrors({}) }}
            onBlur={() => setDraftIntent(false)}
            title="Only Date and Customer Number are required to save as draft"
          >
            {saving ? 'Saving…' : 'Save as Draft'}
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={saving}
            onMouseEnter={() => setDraftIntent(false)}
            onFocus={() => setDraftIntent(false)}
          >
            {saving ? 'Creating…' : 'Create Event'}
          </button>
        </div>
      </form>
    </div>
  )
}
