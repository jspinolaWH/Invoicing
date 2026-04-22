import { useEffect, useState } from 'react'
import { getProjects, createProject, updateProject, deactivateProject } from '../../api/projects'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-287', label: '3.4.25 Projects — project-based invoicing', href: 'https://ioteelab.atlassian.net/browse/PD-287' },
]

const emptyForm = { customerNumber: '', name: '', description: '' }
const emptyErrors = { customerNumber: '', name: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.customerNumber.trim()) errors.customerNumber = 'Customer number is required.'
  else if (!/^\d{6,9}$/.test(form.customerNumber.trim())) errors.customerNumber = 'Must be 6-9 digits.'
  if (!form.name.trim()) errors.name = 'Name is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [customerFilter, setCustomerFilter] = useState('')

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
      const params = customerFilter.trim() ? { customerNumber: customerFilter.trim() } : {}
      const res = await getProjects(params)
      setProjects(res.data)
    } catch {
      setError('Failed to load projects.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [customerFilter])

  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (p) => {
    setEditing(p)
    setForm({ customerNumber: p.customerNumber, name: p.name, description: p.description ?? '' })
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
    setTouched({ customerNumber: true, name: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        customerNumber: form.customerNumber.trim(),
        name: form.name.trim(),
        description: form.description.trim() || null,
      }
      if (editing) {
        await updateProject(editing.id, payload)
      } else {
        await createProject(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (id) => {
    if (!window.confirm('Deactivate this project?')) return
    try {
      await deactivateProject(id)
      load()
    } catch {
      setError('Deactivate failed.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Projects</h1>
          <p>Manage projects linked to customers for project-based invoicing</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Project</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      <div className="filter-bar">
        <label>Filter by customer number:</label>
        <input
          value={customerFilter}
          onChange={(e) => setCustomerFilter(e.target.value)}
          placeholder="e.g. 123456789"
          className="filter-select"
          style={{ width: 160 }}
        />
      </div>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Customer #</th>
              <th>Name</th>
              <th>Description</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {projects.length === 0 ? (
              <tr><td colSpan={5} className="empty">No projects found.</td></tr>
            ) : (
              projects.map((p) => (
                <tr key={p.id} style={!p.active ? { opacity: 0.5 } : {}}>
                  <td><span className="code-badge">{p.customerNumber}</span></td>
                  <td>{p.name}</td>
                  <td>{p.description ?? <span className="muted">—</span>}</td>
                  <td>{p.active ? 'Active' : 'Inactive'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(p)}>Edit</button>
                    {p.active && (
                      <button className="btn-danger" onClick={() => handleDeactivate(p.id)}>Deactivate</button>
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
              <h2>{editing ? 'Edit Project' : 'Add Project'}</h2>
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
                <label>Name <span className="required">*</span></label>
                <input
                  value={form.name}
                  onChange={(e) => handleChange('name', e.target.value)}
                  onBlur={() => handleBlur('name')}
                  placeholder="Project name"
                  className={errors.name && touched.name ? 'input-error' : ''}
                />
                {errors.name && touched.name && (
                  <span className="field-error">{errors.name}</span>
                )}
              </div>
              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <textarea
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="Optional description"
                  rows={3}
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
