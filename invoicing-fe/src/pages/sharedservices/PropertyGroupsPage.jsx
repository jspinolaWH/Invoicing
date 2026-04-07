import { useEffect, useState } from 'react'
import { getPropertyGroups, createPropertyGroup, getPropertyGroup, replaceParticipants, validatePropertyGroup } from '../../api/propertyGroups'
import '../masterdata/VatRatesPage.css'

export default function PropertyGroupsPage() {
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newName, setNewName] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [saving, setSaving] = useState(false)
  const [selectedGroup, setSelectedGroup] = useState(null)
  const [groupDetail, setGroupDetail] = useState(null)
  const [participants, setParticipants] = useState([])
  const [validationResult, setValidationResult] = useState(null)

  const load = async () => {
    setLoading(true); setError(null)
    try { setGroups((await getPropertyGroups()).data) }
    catch { setError('Failed to load property groups.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    if (!newName.trim()) return
    setSaving(true)
    try {
      await createPropertyGroup({ name: newName.trim(), description: newDesc.trim() || null })
      setShowCreateModal(false); setNewName(''); setNewDesc('')
      setSuccessMsg('Group created.'); setTimeout(() => setSuccessMsg(null), 3000)
      load()
    } catch { setError('Create failed.') }
    finally { setSaving(false) }
  }

  const openDetail = async (group) => {
    setSelectedGroup(group)
    const detail = (await getPropertyGroup(group.id)).data
    setGroupDetail(detail)
    setParticipants(detail.participants.map(p => ({ ...p })))
    const v = (await validatePropertyGroup(group.id)).data
    setValidationResult(v)
  }

  const handleSaveParticipants = async () => {
    setSaving(true)
    try {
      await replaceParticipants(selectedGroup.id, participants.map(p => ({
        customerNumber: p.customerNumber,
        sharePercentage: parseFloat(p.sharePercentage),
        validFrom: p.validFrom,
        validTo: p.validTo || null,
      })))
      const v = (await validatePropertyGroup(selectedGroup.id)).data
      setValidationResult(v)
      const detail = (await getPropertyGroup(selectedGroup.id)).data
      setGroupDetail(detail)
      setParticipants(detail.participants.map(p => ({ ...p })))
      setSuccessMsg('Participants saved.'); setTimeout(() => setSuccessMsg(null), 3000)
      load()
    } catch (e) {
      setError(e?.response?.data || 'Save failed — shares may not sum to 100%.')
    } finally { setSaving(false) }
  }

  const addParticipantRow = () => {
    setParticipants(prev => [...prev, { customerNumber: '', sharePercentage: '', validFrom: '', validTo: '' }])
  }

  const removeParticipantRow = (idx) => {
    setParticipants(prev => prev.filter((_, i) => i !== idx))
  }

  const totalShare = participants.reduce((sum, p) => sum + (parseFloat(p.sharePercentage) || 0), 0)
  const totalValid = Math.abs(totalShare - 100) < 0.01

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Property Groups</h1>
          <p>Shared service arrangements — participants and split percentages</p>
        </div>
        <button className="btn-primary" onClick={() => setShowCreateModal(true)}>+ New Group</button>
      </div>

      {successMsg && <div className="success-msg">{successMsg}</div>}
      {error && <div className="error-msg" onClick={() => setError(null)}>{error}</div>}

      <div style={{ display: 'flex', gap: 'var(--space-4)' }}>
        <div style={{ flex: 1 }}>
          {loading ? <div className="loading">Loading...</div> : (
            <table className="data-table">
              <thead><tr><th>Name</th><th>Participants</th><th>Total %</th><th>Valid</th><th>Actions</th></tr></thead>
              <tbody>
                {groups.length === 0 ? <tr><td colSpan={5} className="empty">No property groups.</td></tr>
                  : groups.map(g => (
                    <tr key={g.id}>
                      <td>{g.name}</td>
                      <td>{g.participantCount}</td>
                      <td style={{ color: g.valid ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
                        {g.totalSharePercentage?.toFixed(2)}%
                      </td>
                      <td>{g.valid ? '✓' : '✗'}</td>
                      <td className="actions">
                        <button className="btn-secondary" onClick={() => openDetail(g)}>Manage</button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          )}
        </div>

        {selectedGroup && groupDetail && (
          <div style={{ flex: 2, border: '1px solid var(--border)', borderRadius: 8, padding: 'var(--space-4)' }}>
            <h3>{selectedGroup.name}</h3>
            {selectedGroup.description && <p className="muted">{selectedGroup.description}</p>}

            <div style={{
              padding: 'var(--space-2) var(--space-3)',
              borderRadius: 6,
              marginBottom: 'var(--space-3)',
              background: totalValid ? '#dcfce7' : '#fee2e2',
              color: totalValid ? '#166534' : '#991b1b',
              fontWeight: 600,
            }}>
              Total: {totalShare.toFixed(2)}% {totalValid ? '✓ Valid' : '✗ Must equal 100%'}
            </div>

            <table className="data-table">
              <thead>
                <tr><th>Customer Number</th><th>Share %</th><th>Valid From</th><th>Valid To</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {participants.map((p, idx) => (
                  <tr key={idx}>
                    <td><input value={p.customerNumber} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, customerNumber: e.target.value } : x))} style={{ width: 120 }} /></td>
                    <td><input type="number" step="0.01" value={p.sharePercentage} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, sharePercentage: e.target.value } : x))} style={{ width: 80 }} /></td>
                    <td><input type="date" value={p.validFrom ?? ''} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, validFrom: e.target.value } : x))} /></td>
                    <td><input type="date" value={p.validTo ?? ''} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, validTo: e.target.value || null } : x))} /></td>
                    <td><button className="btn-danger" onClick={() => removeParticipantRow(idx)}>Remove</button></td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div style={{ marginTop: 'var(--space-3)', display: 'flex', gap: 'var(--space-3)' }}>
              <button className="btn-secondary" onClick={addParticipantRow}>+ Add Row</button>
              <button className="btn-primary" onClick={handleSaveParticipants} disabled={saving || !totalValid}>
                {saving ? 'Saving…' : 'Save Participants'}
              </button>
            </div>
          </div>
        )}
      </div>

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>New Property Group</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="field"><label>Name *</label><input value={newName} onChange={e => setNewName(e.target.value)} /></div>
              <div className="field"><label>Description</label><input value={newDesc} onChange={e => setNewDesc(e.target.value)} /></div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>Cancel</button>
              <button className="btn-primary" onClick={handleCreate} disabled={saving}>{saving ? 'Saving…' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
