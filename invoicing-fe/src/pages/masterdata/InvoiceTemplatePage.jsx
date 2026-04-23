import { useEffect, useState } from 'react'
import { getTemplates, createTemplate, updateTemplate, deleteTemplate } from '../../api/invoiceTemplates'
import { getSeries } from '../../api/invoiceNumberSeries'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-309', label: '3.4.3 Invoice numbering sequence determination', href: 'https://ioteelab.atlassian.net/browse/PD-309' },
]

const emptyForm = { name: '', code: '', numberSeriesId: '' }
const emptyErrors = { name: '', code: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.name.trim()) errors.name = 'Name is required.'
  if (!form.code.trim()) errors.code = 'Code is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function InvoiceTemplatePage() {
  const [templates, setTemplates] = useState([])
  const [seriesList, setSeriesList] = useState([])
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
      const [tRes, sRes] = await Promise.all([getTemplates(), getSeries()])
      setTemplates(tRes.data)
      setSeriesList(sRes.data)
    } catch {
      setError('Failed to load templates.')
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

  const openEdit = (t) => {
    setEditing(t)
    setForm({ name: t.name, code: t.code, numberSeriesId: t.numberSeriesId || '' })
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
    setTouched(t => ({ ...t, [field]: true }))
    setErrors(validate(form))
  }

  const handleSave = async () => {
    setTouched({ name: true, code: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        numberSeriesId: form.numberSeriesId ? Number(form.numberSeriesId) : null,
      }
      if (editing) {
        await updateTemplate(editing.id, payload)
      } else {
        await createTemplate(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The template name or code may already exist.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (t) => {
    if (!window.confirm(`Delete template "${t.name}"?`)) return
    try {
      await deleteTemplate(t.id)
      load()
    } catch {
      setError('Delete failed.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Templates</h1>
          <p>Define invoice templates with a linked number series for structured invoice numbering.</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Template</button>
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
              <th>Code</th>
              <th>Number Series</th>
              <th>Created By</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {templates.length === 0 ? (
              <tr><td colSpan={5} className="empty">No templates found.</td></tr>
            ) : (
              templates.map(t => (
                <tr key={t.id}>
                  <td>{t.name}</td>
                  <td><span className="code-badge">{t.code}</span></td>
                  <td>{t.numberSeriesName ? <span className="unit-badge">{t.numberSeriesName}</span> : <span className="muted">—</span>}</td>
                  <td className="muted">{t.createdBy || '—'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(t)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(t)}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Template' : 'Add Template'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Name <span className="required">*</span></label>
                <input
                  value={form.name}
                  onChange={e => handleChange('name', e.target.value)}
                  onBlur={() => handleBlur('name')}
                  placeholder="e.g. Standard Waste Invoice"
                  className={errors.name && touched.name ? 'input-error' : ''}
                />
                {errors.name && touched.name && <span className="field-error">{errors.name}</span>}
              </div>

              <div className="field">
                <label>Code <span className="required">*</span></label>
                <input
                  value={form.code}
                  onChange={e => handleChange('code', e.target.value)}
                  onBlur={() => handleBlur('code')}
                  placeholder="e.g. WASTE_STANDARD"
                  className={errors.code && touched.code ? 'input-error' : ''}
                />
                {errors.code && touched.code && <span className="field-error">{errors.code}</span>}
              </div>

              <div className="field">
                <label>Number Series</label>
                <select
                  value={form.numberSeriesId}
                  onChange={e => handleChange('numberSeriesId', e.target.value)}
                >
                  <option value="">— None —</option>
                  {seriesList.map(s => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.prefix}){s.category ? ` · ${s.category}` : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 6, padding: '10px 14px', marginTop: 8 }}>
                <p style={{ margin: '0 0 6px', fontSize: 13, fontWeight: 600, color: '#0369a1' }}>Custom Text Placement</p>
                <p style={{ margin: '0 0 6px', fontSize: 12, color: '#374151' }}>
                  Custom text entered on an invoice is included in the FINVOICE XML output as an <code style={{ background: '#e0f2fe', borderRadius: 3, padding: '1px 4px' }}>InvoiceFreeText</code> element, immediately after the invoice header fields.
                </p>
                <p style={{ margin: 0, fontSize: 12, color: '#374151' }}>
                  The text applies per-invoice (set on the invoice detail page) or in bulk (via the invoice list "Set Text" action). It is visible to the customer in the transmitted invoice document.
                </p>
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
