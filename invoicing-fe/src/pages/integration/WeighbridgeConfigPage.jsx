import { useEffect, useState } from 'react'
import {
  getWeighbridgeConfigs,
  upsertWeighbridgeConfig,
  deactivateWeighbridgeConfig,
} from '../../api/weighbridgeConfigs'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-287', label: '3.4.25 Projects — customer-specific weighbridge integration', href: 'https://ioteelab.atlassian.net/browse/PD-287' },
]

const emptyForm = { customerNumber: '', externalSystemId: '', defaultProductCode: '', siteReference: '' }
const emptyErrors = { customerNumber: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.customerNumber.trim()) errors.customerNumber = 'Customer number is required.'
  else if (!/^\d{6,9}$/.test(form.customerNumber.trim())) errors.customerNumber = 'Must be 6-9 digits.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function WeighbridgeConfigPage() {
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getWeighbridgeConfigs()
      setConfigs(res.data)
    } catch {
      setError('Failed to load weighbridge configurations.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => {
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (c) => {
    setForm({
      customerNumber: c.customerNumber,
      externalSystemId: c.externalSystemId ?? '',
      defaultProductCode: c.defaultProductCode ?? '',
      siteReference: c.siteReference ?? '',
    })
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
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
    setTouched({ customerNumber: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      await upsertWeighbridgeConfig({
        customerNumber: form.customerNumber.trim(),
        externalSystemId: form.externalSystemId.trim() || null,
        defaultProductCode: form.defaultProductCode.trim() || null,
        siteReference: form.siteReference.trim() || null,
      })
      closeModal()
      load()
    } catch {
      setError('Save failed. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (id) => {
    if (!window.confirm('Deactivate this weighbridge configuration?')) return
    try {
      await deactivateWeighbridgeConfig(id)
      load()
    } catch {
      setError('Deactivate failed.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Weighbridge Integration Config</h1>
          <p>Customer-specific weighbridge integration settings for billing event ingestion</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Configuration</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Customer #</th>
              <th>External System ID</th>
              <th>Default Product Code</th>
              <th>Site Reference</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {configs.length === 0 ? (
              <tr><td colSpan={6} className="empty">No configurations found.</td></tr>
            ) : (
              configs.map((c) => (
                <tr key={c.id} style={!c.active ? { opacity: 0.5 } : {}}>
                  <td><span className="code-badge">{c.customerNumber}</span></td>
                  <td>{c.externalSystemId ?? <span className="muted">—</span>}</td>
                  <td>{c.defaultProductCode ? <span className="code-badge">{c.defaultProductCode}</span> : <span className="muted">—</span>}</td>
                  <td>{c.siteReference ?? <span className="muted">—</span>}</td>
                  <td>{c.active ? 'Active' : 'Inactive'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(c)}>Edit</button>
                    {c.active && (
                      <button className="btn-danger" onClick={() => handleDeactivate(c.id)}>Deactivate</button>
                    )}
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
              <h2>Weighbridge Integration Configuration</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">
              <div className="field">
                <label>Customer Number <span className="required">*</span></label>
                <input
                  value={form.customerNumber}
                  onChange={(e) => handleChange('customerNumber', e.target.value)}
                  onBlur={() => handleBlur('customerNumber')}
                  placeholder="6-9 digits"
                  className={errors.customerNumber && touched.customerNumber ? 'input-error' : ''}
                />
                {errors.customerNumber && touched.customerNumber && (
                  <span className="field-error">{errors.customerNumber}</span>
                )}
              </div>
              <div className="field">
                <label>External System ID <span className="optional">(optional)</span></label>
                <input
                  value={form.externalSystemId}
                  onChange={(e) => handleChange('externalSystemId', e.target.value)}
                  placeholder="e.g. WB-HELSINKI-01"
                />
              </div>
              <div className="field">
                <label>Default Product Code <span className="optional">(optional)</span></label>
                <input
                  value={form.defaultProductCode}
                  onChange={(e) => handleChange('defaultProductCode', e.target.value)}
                  placeholder="e.g. WASTE-WEIGHBRIDGE"
                />
              </div>
              <div className="field">
                <label>Site Reference <span className="optional">(optional)</span></label>
                <input
                  value={form.siteReference}
                  onChange={(e) => handleChange('siteReference', e.target.value)}
                  placeholder="e.g. Site A — East Gate"
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
