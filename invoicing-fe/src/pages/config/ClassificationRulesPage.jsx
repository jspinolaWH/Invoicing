import { useEffect, useState } from 'react'
import {
  getClassificationRules,
  createClassificationRule,
  updateClassificationRule,
  deleteClassificationRule,
} from '../../api/classificationRules'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const RELATED_TASKS = [
  { id: 'PD-284', label: '3.3.1 Legal classification rules', href: 'https://ioteelab.atlassian.net/browse/PD-284' },
  { id: 'PD-285', label: '3.3.2 Classification rule priority', href: 'https://ioteelab.atlassian.net/browse/PD-285' },
]

const CUSTOMER_TYPES = ['', 'PRIVATE', 'BUSINESS', 'MUNICIPALITY', 'AUTHORITY']
const CLASSIFICATIONS = ['PUBLIC_LAW', 'PRIVATE_LAW']

const emptyForm = {
  priority: 1,
  label: '',
  customerTypeCondition: '',
  productCodeCondition: '',
  regionCondition: '',
  resultClassification: 'PUBLIC_LAW',
  active: true,
}

const emptyErrors = { priority: '', resultClassification: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.priority || form.priority < 1) errors.priority = 'Priority must be at least 1.'
  if (!form.resultClassification) errors.resultClassification = 'Result classification is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function ClassificationRulesPage() {
  const [rules, setRules] = useState([])
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
      const res = await getClassificationRules(COMPANY_ID)
      setRules(res.data)
    } catch {
      setError('Failed to load classification rules.')
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

  const openEdit = (r) => {
    setEditing(r)
    setForm({
      priority: r.priority,
      label: r.label ?? '',
      customerTypeCondition: r.customerTypeCondition ?? '',
      productCodeCondition: r.productCodeCondition ?? '',
      regionCondition: r.regionCondition ?? '',
      resultClassification: r.resultClassification,
      active: r.active,
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
    setTouched({ priority: true, resultClassification: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        companyId: COMPANY_ID,
        priority: parseInt(form.priority, 10),
        label: form.label || null,
        customerTypeCondition: form.customerTypeCondition || null,
        productCodeCondition: form.productCodeCondition || null,
        regionCondition: form.regionCondition || null,
        resultClassification: form.resultClassification,
        active: form.active,
      }
      if (editing) {
        await updateClassificationRule(editing.id, payload)
      } else {
        await createClassificationRule(payload)
      }
      closeModal()
      load()
    } catch (err) {
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? 'Save failed.'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this classification rule?')) return
    try {
      await deleteClassificationRule(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Classification Rules</h1>
          <p>Define legal classification rules (PUBLIC_LAW / PRIVATE_LAW) by priority</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Rule</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Priority</th>
              <th>Label</th>
              <th>Customer Type</th>
              <th>Product Code</th>
              <th>Region</th>
              <th>Result</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rules.length === 0 ? (
              <tr><td colSpan={8} className="empty">No classification rules found.</td></tr>
            ) : (
              rules.map((r) => (
                <tr key={r.id}>
                  <td>{r.priority}</td>
                  <td>{r.label ?? <span className="muted">—</span>}</td>
                  <td>{r.customerTypeCondition ?? <span className="muted">Any</span>}</td>
                  <td>{r.productCodeCondition ?? <span className="muted">Any</span>}</td>
                  <td>{r.regionCondition ?? <span className="muted">Any</span>}</td>
                  <td><span className="code-badge">{r.resultClassification}</span></td>
                  <td>{r.active ? 'Yes' : 'No'}</td>
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
              <h2>{editing ? 'Edit Classification Rule' : 'Add Classification Rule'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Priority <span className="required">*</span></label>
                <input
                  type="number"
                  min="1"
                  value={form.priority}
                  onChange={(e) => handleChange('priority', e.target.value)}
                  onBlur={() => handleBlur('priority')}
                  className={errors.priority && touched.priority ? 'input-error' : ''}
                />
                {errors.priority && touched.priority && (
                  <span className="field-error">{errors.priority}</span>
                )}
              </div>

              <div className="field">
                <label>Label <span className="optional">(optional)</span></label>
                <input
                  value={form.label}
                  onChange={(e) => handleChange('label', e.target.value)}
                  placeholder="Human-readable label"
                />
              </div>

              <div className="field">
                <label>Customer Type Condition <span className="optional">(leave empty for any)</span></label>
                <select
                  value={form.customerTypeCondition}
                  onChange={(e) => handleChange('customerTypeCondition', e.target.value)}
                >
                  {CUSTOMER_TYPES.map((t) => (
                    <option key={t} value={t}>{t || 'Any'}</option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>Product Code Condition <span className="optional">(optional)</span></label>
                <input
                  value={form.productCodeCondition}
                  onChange={(e) => handleChange('productCodeCondition', e.target.value)}
                  placeholder="e.g. WASTE-COLLECTION-240L"
                />
              </div>

              <div className="field">
                <label>Region Condition <span className="optional">(optional)</span></label>
                <input
                  value={form.regionCondition}
                  onChange={(e) => handleChange('regionCondition', e.target.value)}
                  placeholder="e.g. HELSINKI"
                />
              </div>

              <div className="field">
                <label>Result Classification <span className="required">*</span></label>
                <select
                  value={form.resultClassification}
                  onChange={(e) => handleChange('resultClassification', e.target.value)}
                  className={errors.resultClassification && touched.resultClassification ? 'input-error' : ''}
                >
                  {CLASSIFICATIONS.map((c) => (
                    <option key={c} value={c}>{c.replace('_', ' ')}</option>
                  ))}
                </select>
                {errors.resultClassification && touched.resultClassification && (
                  <span className="field-error">{errors.resultClassification}</span>
                )}
              </div>

              <div className="field">
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={form.active}
                    onChange={(e) => handleChange('active', e.target.checked)}
                  />
                  Active
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
