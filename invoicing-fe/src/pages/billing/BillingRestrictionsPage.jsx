import { useEffect, useState } from 'react'
import { getBillingRestrictions, createBillingRestriction, updateBillingRestriction, deleteBillingRestriction } from '../../api/billingRestrictions'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-292', label: '3.4.22 Billing restrictions / immediate invoicing', href: 'https://ioteelab.atlassian.net/browse/PD-292' },
]

const emptyForm = { municipality: '', customerType: '', serviceType: '', locationId: '', minAmount: '', period: '', serviceResponsibility: '', billingType: 'CYCLE_BASED', description: '', active: true }

export default function BillingRestrictionsPage() {
  const [restrictions, setRestrictions] = useState([])
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
    try { setRestrictions((await getBillingRestrictions()).data) }
    catch { setError('Failed to load billing restrictions.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => { setEditing(null); setForm(emptyForm); setFormError(null); setShowModal(true) }
  const openEdit = (r) => {
    setEditing(r)
    setForm({ municipality: r.municipality ?? '', customerType: r.customerType ?? '', serviceType: r.serviceType ?? '', locationId: r.locationId ?? '', minAmount: r.minAmount ?? '', period: r.period ?? '', serviceResponsibility: r.serviceResponsibility ?? '', billingType: r.billingType, description: r.description ?? '', active: r.active })
    setFormError(null); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditing(null) }

  const handleSave = async () => {
    setSaving(true); setFormError(null)
    try {
      const payload = { ...form, locationId: form.locationId || null, minAmount: form.minAmount ? parseFloat(form.minAmount) : null, municipality: form.municipality || null, customerType: form.customerType || null, serviceType: form.serviceType || null, period: form.period || null, serviceResponsibility: form.serviceResponsibility || null, description: form.description || null }
      if (editing) await updateBillingRestriction(editing.id, payload)
      else await createBillingRestriction(payload)
      close(); load()
      setSuccessMsg(editing ? 'Restriction updated.' : 'Restriction created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch { setFormError('Save failed.') }
    finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this restriction?')) return
    try { await deleteBillingRestriction(id); load() }
    catch { setError('Delete failed.') }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Billing Restrictions</h1>
          <p>Configure immediate invoicing overrides and invoice run filter criteria</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Restriction</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? <div className="loading">Loading...</div> : (
        <table className="data-table">
          <thead><tr><th>Billing Type</th><th>Service Type</th><th>Municipality</th><th>Customer Type</th><th>Min Amount</th><th>Period</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {restrictions.length === 0
              ? <tr><td colSpan={8} className="empty">No billing restrictions found.</td></tr>
              : restrictions.map(r => (
                <tr key={r.id} style={{ opacity: r.active ? 1 : 0.5 }}>
                  <td><span className="code-badge" style={{ borderColor: r.billingType === 'IMMEDIATE' ? 'var(--color-warning, #d97706)' : undefined, color: r.billingType === 'IMMEDIATE' ? 'var(--color-warning, #d97706)' : undefined }}>{r.billingType}</span></td>
                  <td>{r.serviceType ?? <span className="muted">—</span>}</td>
                  <td>{r.municipality ?? <span className="muted">—</span>}</td>
                  <td>{r.customerType ?? <span className="muted">—</span>}</td>
                  <td>{r.minAmount != null ? `€${r.minAmount}` : <span className="muted">—</span>}</td>
                  <td>{r.period ?? <span className="muted">—</span>}</td>
                  <td>{r.active ? 'Yes' : <span className="muted">No</span>}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(r)}>Edit</button>
                    {r.active && <button className="btn-danger" onClick={() => handleDelete(r.id)}>Deactivate</button>}
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
              <h2>{editing ? 'Edit Restriction' : 'Add Billing Restriction'}</h2>
              <button className="modal-close" onClick={close}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Billing Type <span className="required">*</span></label>
                <select value={form.billingType} onChange={e => setForm({ ...form, billingType: e.target.value })}>
                  <option value="CYCLE_BASED">CYCLE_BASED</option>
                  <option value="IMMEDIATE">IMMEDIATE</option>
                </select>
              </div>
              <div className="field"><label>Service Type</label><input value={form.serviceType} onChange={e => setForm({ ...form, serviceType: e.target.value })} placeholder="e.g. SEPTIC_TANK_EMPTYING" /></div>
              <div className="field"><label>Municipality</label><input value={form.municipality} onChange={e => setForm({ ...form, municipality: e.target.value })} /></div>
              <div className="field"><label>Customer Type</label>
                <select value={form.customerType} onChange={e => setForm({ ...form, customerType: e.target.value })}>
                  <option value="">— any —</option>
                  <option>RESIDENTIAL</option><option>BUSINESS</option><option>MUNICIPAL</option>
                </select>
              </div>
              <div className="field"><label>Min Amount (€)</label><input type="number" step="0.01" value={form.minAmount} onChange={e => setForm({ ...form, minAmount: e.target.value })} /></div>
              <div className="field"><label>Period</label><input value={form.period} onChange={e => setForm({ ...form, period: e.target.value })} placeholder="e.g. 2025-Q1" /></div>
              <div className="field"><label>Service Responsibility</label>
                <select value={form.serviceResponsibility} onChange={e => setForm({ ...form, serviceResponsibility: e.target.value })}>
                  <option value="">— any —</option>
                  <option value="PUBLIC_LAW">PUBLIC_LAW</option>
                  <option value="PRIVATE_LAW">PRIVATE_LAW</option>
                </select>
              </div>
              <div className="field"><label>Description</label><input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></div>
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
