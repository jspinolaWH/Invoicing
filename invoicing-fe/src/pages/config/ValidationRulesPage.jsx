import { useEffect, useState } from 'react'
import {
  getValidationRules,
  createValidationRule,
  updateValidationRule,
  deleteValidationRule,
} from '../../api/validationRules'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const RELATED_TASKS = [
  { id: 'PD-271', label: '3.3.3 Validation rule configuration', href: 'https://ioteelab.atlassian.net/browse/PD-271' },
  { id: 'PD-278', label: '3.3.4 Mandatory field validation', href: 'https://ioteelab.atlassian.net/browse/PD-278' },
]

const RULE_TYPES = ['MANDATORY_FIELD', 'PRICE_CONSISTENCY', 'QUANTITY_THRESHOLD', 'CLASSIFICATION', 'REPORTING_DATA_COMPLETENESS', 'VAT_ACCURACY']

const emptyForm = {
  ruleType: 'MANDATORY_FIELD',
  ruleCode: '',
  config: '',
  blocking: true,
  active: true,
  description: '',
}

const emptyErrors = { ruleCode: '', ruleType: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.ruleCode.trim()) errors.ruleCode = 'Rule code is required.'
  if (!form.ruleType) errors.ruleType = 'Rule type is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function ValidationRulesPage() {
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
      const res = await getValidationRules(COMPANY_ID)
      setRules(res.data)
    } catch {
      setError('Failed to load validation rules.')
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
      ruleType: r.ruleType,
      ruleCode: r.ruleCode,
      config: r.config ?? '',
      blocking: r.blocking,
      active: r.active,
      description: r.description ?? '',
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
    setTouched({ ruleCode: true, ruleType: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        companyId: COMPANY_ID,
        ruleType: form.ruleType,
        ruleCode: form.ruleCode.trim(),
        config: form.config || null,
        blocking: form.blocking,
        active: form.active,
        description: form.description || null,
      }
      if (editing) {
        await updateValidationRule(editing.id, payload)
      } else {
        await createValidationRule(payload)
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
    if (!window.confirm('Delete this validation rule?')) return
    try {
      await deleteValidationRule(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Validation Rules</h1>
          <p>Configure mandatory field and consistency validation rules</p>
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
              <th>Rule Code</th>
              <th>Type</th>
              <th>Blocking</th>
              <th>Active</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rules.length === 0 ? (
              <tr><td colSpan={6} className="empty">No validation rules found.</td></tr>
            ) : (
              rules.map((r) => (
                <tr key={r.id}>
                  <td><span className="code-badge">{r.ruleCode}</span></td>
                  <td>{r.ruleType}</td>
                  <td style={{ color: r.blocking ? '#dc2626' : '#6b7280' }}>
                    {r.blocking ? 'Yes' : 'No'}
                  </td>
                  <td>{r.active ? 'Yes' : 'No'}</td>
                  <td className="muted">{r.description ?? '—'}</td>
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
              <h2>{editing ? 'Edit Validation Rule' : 'Add Validation Rule'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Rule Type <span className="required">*</span></label>
                <select
                  value={form.ruleType}
                  onChange={(e) => handleChange('ruleType', e.target.value)}
                  className={errors.ruleType && touched.ruleType ? 'input-error' : ''}
                >
                  {RULE_TYPES.map((t) => (
                    <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                  ))}
                </select>
                {errors.ruleType && touched.ruleType && (
                  <span className="field-error">{errors.ruleType}</span>
                )}
              </div>

              <div className="field">
                <label>Rule Code <span className="required">*</span></label>
                <input
                  value={form.ruleCode}
                  onChange={(e) => handleChange('ruleCode', e.target.value)}
                  onBlur={() => handleBlur('ruleCode')}
                  placeholder="e.g. BUSINESS_ID_MANDATORY"
                  className={errors.ruleCode && touched.ruleCode ? 'input-error' : ''}
                />
                {errors.ruleCode && touched.ruleCode && (
                  <span className="field-error">{errors.ruleCode}</span>
                )}
              </div>

              <div className="field">
                <label>Config JSON <span className="optional">(optional)</span></label>
                <input
                  value={form.config}
                  onChange={(e) => handleChange('config', e.target.value)}
                  placeholder='e.g. {"field":"billingProfile.businessId"}'
                />
              </div>

              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <input
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="Human-readable description"
                />
              </div>

              <div className="field">
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={form.blocking}
                    onChange={(e) => handleChange('blocking', e.target.checked)}
                  />
                  Blocking (prevents invoice generation if violated)
                </label>
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
