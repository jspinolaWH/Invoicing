import { useEffect, useState } from 'react'
import { getSeries, createSeries, updateSeries, deleteSeries, assignNumber } from '../../api/invoiceNumberSeries'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'
import './InvoiceNumberSeriesPage.css'

const RELATED_TASKS = [
  { id: 'PD-309', label: '3.4.3 Invoice numbering sequence determination', href: 'https://ioteelab.atlassian.net/browse/PD-309' },
]

const CATEGORIES = ['', 'SLUDGE', 'CREDIT', 'PUBLIC', 'PRIVATE', 'ANNUAL']
const emptyForm = { name: '', prefix: '', formatPattern: '{PREFIX}-{YEAR}-{COUNTER:06d}', category: '' }
const emptyErrors = { name: '', prefix: '', formatPattern: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.name.trim())          errors.name = 'Name is required.'
  if (!form.prefix.trim())        errors.prefix = 'Prefix is required.'
  if (!form.formatPattern.trim()) errors.formatPattern = 'Format pattern is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

function previewFormat(form) {
  if (!form.formatPattern || !form.prefix) return ''
  return form.formatPattern
    .replace('{PREFIX}', form.prefix.toUpperCase() || 'XXX')
    .replace('{YEAR}', new Date().getFullYear())
    .replace('{COUNTER:06d}', '000001')
}

