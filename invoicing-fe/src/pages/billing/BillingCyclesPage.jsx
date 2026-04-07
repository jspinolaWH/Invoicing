import { useEffect, useState } from 'react'
import { getBillingCycles, createBillingCycle, updateBillingCycle } from '../../api/billingCycles'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-291', label: '3.4.21 Billing cycles', href: 'https://ioteelab.atlassian.net/browse/PD-291' },
]

const FREQUENCIES = ['MONTHLY', 'QUARTERLY', 'ANNUAL']
const emptyForm = { customerNumber: '', frequency: 'MONTHLY', nextBillingDate: '', description: '', contractReference: '', propertyReference: '', serviceType: '', active: true }

export default function BillingCyclesPage() {
  const [cycles, setCycles] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [customerFilter, setCustomerFilter] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  const load = async (cn = '') => {
    setLoading(true); setError(null)
    try { setCycles((await getBillingCycles(cn || undefined)).data) }
    catch { setError('Failed to load billing cycles.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => { setEditing(null); setForm(emptyForm); setFormError(null); setShowModal(true) }
  const openEdit = (c) => {
    setEditing(c)
    setForm({ customerNumber: c.customerNumber, frequency: c.frequency, nextBillingDate: c.nextBillingDate, description: c.description ?? '', contractReference: c.contractReference ?? '', propertyReference: c.propertyReference ?? '', serviceType: c.serviceType ?? '', active: c.active })
    setFormError(null); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditing(null) }

  const handleSave = async () => {
    if (!form.customerNumber || !form.nextBillingDate) { setFormError('Customer number and next billing date are required.'); return }
    setSaving(true); setFormError(null)
    try {
      if (editing) await updateBillingCycle(editing.id, form)
      else await createBillingCycle(form)
      close(); load(customerFilter)
      setSuccessMsg(editing ? 'Cycle updated.' : 'Cycle created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch { setFormError('Save failed.') }
    finally { setSaving(false) }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Billing Cycles</h1>
          <p>Manage invoicing schedules per customer, contract, or property</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Cycle</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      <div className="filter-bar">
        <label>Filter by customer:</label>
        <input value={customerFilter} onChange={e => { setCustomerFilter(e.target.value); load(e.target.value) }} placeholder="Customer number…" />
        {customerFilter && <button className="btn-link" onClick={() => { setCustomerFilter(''); load('') }}>Clear</button>}
      </div>

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? <div className="loading">Loading...</div> : (
        <table className="data-table">
          <thead><tr><th>Customer #</th><th>Frequency</th><th>Next Billing Date</th><th>Service Type</th><th>Description</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {cycles.length === 0
              ? <tr><td colSpan={7} className="empty">No billing cycles found.</td></tr>
              : cycles.map(c => (
                <tr key={c.id} style={{ opacity: c.active ? 1 : 0.5 }}>
                  <td><code>{c.customerNumber}</code></td>
                  <td><span className="code-badge">{c.frequency}</span></td>
                  <td>{c.nextBillingDate}</td>
                  <td>{c.serviceType ?? <span className="muted">—</span>}</td>
                  <td>{c.description ?? <span className="muted">—</span>}</td>
                  <td>{c.active ? 'Yes' : <span className="muted">No</span>}</td>
                  <td className="actions"><button className="btn-secondary" onClick={() => openEdit(c)}>Edit</button></td>
                </tr>
              ))}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={close}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Billing Cycle' : 'Add Billing Cycle'}</h2>
              <button className="modal-close" onClick={close}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Customer Number <span className="required">*</span></label>
                <input value={form.customerNumber} onChange={e => setForm({ ...form, customerNumber: e.target.value })} disabled={!!editing} />
              </div>
              <div className="field"><label>Frequency <span className="required">*</span></label>
                <select value={form.frequency} onChange={e => setForm({ ...form, frequency: e.target.value })}>
                  {FREQUENCIES.map(f => <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
              <div className="field"><label>Next Billing Date <span className="required">*</span></label>
                <input type="date" value={form.nextBillingDate} onChange={e => setForm({ ...form, nextBillingDate: e.target.value })} />
              </div>
              <div className="field"><label>Service Type</label>
                <input value={form.serviceType} onChange={e => setForm({ ...form, serviceType: e.target.value })} placeholder="e.g. CONTAINER_EMPTYING" />
              </div>
              <div className="field"><label>Contract Reference</label>
                <input value={form.contractReference} onChange={e => setForm({ ...form, contractReference: e.target.value })} />
              </div>
              <div className="field"><label>Property Reference</label>
                <input value={form.propertyReference} onChange={e => setForm({ ...form, propertyReference: e.target.value })} />
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
