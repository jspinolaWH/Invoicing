import { useEffect, useState } from 'react'
import { getMinimumFeeConfigs, createMinimumFeeConfig, updateMinimumFeeConfig, deleteMinimumFeeConfig } from '../../api/minimumFeeConfig'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-286', label: '3.4.27 Minimum fee configuration', href: 'https://ioteelab.atlassian.net/browse/PD-286' },
]
const PERIOD_TYPES = ['ANNUAL', 'QUARTERLY']
const CUSTOMER_TYPES = ['RESIDENTIAL', 'BUSINESS', 'MUNICIPAL']
const emptyForm = { customerType: 'RESIDENTIAL', netAmountThreshold: '', periodType: 'ANNUAL', contractStartAdjustment: true, contractEndAdjustment: true, adjustmentProductCode: 'MIN_FEE_ADJ', description: '', active: true }

export default function MinimumFeeConfigPage() {
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  const load = async () => {
    setLoading(true); setError(null)
    try { setConfigs((await getMinimumFeeConfigs()).data) }
    catch { setError('Failed to load minimum fee configs.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => { setEditing(null); setForm(emptyForm); setFormError(null); setShowModal(true) }
  const openEdit = (c) => {
    setEditing(c)
    setForm({ customerType: c.customerType, netAmountThreshold: c.netAmountThreshold, periodType: c.periodType, contractStartAdjustment: c.contractStartAdjustment, contractEndAdjustment: c.contractEndAdjustment, adjustmentProductCode: c.adjustmentProductCode, description: c.description ?? '', active: c.active })
    setFormError(null); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditing(null) }

  const handleSave = async () => {
    if (!form.netAmountThreshold || !form.adjustmentProductCode) { setFormError('Threshold and adjustment product code are required.'); return }
    setSaving(true); setFormError(null)
    try {
      const payload = { ...form, netAmountThreshold: parseFloat(form.netAmountThreshold) }
      if (editing) await updateMinimumFeeConfig(editing.id, payload)
      else await createMinimumFeeConfig(payload)
      close(); load()
      setSuccessMsg(editing ? 'Config updated.' : 'Config created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch { setFormError('Save failed.') }
    finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this minimum fee config?')) return
    try { await deleteMinimumFeeConfig(id); load() }
    catch { setError('Delete failed.') }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Minimum Fee Configuration</h1>
          <p>Define minimum billable net amounts per customer type and billing period</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Config</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? <div className="loading">Loading...</div> : (
        <table className="data-table">
          <thead><tr><th>Customer Type</th><th>Threshold (€ net)</th><th>Period</th><th>Start Adj.</th><th>End Adj.</th><th>Product Code</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {configs.length === 0
              ? <tr><td colSpan={8} className="empty">No minimum fee configs found.</td></tr>
              : configs.map(c => (
                <tr key={c.id} style={{ opacity: c.active ? 1 : 0.5 }}>
                  <td><span className="code-badge">{c.customerType}</span></td>
                  <td>{parseFloat(c.netAmountThreshold).toFixed(2)}</td>
                  <td>{c.periodType}</td>
                  <td>{c.contractStartAdjustment ? 'Yes' : 'No'}</td>
                  <td>{c.contractEndAdjustment ? 'Yes' : 'No'}</td>
                  <td><code>{c.adjustmentProductCode}</code></td>
                  <td>{c.active ? 'Yes' : <span className="muted">No</span>}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(c)}>Edit</button>
                    {c.active && <button className="btn-danger" onClick={() => handleDelete(c.id)}>Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={close}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Minimum Fee Config' : 'Add Minimum Fee Config'}</h2>
              <button className="modal-close" onClick={close}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Customer Type <span className="required">*</span></label>
                <select value={form.customerType} onChange={e => setForm({ ...form, customerType: e.target.value })}>
                  {CUSTOMER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="field"><label>Minimum Net Amount (€ VAT 0%) <span className="required">*</span></label>
                <input type="number" step="0.01" min="0" value={form.netAmountThreshold} onChange={e => setForm({ ...form, netAmountThreshold: e.target.value })} />
              </div>
              <div className="field"><label>Period Type <span className="required">*</span></label>
                <select value={form.periodType} onChange={e => setForm({ ...form, periodType: e.target.value })}>
                  {PERIOD_TYPES.map(p => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <div className="field"><label><input type="checkbox" checked={form.contractStartAdjustment} onChange={e => setForm({ ...form, contractStartAdjustment: e.target.checked })} /> Exempt if contract started after period start</label></div>
              <div className="field"><label><input type="checkbox" checked={form.contractEndAdjustment} onChange={e => setForm({ ...form, contractEndAdjustment: e.target.checked })} /> Exempt if contract ended before period end</label></div>
              <div className="field"><label>Adjustment Product Code <span className="required">*</span></label>
                <input value={form.adjustmentProductCode} onChange={e => setForm({ ...form, adjustmentProductCode: e.target.value })} />
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