export default function InvoiceNumberSeriesPage() {
  const [seriesList, setSeriesList] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Add / Edit modal
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  // Test Assign panel
  const [assignPanelId, setAssignPanelId] = useState(null)
  const [simulationMode, setSimulationMode] = useState(true)
  const [assignResult, setAssignResult] = useState(null)
  const [assigning, setAssigning] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getSeries()
      setSeriesList(res.data)
    } catch {
      setError('Failed to load invoice number series.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  // ── Modal ──────────────────────────────────────
  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (s) => {
    setEditing(s)
    setForm({ name: s.name, prefix: s.prefix, formatPattern: s.formatPattern, category: s.category || '' })
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
    setTouched({ name: true, prefix: true, formatPattern: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        name: form.name.trim(),
        prefix: form.prefix.trim(),
        formatPattern: form.formatPattern.trim(),
        category: form.category || null,
      }
      if (editing) {
        await updateSeries(editing.id, payload)
      } else {
        await createSeries(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The series name may already exist.')
    } finally {
      setSaving(false)
    }
  }

  // ── Delete ─────────────────────────────────────
  const handleDelete = async (s) => {
    if (!window.confirm(`Delete series "${s.name}"? This cannot be undone.`)) return
    try {
      await deleteSeries(s.id)
      load()
    } catch (e) {
      setError(e.response?.data?.message || 'Delete failed. Series may be in use.')
    }
  }

  // ── Test Assign panel ──────────────────────────
  const openAssignPanel = (id) => {
    setAssignPanelId((prev) => (prev === id ? null : id))
    setSimulationMode(true)
    setAssignResult(null)
  }

  const handleAssign = async (id) => {
    if (!simulationMode && !window.confirm(
      'This will permanently increment the counter. Are you sure?'
    )) return

    setAssigning(true)
    setAssignResult(null)
    try {
      const res = await assignNumber(id, simulationMode)
      setAssignResult(res.data)
      if (!simulationMode) load()   // refresh counter in table
    } catch {
      setError('Failed to assign number.')
    } finally {
      setAssigning(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Number Series</h1>
          <p>Manage sequential invoice numbering with atomic counter assignment</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Series</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Prefix</th>
              <th>Category</th>
              <th>Format Pattern</th>
              <th>Current Counter</th>
              <th>Released</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {seriesList.length === 0 ? (
              <tr><td colSpan={7} className="empty">No series found.</td></tr>
            ) : (
              seriesList.map((s) => (
                <>
                  <tr key={s.id}>
                    <td><span className="code-badge">{s.name}</span></td>
                    <td><span className="unit-badge">{s.prefix}</span></td>
                    <td className="muted">{s.category || '—'}</td>
                    <td className="pattern-cell">{s.formatPattern}</td>
                    <td><span className="counter-value">{s.currentCounter.toLocaleString()}</span></td>
                    <td className="muted">{s.releasedNumbersCount}</td>
                    <td className="actions">
                      <button className="btn-secondary" onClick={() => openEdit(s)}>Edit</button>
                      <button
                        className={`btn-secondary ${assignPanelId === s.id ? 'active' : ''}`}
                        onClick={() => openAssignPanel(s.id)}
                      >
                        Test Assign {assignPanelId === s.id ? '▲' : '▼'}
                      </button>
                      <button className="btn-danger" onClick={() => handleDelete(s)}>Delete</button>
                    </td>
                  </tr>

                  {assignPanelId === s.id && (
                    <tr key={`${s.id}-assign`} className="assign-row">
                      <td colSpan={7}>
                        <div className="assign-panel">
                          <div className="assign-panel-title">Test Number Assignment</div>

                          <label className="checkbox-label simulation-toggle">
                            <input
                              type="checkbox"
                              checked={simulationMode}
                              onChange={(e) => {
                                setSimulationMode(e.target.checked)
                                setAssignResult(null)
                              }}
                            />
                            <span>Simulation mode</span>
                            <span className="optional">(counter is NOT incremented)</span>
                          </label>

                          {!simulationMode && (
                            <div className="assign-warning">
                              Real mode — this will permanently increment the counter.
                            </div>
                          )}

                          <button
                            className="btn-primary"
                            onClick={() => handleAssign(s.id)}
                            disabled={assigning}
                          >
                            {assigning ? 'Assigning...' : 'Assign Number'}
                          </button>

                          {assignResult && (
                            <div className={`assign-result ${assignResult.simulationMode ? 'assign-result--sim' : 'assign-result--real'}`}>
                              <span className="assign-result-label">
                                {assignResult.simulationMode ? 'Simulation preview' : 'Assigned number'}
                              </span>
                              <span className="assign-result-number">{assignResult.assignedNumber}</span>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Series' : 'Add Series'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Series Name <span className="required">*</span></label>
                <input
                  value={form.name}
                  onChange={(e) => handleChange('name', e.target.value)}
                  onBlur={() => handleBlur('name')}
                  placeholder="e.g. MAIN_2026"
                  className={errors.name && touched.name ? 'input-error' : ''}
                />
                {errors.name && touched.name && (
                  <span className="field-error">{errors.name}</span>
                )}
              </div>

              <div className="field">
                <label>Prefix <span className="required">*</span></label>
                <input
                  value={form.prefix}
                  onChange={(e) => handleChange('prefix', e.target.value)}
                  onBlur={() => handleBlur('prefix')}
                  placeholder="e.g. INV"
                  className={errors.prefix && touched.prefix ? 'input-error' : ''}
                />
                {errors.prefix && touched.prefix && (
                  <span className="field-error">{errors.prefix}</span>
                )}
              </div>

              <div className="field">
                <label>Category</label>
                <select
                  value={form.category}
                  onChange={(e) => handleChange('category', e.target.value)}
                >
                  {CATEGORIES.map(c => (
                    <option key={c} value={c}>{c || '— None —'}</option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>Format Pattern <span className="required">*</span></label>
                <input
                  value={form.formatPattern}
                  onChange={(e) => handleChange('formatPattern', e.target.value)}
                  onBlur={() => handleBlur('formatPattern')}
                  placeholder="{PREFIX}-{YEAR}-{COUNTER:06d}"
                  className={errors.formatPattern && touched.formatPattern ? 'input-error' : ''}
                />
                {errors.formatPattern && touched.formatPattern && (
                  <span className="field-error">{errors.formatPattern}</span>
                )}
                {previewFormat(form) && (
                  <span className="field-hint">
                    Preview: <strong>{previewFormat(form)}</strong>
                  </span>
                )}
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
