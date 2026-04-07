import { useEffect, useState } from 'react'
import { getSeasonalFees, createSeasonalFee, updateSeasonalFee, deleteSeasonalFee, generateNow } from '../../api/seasonalFees'
import { getProducts } from '../../api/products'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-288', label: '3.4.28 Seasonal fees / periodic charges', href: 'https://ioteelab.atlassian.net/browse/PD-288' },
]
const FREQUENCIES = ['MONTHLY', 'QUARTERLY', 'ANNUAL']
const emptyForm = { customerNumber: '', productId: '', billingFrequency: 'ANNUAL', amount: '', nextDueDate: '', propertyReference: '', description: '', active: true }

export default function SeasonalFeesPage() {
  const [fees, setFees] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [customerFilter, setCustomerFilter] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)
  const [generateMsg, setGenerateMsg] = useState({})

  const load = async (cn = '') => {
    setLoading(true); setError(null)
    try { setFees((await getSeasonalFees(cn || undefined)).data) }
    catch { setError('Failed to load seasonal fees.') }
    finally { setLoading(false) }
  }

  useEffect(() => {
    load()
    getProducts().then(r => setProducts(r.data)).catch(() => {})
  }, [])

  const openAdd = () => { setEditing(null); setForm(emptyForm); setFormError(null); setShowModal(true) }
  const openEdit = (f) => {
    setEditing(f)
    setForm({ customerNumber: f.customerNumber, productId: f.productId, billingFrequency: f.billingFrequency, amount: f.amount, nextDueDate: f.nextDueDate, propertyReference: f.propertyReference ?? '', description: f.description ?? '', active: f.active })
    setFormError(null); setShowModal(true)
  }
  const close = () => { setShowModal(false); setEditing(null) }

  const handleSave = async () => {
    if (!form.customerNumber || !form.productId || !form.amount || !form.nextDueDate) { setFormError('Customer, product, amount, and next due date are required.'); return }
    setSaving(true); setFormError(null)
    try {
      const payload = { ...form, productId: Number(form.productId), amount: parseFloat(form.amount) }
      if (editing) await updateSeasonalFee(editing.id, payload)
      else await createSeasonalFee(payload)
      close(); load(customerFilter)
      setSuccessMsg(editing ? 'Seasonal fee updated.' : 'Seasonal fee created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch { setFormError('Save failed.') }
    finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this seasonal fee config?')) return
    try { await deleteSeasonalFee(id); load(customerFilter) }
    catch { setError('Delete failed.') }
  }

  const handleGenerate = async (id) => {
    try {
      const res = await generateNow(id)
      const msg = `Event #${res.data.billingEventId} created. Next due: ${res.data.newNextDueDate}`
      setGenerateMsg(prev => ({ ...prev, [id]: msg }))
      load(customerFilter)
      setTimeout(() => setGenerateMsg(prev => { const n = { ...prev }; delete n[id]; return n }), 5000)
    } catch { setError('Generate failed.') }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Seasonal Fees</h1>
          <p>Automated periodic fee generation — annual base fees, quarterly rents, monthly charges</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Seasonal Fee</button>
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
          <thead><tr><th>Customer #</th><th>Product</th><th>Frequency</th><th>Amount (€)</th><th>Next Due Date</th><th>Property Ref</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {fees.length === 0
              ? <tr><td colSpan={8} className="empty">No seasonal fees found.</td></tr>
              : fees.map(f => (
                <tr key={f.id} style={{ opacity: f.active ? 1 : 0.5 }}>
                  <td><code>{f.customerNumber}</code></td>
                  <td>{f.productName}</td>
                  <td><span className="code-badge">{f.billingFrequency}</span></td>
                  <td>{parseFloat(f.amount).toFixed(2)}</td>
                  <td>{f.nextDueDate}</td>
                  <td>{f.propertyReference ?? <span className="muted">—</span>}</td>
                  <td>{f.active ? 'Yes' : <span className="muted">No</span>}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(f)}>Edit</button>
                    {f.active && <>
                      <button className="btn-secondary" onClick={() => handleGenerate(f.id)}>Generate Now</button>
                      <button className="btn-danger" onClick={() => handleDelete(f.id)}>Deactivate</button>
                    </>}
                    {generateMsg[f.id] && <span className="success-msg" style={{ fontSize: '0.8rem', display: 'block' }}>{generateMsg[f.id]}</span>}
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
              <h2>{editing ? 'Edit Seasonal Fee' : 'Add Seasonal Fee'}</h2>
              <button className="modal-close" onClick={close}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Customer Number <span className="required">*</span></label>
                <input value={form.customerNumber} onChange={e => setForm({ ...form, customerNumber: e.target.value })} disabled={!!editing} />
              </div>
              <div className="field"><label>Product <span className="required">*</span></label>
                <select value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value })}>
                  <option value="">— select —</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.code}</option>)}
                </select>
              </div>
              <div className="field"><label>Billing Frequency <span className="required">*</span></label>
                <select value={form.billingFrequency} onChange={e => setForm({ ...form, billingFrequency: e.target.value })}>
                  {FREQUENCIES.map(f => <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
              <div className="field"><label>Amount (€ net) <span className="required">*</span></label>
                <input type="number" step="0.01" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} />
              </div>
              <div className="field"><label>Next Due Date <span className="required">*</span></label>
                <input type="date" value={form.nextDueDate} onChange={e => setForm({ ...form, nextDueDate: e.target.value })} />
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
