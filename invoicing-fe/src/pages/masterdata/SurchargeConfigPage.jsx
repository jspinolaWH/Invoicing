import { useEffect, useState } from 'react'
import { getSurchargeConfigs, createSurchargeConfig, updateSurchargeConfig, deleteSurchargeConfig, setGlobalSurchargeEnabled } from '../../api/surchargeConfig'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-294', label: '3.4.26 Invoice surcharge configuration', href: 'https://ioteelab.atlassian.net/browse/PD-294' },
]
const DELIVERY_METHODS = ['E_INVOICE', 'EMAIL', 'PAPER', 'DIRECT_PAYMENT']
const emptyForm = { deliveryMethod: 'PAPER', amount: '', description: '', active: true }

export default function SurchargeConfigPage() {
  const [configs, setConfigs] = useState([])
  const [globalEnabled, setGlobalEnabled] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)
  const [toggling, setToggling] = useState(false)

  const load = async () => {
    setLoading(true); setError(null)
    try {
      const res = await getSurchargeConfigs()
      setConfigs(res.data)
      if (res.data.length > 0) setGlobalEnabled(res.data[0].globalSurchargeEnabled)
    }
    catch { setError('Failed to load surcharge configs.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => { setEditing(null); setForm(emptyForm); setFormError(null); setShowModal(true) }
  const openEdit = (c) => {
    setEditing(c)
    setForm({ deliveryMethod: c.deliveryMethod, amount: c.amount, description: c.description ?? '', active: c.active })
    setFormError(null); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditing(null) }

  const handleSave = async () => {
    if (!form.amount) { setFormError('Amount is required.'); return }
    setSaving(true); setFormError(null)
    try {
      const payload = { ...form, amount: parseFloat(form.amount) }
      if (editing) await updateSurchargeConfig(editing.id, payload)
      else await createSurchargeConfig(payload)
      close(); load()
      setSuccessMsg(editing ? 'Config updated.' : 'Config created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch { setFormError('Save failed.') }
    finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this surcharge config?')) return
    try { await deleteSurchargeConfig(id); load() }
    catch { setError('Delete failed.') }
  }

  const handleGlobalToggle = async () => {
    setToggling(true)
    try {
      await setGlobalSurchargeEnabled(!globalEnabled)
      setGlobalEnabled(!globalEnabled)
      load()
    } catch { setError('Toggle failed.') }
    finally { setToggling(false) }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Surcharge Configuration</h1>
          <p>Configure invoice delivery method surcharges</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Config</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-4)', padding: 'var(--space-4)', background: globalEnabled ? '#f0fdf4' : '#fff5f5', border: `1px solid ${globalEnabled ? '#bbf7d0' : '#fecaca'}`, borderRadius: 'var(--radius-md)' }}>
        <span style={{ fontWeight: 500 }}>Invoice Surcharges: <strong style={{ color: globalEnabled ? 'green' : '#dc2626' }}>{globalEnabled ? 'ENABLED' : 'DISABLED'}</strong></span>
        <button className="btn-secondary" onClick={handleGlobalToggle} disabled={toggling}>
          {toggling ? 'Updating…' : (globalEnabled ? 'Disable All' : 'Enable All')}
        </button>
        {!globalEnabled && <span className="muted">All invoice surcharges are currently disabled</span>}
      </div>

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? <div className="loading">Loading...</div> : (
        <div style={{ opacity: globalEnabled ? 1 : 0.6 }}>
          <table className="data-table">
            <thead><tr><th>Delivery Method</th><th>Amount (€)</th><th>Description</th><th>Active</th><th>Actions</th></tr></thead>
            <tbody>
              {configs.length === 0
                ? <tr><td colSpan={5} className="empty">No surcharge configs found.</td></tr>
                : configs.map(c => (
                  <tr key={c.id}>
                    <td><span className="code-badge">{c.deliveryMethod}</span></td>
                    <td>{parseFloat(c.amount).toFixed(2)}</td>
                    <td>{c.description ?? <span className="muted">—</span>}</td>
                    <td>{c.active ? 'Yes' : <span className="muted">No</span>}</td>
                    <td className="actions">
                      <button className="btn-secondary" onClick={() => openEdit(c)}>Edit</button>
                      {c.active && <button className="btn-danger" onClick={() => handleDelete(c.id)}>Deactivate</button>}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={close}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Surcharge Config' : 'Add Surcharge Config'}</h2>
              <button className="modal-close" onClick={close}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Delivery Method <span className="required">*</span></label>
                <select value={form.deliveryMethod} onChange={e => setForm({ ...form, deliveryMethod: e.target.value })} disabled={!!editing}>
                  {DELIVERY_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
                </select>
              </div>
              <div className="field"><label>Amount (€) <span className="required">*</span></label>
                <input type="number" step="0.01" min="0" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} />
              </div>
              <div className="field"><label>Description</label>
                <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
              </div>
              <div className="field"><label><input type="checkbox" checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })} /> Active</label></div>
              {formError && <div className="error-msg">{formError}</div>}
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={close} disabled={saving}>Cancel</button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
