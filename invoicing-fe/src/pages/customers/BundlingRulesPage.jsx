import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getBundlingRules, replaceBundlingRules, getBundlingRuleAuditLog } from '../../api/bundlingRules'
import { checkCustomerLock } from '../../api/locks'
import CustomerLockedBadge from '../../components/CustomerLockedBadge'
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
  const [auditLog, setAuditLog] = useState([])
  const [auditLoading, setAuditLoading] = useState(false)
  const [showAudit, setShowAudit] = useState(false)
  const [isLocked, setIsLocked] = useState(false)

  const load = async () => {
    setLoading(true); setError(null)
    try { setRules((await getBundlingRules(customerNumber)).data) }
    catch { setError('Failed to load bundling rules.') }
    finally { setLoading(false) }
  }

  const loadAuditLog = async () => {
    setAuditLoading(true)
    try { setAuditLog((await getBundlingRuleAuditLog(customerNumber)).data) }
    catch { /* silently ignore */ }
    finally { setAuditLoading(false) }
  }

  useEffect(() => { load() }, [customerNumber])

  useEffect(() => {
    if (!customerNumber) return
    checkCustomerLock(customerNumber)
      .then(res => setIsLocked(res.data.locked))
      .catch(() => {})
  }, [customerNumber])

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
          <h1>
            Bundling Rules — {customerNumber}
            <CustomerLockedBadge customerNumber={customerNumber} />
          </h1>
          <p>Configure how billing events are grouped on invoices for this customer</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg">{error}</div>}

      {isLocked && (
        <div style={{ marginBottom: 16, padding: '8px 12px', background: '#fffbeb', border: '1px solid #f59e0b', borderRadius: 6, color: '#92400e', fontSize: 13 }}>
          Invoice processing in progress. Bundling rule changes cannot be made during this time.
        </div>
      )}

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
                      <button className="btn-secondary" onClick={() => !isLocked && toggleType(idx)} style={{ minWidth: 110, ...(isLocked ? { opacity: 0.5, cursor: 'not-allowed' } : {}) }} disabled={isLocked}>
                        {r.bundlingType}
                      </button>
                    </td>
                    <td>
                      <input value={r.description ?? ''} onChange={e => setRules(prev => prev.map((x, i) => i === idx ? { ...x, description: e.target.value } : x))} placeholder="note (optional)" style={{ width: '100%', ...(isLocked ? { background: '#f3f4f6', color: '#6b7280', cursor: 'not-allowed' } : {}) }} disabled={isLocked} />
                    </td>
                    <td className="actions">
                      <button className="btn-danger" onClick={() => removeRule(idx)} disabled={isLocked} style={isLocked ? { opacity: 0.5, cursor: 'not-allowed' } : undefined}>Remove</button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>

          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', marginTop: 'var(--space-4)', flexWrap: 'wrap' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Add Product Group</label>
              <input value={newGroup} onChange={e => setNewGroup(e.target.value)} placeholder="e.g. CONTAINER_EMPTYING" onKeyDown={e => e.key === 'Enter' && !isLocked && addRule()} disabled={isLocked} style={isLocked ? { background: '#f3f4f6', color: '#6b7280', cursor: 'not-allowed' } : undefined} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label>Type</label>
              <select value={newType} onChange={e => setNewType(e.target.value)} disabled={isLocked} style={isLocked ? { background: '#f3f4f6', color: '#6b7280', cursor: 'not-allowed' } : undefined}>
                <option value="SINGLE_LINE">SINGLE_LINE</option>
                <option value="SEPARATE">SEPARATE</option>
              </select>
            </div>
            <button className="btn-secondary" onClick={addRule} disabled={isLocked} style={isLocked ? { opacity: 0.5, cursor: 'not-allowed' } : undefined}>Add Row</button>
          </div>

          <div style={{ marginTop: 'var(--space-6)' }}>
            <button className="btn-primary" onClick={handleSave} disabled={saving || isLocked}>{saving ? 'Saving…' : 'Save Changes'}</button>
          </div>
        </>
      )}

      <div style={{ marginTop: 'var(--space-8)', borderTop: '1px solid var(--border)', paddingTop: 'var(--space-6)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-4)' }}>
          <h2 style={{ margin: 0 }}>Change History</h2>
          <button
            className="btn-secondary"
            onClick={() => { if (!showAudit) loadAuditLog(); setShowAudit(v => !v) }}
          >
            {showAudit ? 'Hide' : 'Show'}
          </button>
        </div>

        {showAudit && (
          auditLoading
            ? <div className="loading">Loading history…</div>
            : auditLog.length === 0
              ? <p style={{ color: 'var(--text-secondary)' }}>No changes recorded yet.</p>
              : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>When</th>
                      <th>Product Group</th>
                      <th>Action</th>
                      <th>From</th>
                      <th>To</th>
                      <th>Changed By</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLog.map(entry => (
                      <tr key={entry.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>{new Date(entry.changedAt).toLocaleString()}</td>
                        <td><span className="code-badge">{entry.productGroup}</span></td>
                        <td>
                          <span style={{
                            padding: '2px 8px',
                            borderRadius: 4,
                            fontSize: '0.8rem',
                            fontWeight: 600,
                            background: entry.action === 'ADDED' ? 'var(--success-bg, #d4edda)' : entry.action === 'REMOVED' ? 'var(--danger-bg, #f8d7da)' : 'var(--warning-bg, #fff3cd)',
                            color: entry.action === 'ADDED' ? 'var(--success, #155724)' : entry.action === 'REMOVED' ? 'var(--danger, #721c24)' : 'var(--warning-text, #856404)'
                          }}>
                            {entry.action}
                          </span>
                        </td>
                        <td>{entry.oldValue ?? '—'}</td>
                        <td>{entry.newValue ?? '—'}</td>
                        <td>{entry.changedBy}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
        )}
      </div>
    </div>
  )
}
