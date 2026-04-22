import { useEffect, useState } from 'react'
import { getPriceLists, createPriceList, updatePriceList, deletePriceList } from '../../api/priceLists'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-296', label: '3.4.16 Cost centers and accounts', href: 'https://ioteelab.atlassian.net/browse/PD-296' },
]

const emptyForm = {
  code: '',
  name: '',
  tariffVariant: '',
  validFrom: '',
  validTo: '',
  description: '',
  active: true,
}
const emptyErrors = { code: '', name: '', validFrom: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.code.trim())      errors.code = 'Code is required.'
  if (!form.name.trim())      errors.name = 'Name is required.'
  if (!form.validFrom)        errors.validFrom = 'Valid from date is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function PriceListsPage() {
  const [priceLists, setPriceLists] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getPriceLists()
      setPriceLists(res.data)
    } catch {
      setError('Failed to load price lists.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (pl) => {
    setEditing(pl)
    setForm({
      code: pl.code,
      name: pl.name,
      tariffVariant: pl.tariffVariant ?? '',
      validFrom: pl.validFrom ?? '',
      validTo: pl.validTo ?? '',
      description: pl.description ?? '',
      active: pl.active,
    })
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
  }

  const handleChange = (field, value) => {
    const updated = { ...form, [field]: value }
    setForm(updated)
    if (touched[field]) setErrors(validate(updated))
  }

  const handleBlur = (field) => {
    setTouched((t) => ({ ...t, [field]: true }))
    setErrors(validate(form))
  }

  const handleSave = async () => {
    setTouched({ code: true, name: true, validFrom: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        code: form.code.trim(),
        name: form.name.trim(),
        tariffVariant: form.tariffVariant.trim() || null,
        validFrom: form.validFrom || null,
        validTo: form.validTo || null,
        description: form.description.trim() || null,
        active: form.active,
      }
      if (editing) {
        await updatePriceList(editing.id, payload)
      } else {
        await createPriceList(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The price list code may already exist.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this price list?')) return
    try {
      await deletePriceList(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Price Lists</h1>
          <p>Manage tariff price lists with validity periods and customer type variants</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Price List</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Tariff Variant</th>
              <th>Valid From</th>
              <th>Valid To</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {priceLists.length === 0 ? (
              <tr><td colSpan={7} className="empty">No price lists found.</td></tr>
            ) : (
              priceLists.map((pl) => (
                <tr key={pl.id}>
                  <td><span className="code-badge">{pl.code}</span></td>
                  <td>{pl.name}</td>
                  <td>{pl.tariffVariant ? <span className="unit-badge">{pl.tariffVariant}</span> : <span className="muted">—</span>}</td>
                  <td>{pl.validFrom}</td>
                  <td>{pl.validTo ?? <span className="muted">—</span>}</td>
                  <td>
                    {pl.active
                      ? <span className="rc-badge" style={{ background: 'var(--color-success-bg, #d1fae5)', color: 'var(--color-success, #065f46)' }}>Active</span>
                      : <span className="muted">Inactive</span>}
                  </td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(pl)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(pl.id)}>Delete</button>
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
              <h2>{editing ? 'Edit Price List' : 'Add Price List'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Code <span className="required">*</span></label>
                <input
                  value={form.code}
                  onChange={(e) => handleChange('code', e.target.value)}
                  onBlur={() => handleBlur('code')}
                  placeholder="e.g. PL-RESIDENTIAL-2024"
                  className={errors.code && touched.code ? 'input-error' : ''}
                />
                {errors.code && touched.code && (
                  <span className="field-error">{errors.code}</span>
                )}
              </div>

              <div className="field">
                <label>Name <span className="required">*</span></label>
                <input
                  value={form.name}
                  onChange={(e) => handleChange('name', e.target.value)}
                  onBlur={() => handleBlur('name')}
                  placeholder="e.g. Residential Tariff 2024"
                  className={errors.name && touched.name ? 'input-error' : ''}
                />
                {errors.name && touched.name && (
                  <span className="field-error">{errors.name}</span>
                )}
              </div>

              <div className="field">
                <label>Tariff Variant <span className="optional">(optional)</span></label>
                <input
                  value={form.tariffVariant}
                  onChange={(e) => handleChange('tariffVariant', e.target.value)}
                  placeholder="e.g. RESIDENTIAL, COMMERCIAL, INDUSTRIAL"
                />
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
                {errors.validFrom && touched.validFrom && (
                  <span className="field-error">{errors.validFrom}</span>
                )}
              </div>

              <div className="field">
                <label>Valid To <span className="optional">(optional — leave blank for open-ended)</span></label>
                <input
                  type="date"
                  value={form.validTo}
                  onChange={(e) => handleChange('validTo', e.target.value)}
                />
              </div>

              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <input
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="e.g. Standard residential waste tariff"
                />
              </div>

              <div className="field field-checkbox">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={form.active}
                    onChange={(e) => handleChange('active', e.target.checked)}
                  />
                  <span>Active</span>
                </label>
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
