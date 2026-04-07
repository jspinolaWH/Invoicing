import { useEffect, useState } from 'react'
import { getVatRates, createVatRate, updateVatRate, deleteVatRate } from '../../api/vatRates'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-310', label: '3.4.2 FINVOICE data', href: 'https://ioteelab.atlassian.net/browse/PD-310' },
  { id: 'PD-309', label: '3.4.3 Invoice numbering sequence determination', href: 'https://ioteelab.atlassian.net/browse/PD-309' },
]

const emptyForm = { code: '', rate: '', validFrom: '', validTo: '' }
const emptyErrors = { code: '', rate: '', validFrom: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.code.trim())       errors.code = 'Code is required.'
  if (!form.rate)              errors.rate = 'Rate is required.'
  else if (isNaN(parseFloat(form.rate)) || parseFloat(form.rate) < 0)
                               errors.rate = 'Rate must be a valid number (0 or above).'
  if (!form.validFrom)         errors.validFrom = 'Valid From date is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function VatRatesPage() {
  const [rates, setRates] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [eventDateFilter, setEventDateFilter] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingRate, setEditingRate] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  const load = async (eventDate) => {
    setLoading(true)
    setError(null)
    try {
      const params = eventDate ? { eventDate } : {}
      const res = await getVatRates(params)
      setRates(res.data)
    } catch {
      setError('Failed to load VAT rates.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load('') }, [])

  const handleFilterChange = (e) => {
    setEventDateFilter(e.target.value)
    load(e.target.value)
  }

  const openAdd = () => {
    setEditingRate(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (rate) => {
    setEditingRate(rate)
    setForm({ code: rate.code, rate: rate.rate, validFrom: rate.validFrom, validTo: rate.validTo ?? '' })
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditingRate(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
  }

  const handleChange = (field, value) => {
    const updated = { ...form, [field]: value }
    setForm(updated)
    // Re-validate the changed field immediately if it was already touched
    if (touched[field]) {
      setErrors(validate(updated))
    }
  }

  const handleBlur = (field) => {
    const nowTouched = { ...touched, [field]: true }
    setTouched(nowTouched)
    setErrors(validate(form))
  }

  const handleSave = async () => {
    // Mark all required fields as touched and run full validation
    setTouched({ code: true, rate: true, validFrom: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return  // block request — don't hit the BE

    setSaving(true)
    try {
      const payload = {
        code: form.code.trim(),
        rate: parseFloat(form.rate),
        validFrom: form.validFrom,
        validTo: form.validTo || null,
      }
      if (editingRate) {
        await updateVatRate(editingRate.id, payload)
      } else {
        await createVatRate(payload)
      }
      closeModal()
      load(eventDateFilter)
    } catch {
      setError('Save failed. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this VAT rate?')) return
    try {
      await deleteVatRate(id)
      load(eventDateFilter)
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>VAT Rates</h1>
          <p>Manage VAT rates resolved by event date</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add VAT Rate</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      <div className="filter-bar">
        <label>Filter by event date:</label>
        <input
          type="date"
          value={eventDateFilter}
          onChange={handleFilterChange}
        />
        {eventDateFilter && (
          <button className="btn-link" onClick={() => { setEventDateFilter(''); load('') }}>
            Clear
          </button>
        )}
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Rate (%)</th>
              <th>Valid From</th>
              <th>Valid To</th>
              <th>Created By</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rates.length === 0 ? (
              <tr><td colSpan={6} className="empty">No VAT rates found.</td></tr>
            ) : (
              rates.map((r) => (
                <tr key={r.id}>
                  <td><span className="code-badge">{r.code}</span></td>
                  <td>{r.rate}%</td>
                  <td>{r.validFrom}</td>
                  <td>{r.validTo ?? <span className="no-end-date">Active</span>}</td>
                  <td className="muted">{r.createdBy}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(r)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(r.id)}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingRate ? 'Edit VAT Rate' : 'Add VAT Rate'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Code <span className="required">*</span></label>
                <input
                  value={form.code}
                  onChange={(e) => handleChange('code', e.target.value)}
                  onBlur={() => handleBlur('code')}
                  placeholder="e.g. VAT_24"
                  className={errors.code && touched.code ? 'input-error' : ''}
                />
                {errors.code && touched.code && <span className="field-error">{errors.code}</span>}
              </div>

              <div className="field">
                <label>Rate (%) <span className="required">*</span></label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.rate}
                  onChange={(e) => handleChange('rate', e.target.value)}
                  onBlur={() => handleBlur('rate')}
                  placeholder="e.g. 24.00"
                  className={errors.rate && touched.rate ? 'input-error' : ''}
                />
                {errors.rate && touched.rate && <span className="field-error">{errors.rate}</span>}
              </div>

              <div className="field">
                <label>Valid From <span className="required">*</span></label>
                <input
                  type="date"
                  value={form.validFrom}
                  onChange={(e) => handleChange('validFrom', e.target.value)}
                  onBlur={() => handleBlur('validFrom')}
                  className={errors.validFrom && touched.validFrom ? 'input-error' : ''}
                />
                {errors.validFrom && touched.validFrom && <span className="field-error">{errors.validFrom}</span>}
              </div>

              <div className="field">
                <label>Valid To <span className="optional">(optional — leave empty for no end date)</span></label>
                <input
                  type="date"
                  value={form.validTo}
                  onChange={(e) => handleChange('validTo', e.target.value)}
                />
              </div>

            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeModal}>Cancel</button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
