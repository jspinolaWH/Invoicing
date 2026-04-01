import { useEffect, useState } from 'react'
import { getAccounts, createAccount, updateAccount, deleteAccount } from '../../api/accountingAccounts'
import './VatRatesPage.css'

const emptyForm = { code: '', name: '', validFrom: '', validTo: '' }
const emptyErrors = { code: '', name: '', validFrom: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.code.trim())   errors.code = 'Code is required.'
  if (!form.name.trim())   errors.name = 'Name is required.'
  if (!form.validFrom)     errors.validFrom = 'Valid From date is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function AccountingAccountsPage() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [activeOnFilter, setActiveOnFilter] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  const load = async (activeOn) => {
    setLoading(true)
    setError(null)
    try {
      const params = activeOn ? { activeOn } : {}
      const res = await getAccounts(params)
      setAccounts(res.data)
    } catch {
      setError('Failed to load accounting accounts.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load('') }, [])

  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (a) => {
    setEditing(a)
    setForm({ code: a.code, name: a.name, validFrom: a.validFrom, validTo: a.validTo ?? '' })
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
        validFrom: form.validFrom,
        validTo: form.validTo || null,
      }
      if (editing) {
        await updateAccount(editing.id, payload)
      } else {
        await createAccount(payload)
      }
      closeModal()
      load(activeOnFilter)
    } catch {
      setError('Save failed. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this accounting account?')) return
    try {
      await deleteAccount(id)
      load(activeOnFilter)
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Accounting Accounts</h1>
          <p>Manage ledger accounts with validity periods</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Account</button>
      </div>

      <div className="filter-bar">
        <label>Filter by date:</label>
        <input
          type="date"
          value={activeOnFilter}
          onChange={(e) => { setActiveOnFilter(e.target.value); load(e.target.value) }}
        />
        {activeOnFilter && (
          <button className="btn-link" onClick={() => { setActiveOnFilter(''); load('') }}>Clear</button>
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
              <th>Name</th>
              <th>Valid From</th>
              <th>Valid To</th>
              <th>Created By</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {accounts.length === 0 ? (
              <tr><td colSpan={6} className="empty">No accounting accounts found.</td></tr>
            ) : (
              accounts.map((a) => (
                <tr key={a.id}>
                  <td><span className="code-badge">{a.code}</span></td>
                  <td>{a.name}</td>
                  <td>{a.validFrom}</td>
                  <td>{a.validTo ?? <span className="no-end-date">Active</span>}</td>
                  <td className="muted">{a.createdBy}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(a)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(a.id)}>Delete</button>
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
              <h2>{editing ? 'Edit Account' : 'Add Account'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Code <span className="required">*</span></label>
                <input
                  value={form.code}
                  onChange={(e) => handleChange('code', e.target.value)}
                  onBlur={() => handleBlur('code')}
                  placeholder="e.g. 3001"
                  className={errors.code && touched.code ? 'input-error' : ''}
                />
                {errors.code && touched.code && <span className="field-error">{errors.code}</span>}
              </div>

              <div className="field">
                <label>Name <span className="required">*</span></label>
                <input
                  value={form.name}
                  onChange={(e) => handleChange('name', e.target.value)}
                  onBlur={() => handleBlur('name')}
                  placeholder="e.g. Waste Collection Revenue"
                  className={errors.name && touched.name ? 'input-error' : ''}
                />
                {errors.name && touched.name && <span className="field-error">{errors.name}</span>}
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
