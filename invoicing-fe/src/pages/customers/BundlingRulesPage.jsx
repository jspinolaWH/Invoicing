import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getBundlingRules, replaceBundlingRules } from '../../api/bundlingRules'
import '../masterdata/VatRatesPage.css'

export default function BundlingRulesPage() {
  const { customerNumber } = useParams()
  const navigate = useNavigate()
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)
  const [newGroup, setNewGroup] = useState('')
  const [newType, setNewType] = useState('SEPARATE')

  const load = async () => {
    setLoading(true); setError(null)
    try { setRules((await getBundlingRules(customerNumber)).data) }
    catch { setError('Failed to load bundling rules.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [customerNumber])

  const toggleType = (idx) => {
    setRules(prev => prev.map((r, i) => i === idx ? { ...r, bundlingType: r.bundlingType === 'SINGLE_LINE' ? 'SEPARATE' : 'SINGLE_LINE' } : r))
  }

  const removeRule = (idx) => {
    setRules(prev => prev.filter((_, i) => i !== idx))
  }

  const addRule = () => {
    if (!newGroup.trim()) return
    setRules(prev => [...prev, { productGroup: newGroup.trim().toUpperCase(), bundlingType: newType, description: '' }])
    setNewGroup(''); setNewType('SEPARATE')
  }

  const handleSave = async () => {
    setSaving(true); setError(null)
    try {
      await replaceBundlingRules(customerNumber, rules.map(r => ({ productGroup: r.productGroup, bundlingType: r.bundlingType, description: r.description ?? null })))
      setSuccessMsg('Bundling rules saved.')
      setTimeout(() => setSuccessMsg(null), 3000)
      load()
    } catch { setError('Save failed. Check for duplicate product groups.') }
    finally { setSaving(false) }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Bundling Rules — {customerNumber}</h1>
          <p>Configure how billing events are grouped on invoices for this customer</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {loading ? <div className="loading">Loading...</div> : (
        <>
          <table className="data-table">
            <thead><tr><th>Product Group</th><th>Bundling Type</th><th>Description</th><th>Actions</th></tr></thead>
            <tbody>
              {rules.length === 0
                ? <tr><td colSpan={4} className="empty">No bundling rules defined. Default is SEPARATE for all groups.</td></tr>
                : rules.map((r, idx) => (
                  <tr key={idx}>
                    <td><span className="code-badge">{r.productGroup}</span></td>
                    <td>
                      <button className="btn-secondary" onClick={() => toggleType(idx)} style={{ minWidth: 110 }}>
                        {r.bundlingType}
                      </button>
                    </td>
                    <td>
                      <input value={r.description ?? ''} onChange={e => setRules(prev => prev.map((x, i) => i === idx ? { ...x, description: e.target.value } : x))} placeholder="note (optional)" style={{ width: '100%' }} />
                    </td>
                    <td className="actions">
                      <button className="btn-danger" onClick={() => removeRule(idx)}>Remove</button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>

          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', marginTop: 'var(--space-4)', flexWrap: 'wrap' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Add Product Group</label>
              <input value={newGroup} onChange={e => setNewGroup(e.target.value)} placeholder="e.g. CONTAINER_EMPTYING" onKeyDown={e => e.key === 'Enter' && addRule()} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Type</label>
              <select value={newType} onChange={e => setNewType(e.target.value)}>
                <option value="SINGLE_LINE">SINGLE_LINE</option>
                <option value="SEPARATE">SEPARATE</option>
              </select>
            </div>
            <button className="btn-secondary" onClick={addRule}>Add Row</button>
          </div>

          <div style={{ marginTop: 'var(--space-6)' }}>
            <button className="btn-primary" onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save Changes'}</button>
          </div>
        </>
      )}
    </div>
  )
}
