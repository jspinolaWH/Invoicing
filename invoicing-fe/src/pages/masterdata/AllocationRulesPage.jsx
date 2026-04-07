import { useEffect, useState } from 'react'
import {
  getAllocationRules, createAllocationRule, updateAllocationRule,
  deleteAllocationRule, resolveAllocationRule,
} from '../../api/allocationRules'
import { getProducts } from '../../api/products'
import { getAccounts } from '../../api/accountingAccounts'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-291', label: '3.4.21 Accounting allocation rules', href: 'https://ioteelab.atlassian.net/browse/PD-291' },
]

const emptyForm = { productId: '', accountingAccountId: '', region: '', municipality: '', description: '' }

export default function AllocationRulesPage() {
  const [rules, setRules] = useState([])
  const [products, setProducts] = useState([])
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editingRule, setEditingRule] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)

  // Resolve test panel
  const [resolveProductId, setResolveProductId] = useState('')
  const [resolveRegion, setResolveRegion] = useState('')
  const [resolveMunicipality, setResolveMunicipality] = useState('')
  const [resolveResult, setResolveResult] = useState(null)
  const [resolveError, setResolveError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getAllocationRules()
      setRules(res.data)
    } catch {
      setError('Failed to load allocation rules.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    getProducts().then(r => setProducts(r.data)).catch(() => {})
    getAccounts().then(r => setAccounts(r.data)).catch(() => {})
  }, [])

  const openAdd = () => {
    setEditingRule(null)
    setForm(emptyForm)
    setFormError(null)
    setShowModal(true)
  }

  const openEdit = (rule) => {
    setEditingRule(rule)
    setForm({
      productId: rule.productId ?? '',
      accountingAccountId: rule.accountingAccountId ?? '',
      region: rule.region ?? '',
      municipality: rule.municipality ?? '',
      description: rule.description ?? '',
    })
    setFormError(null)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditingRule(null)
    setForm(emptyForm)
    setFormError(null)
  }

  const handleSave = async () => {
    if (!form.productId || !form.accountingAccountId) {
      setFormError('Product and Accounting Account are required.')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      const payload = {
        productId: Number(form.productId),
        accountingAccountId: Number(form.accountingAccountId),
        region: form.region || null,
        municipality: form.municipality || null,
        description: form.description || null,
      }
      if (editingRule) {
        await updateAllocationRule(editingRule.id, payload)
      } else {
        await createAllocationRule(payload)
      }
      closeModal()
      load()
      setSuccessMsg(editingRule ? 'Rule updated.' : 'Rule created.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch {
      setFormError('Save failed. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Deactivate this allocation rule?')) return
    try {
      await deleteAllocationRule(id)
      load()
      setSuccessMsg('Rule deactivated.')
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch {
      setError('Delete failed.')
    }
  }

  const handleResolve = async () => {
    if (!resolveProductId) {
      setResolveError('Product is required for resolve.')
      return
    }
    setResolveError(null)
    setResolveResult(null)
    try {
      const res = await resolveAllocationRule({
        productId: resolveProductId,
        region: resolveRegion || undefined,
        municipality: resolveMunicipality || undefined,
      })
      setResolveResult(res.data)
    } catch {
      setResolveError('No matching rule found for the given inputs.')
    }
  }

  const specificityLabel = (score) => {
    if (score === 3) return 'Product + Region + Municipality'
    if (score === 2) return 'Product + Region'
    return 'Product only'
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Accounting Allocation Rules</h1>
          <p>Map products to accounting accounts; use region / municipality for higher specificity</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Rule</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Region</th>
              <th>Municipality</th>
              <th>Account</th>
              <th>Specificity</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rules.length === 0 ? (
              <tr><td colSpan={7} className="empty">No allocation rules found.</td></tr>
            ) : (
              rules.map((r) => (
                <tr key={r.id} style={{ opacity: r.active ? 1 : 0.5 }}>
                  <td><span className="code-badge">{r.productCode}</span> {r.productName}</td>
                  <td>{r.region ?? <span className="muted">—</span>}</td>
                  <td>{r.municipality ?? <span className="muted">—</span>}</td>
                  <td>{r.accountingAccountCode} — {r.accountingAccountName}</td>
                  <td><span className="code-badge">{specificityLabel(r.specificityScore)}</span></td>
                  <td>{r.active ? 'Yes' : <span className="muted">No</span>}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(r)}>Edit</button>
                    {r.active && (
                      <button className="btn-danger" onClick={() => handleDelete(r.id)}>Deactivate</button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}

      <div style={{ marginTop: 'var(--space-8)', padding: 'var(--space-6)', background: 'var(--color-bg-subtle, #f9fafb)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)' }}>
        <h3 style={{ marginTop: 0 }}>Resolve Test</h3>
        <p className="muted">Test which rule would be selected for a given product / region / municipality.</p>
        <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label>Product</label>
            <select value={resolveProductId} onChange={e => setResolveProductId(e.target.value)}>
              <option value="">— select —</option>
              {products.map(p => <option key={p.id} value={p.id}>{p.code}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label>Region</label>
            <input value={resolveRegion} onChange={e => setResolveRegion(e.target.value)} placeholder="optional" />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label>Municipality</label>
            <input value={resolveMunicipality} onChange={e => setResolveMunicipality(e.target.value)} placeholder="optional" />
          </div>
          <button className="btn-secondary" onClick={handleResolve}>Resolve</button>
        </div>
        {resolveError && <div className="error-msg" style={{ marginTop: 'var(--space-3)' }}>{resolveError}</div>}
        {resolveResult && (
          <div style={{ marginTop: 'var(--space-4)', padding: 'var(--space-4)', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 'var(--radius-sm)' }}>
            <strong>Matched rule #{resolveResult.matchedRuleId}</strong> — specificity {resolveResult.specificityScore}<br />
            Account: <span className="code-badge">{resolveResult.accountingAccountCode}</span> {resolveResult.accountingAccountName}<br />
            <span className="muted">{resolveResult.matchReason}</span>
          </div>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingRule ? 'Edit Rule' : 'Add Allocation Rule'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">
              <div className="field">
                <label>Product <span className="required">*</span></label>
                <select value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value })}>
                  <option value="">— select —</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.code}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Accounting Account <span className="required">*</span></label>
                <select value={form.accountingAccountId} onChange={e => setForm({ ...form, accountingAccountId: e.target.value })}>
                  <option value="">— select —</option>
                  {accounts.map(a => <option key={a.id} value={a.id}>{a.code} — {a.name}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Region <span className="optional">(optional)</span></label>
                <input value={form.region} onChange={e => setForm({ ...form, region: e.target.value })} placeholder="e.g. Helsinki" />
              </div>
              <div className="field">
                <label>Municipality <span className="optional">(optional)</span></label>
                <input value={form.municipality} onChange={e => setForm({ ...form, municipality: e.target.value })} placeholder="e.g. MUN-01" />
              </div>
              <div className="field">
                <label>Description</label>
                <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="optional note" />
              </div>
              {formError && <div className="error-msg">{formError}</div>}
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeModal} disabled={saving}>Cancel</button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
