import { useEffect, useState } from 'react'
import { getCostCenters, createCostCenter, updateCostCenter, deleteCostCenter } from '../../api/costCenters'
import './VatRatesPage.css'

const emptyForm = { productSegment: '', receptionSegment: '', responsibilitySegment: '', description: '' }
const emptyErrors = { productSegment: '', receptionSegment: '', responsibilitySegment: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.productSegment.trim())        errors.productSegment = 'Product segment is required.'
  if (!form.receptionSegment.trim())      errors.receptionSegment = 'Reception segment is required.'
  if (!form.responsibilitySegment.trim()) errors.responsibilitySegment = 'Responsibility segment is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

function deriveCode(form) {
  const p = form.productSegment.toUpperCase().trim()
  const r = form.receptionSegment.toUpperCase().trim()
  const s = form.responsibilitySegment.toUpperCase().trim()
  if (!p && !r && !s) return ''
  return [p, r, s].filter(Boolean).join('-')
}

export default function CostCentersPage() {
  const [costCenters, setCostCenters] = useState([])
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
      const res = await getCostCenters()
      setCostCenters(res.data)
    } catch {
      setError('Failed to load cost centers.')
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

  const openEdit = (cc) => {
    setEditing(cc)
    setForm({
      productSegment: cc.productSegment,
      receptionSegment: cc.receptionSegment,
      responsibilitySegment: cc.responsibilitySegment,
      description: cc.description ?? '',
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
    setTouched({ productSegment: true, receptionSegment: true, responsibilitySegment: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        productSegment: form.productSegment.trim(),
        receptionSegment: form.receptionSegment.trim(),
        responsibilitySegment: form.responsibilitySegment.trim(),
        description: form.description.trim() || null,
      }
      if (editing) {
        await updateCostCenter(editing.id, payload)
      } else {
        await createCostCenter(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The composite code may already exist.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this cost center?')) return
    try {
      await deleteCostCenter(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  const previewCode = deriveCode(form)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Cost Centers</h1>
          <p>Manage composite cost center codes by segment</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Cost Center</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Composite Code</th>
              <th>Product</th>
              <th>Reception</th>
              <th>Responsibility</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {costCenters.length === 0 ? (
              <tr><td colSpan={6} className="empty">No cost centers found.</td></tr>
            ) : (
              costCenters.map((cc) => (
                <tr key={cc.id}>
                  <td><span className="code-badge">{cc.compositeCode}</span></td>
                  <td>{cc.productSegment}</td>
                  <td>{cc.receptionSegment}</td>
                  <td>{cc.responsibilitySegment}</td>
                  <td className="muted">{cc.description ?? '—'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(cc)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(cc.id)}>Delete</button>
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
              <h2>{editing ? 'Edit Cost Center' : 'Add Cost Center'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              {previewCode && (
                <div className="composite-preview">
                  <span className="composite-preview-label">Composite code preview</span>
                  <span className="code-badge">{previewCode}</span>
                </div>
              )}

              <div className="field">
                <label>Product Segment <span className="required">*</span></label>
                <input
                  value={form.productSegment}
                  onChange={(e) => handleChange('productSegment', e.target.value)}
                  onBlur={() => handleBlur('productSegment')}
                  placeholder="e.g. WASTE"
                  className={errors.productSegment && touched.productSegment ? 'input-error' : ''}
                />
                {errors.productSegment && touched.productSegment && (
                  <span className="field-error">{errors.productSegment}</span>
                )}
              </div>

              <div className="field">
                <label>Reception Segment <span className="required">*</span></label>
                <input
                  value={form.receptionSegment}
                  onChange={(e) => handleChange('receptionSegment', e.target.value)}
                  onBlur={() => handleBlur('receptionSegment')}
                  placeholder="e.g. HELSINKI-01"
                  className={errors.receptionSegment && touched.receptionSegment ? 'input-error' : ''}
                />
                {errors.receptionSegment && touched.receptionSegment && (
                  <span className="field-error">{errors.receptionSegment}</span>
                )}
              </div>

              <div className="field">
                <label>Responsibility Segment <span className="required">*</span></label>
                <input
                  value={form.responsibilitySegment}
                  onChange={(e) => handleChange('responsibilitySegment', e.target.value)}
                  onBlur={() => handleBlur('responsibilitySegment')}
                  placeholder="e.g. MUNICIPAL"
                  className={errors.responsibilitySegment && touched.responsibilitySegment ? 'input-error' : ''}
                />
                {errors.responsibilitySegment && touched.responsibilitySegment && (
                  <span className="field-error">{errors.responsibilitySegment}</span>
                )}
              </div>

              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <input
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="e.g. Helsinki municipal waste collection"
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
