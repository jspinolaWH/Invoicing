import { useEffect, useState } from 'react'
import { getPropertyGroups, createPropertyGroup, getPropertyGroup, replaceParticipants, validatePropertyGroup, addParticipantRetroactive } from '../../api/propertyGroups'
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
  const [showRetroModal, setShowRetroModal] = useState(false)
  const [retroForm, setRetroForm] = useState({ customerNumber: '', sharePercentage: '', validFrom: '' })
  const [retroSaving, setRetroSaving] = useState(false)

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
        includeIfZeroShare: !!p.includeIfZeroShare,
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
    setParticipants(prev => [...prev, { customerNumber: '', sharePercentage: '', validFrom: '', validTo: '', includeIfZeroShare: false }])
  }

  const handleEqualise = () => {
    const n = participants.length
    if (n === 0) return
    const base = Math.floor(100 / n * 100) / 100
    const lastShare = parseFloat((100 - base * (n - 1)).toFixed(2))
    setParticipants(prev => prev.map((p, i) => ({ ...p, sharePercentage: i === n - 1 ? lastShare : base })))
  }

  const removeParticipantRow = (idx) => {
    setParticipants(prev => prev.filter((_, i) => i !== idx))
  }

  const handleAddRetroactive = async () => {
    if (!retroForm.customerNumber.trim() || !retroForm.sharePercentage || !retroForm.validFrom) return
    setRetroSaving(true)
    try {
      await addParticipantRetroactive(selectedGroup.id, {
        customerNumber: retroForm.customerNumber.trim(),
        sharePercentage: parseFloat(retroForm.sharePercentage),
        validFrom: retroForm.validFrom,
        adjustOtherParticipants: false,
      })
      setShowRetroModal(false)
      setRetroForm({ customerNumber: '', sharePercentage: '', validFrom: '' })
      setSuccessMsg('Participant added retroactively — affected events have been redistributed.')
      setTimeout(() => setSuccessMsg(null), 5000)
      const detail = (await getPropertyGroup(selectedGroup.id)).data
      setGroupDetail(detail)
      setParticipants(detail.participants.map(p => ({ ...p })))
      const v = (await validatePropertyGroup(selectedGroup.id)).data
      setValidationResult(v)
      load()
    } catch (e) {
      setError(e?.response?.data || 'Retroactive add failed.')
    } finally { setRetroSaving(false) }
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
                <tr><th>Customer Number</th><th>Share %</th><th>Valid From</th><th>Valid To</th><th>Include if 0%</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {participants.map((p, idx) => (
                  <tr key={idx}>
                    <td><input value={p.customerNumber} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, customerNumber: e.target.value } : x))} style={{ width: 120 }} /></td>
                    <td><input type="number" step="0.01" value={p.sharePercentage} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, sharePercentage: e.target.value } : x))} style={{ width: 80 }} /></td>
                    <td><input type="date" value={p.validFrom ?? ''} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, validFrom: e.target.value } : x))} /></td>
                    <td><input type="date" value={p.validTo ?? ''} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, validTo: e.target.value || null } : x))} /></td>
                    <td style={{ textAlign: 'center' }}><input type="checkbox" checked={!!p.includeIfZeroShare} onChange={e => setParticipants(prev => prev.map((x, i) => i === idx ? { ...x, includeIfZeroShare: e.target.checked } : x))} /></td>
                    <td><button className="btn-danger" onClick={() => removeParticipantRow(idx)}>Remove</button></td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div style={{ marginTop: 'var(--space-3)', display: 'flex', gap: 'var(--space-3)' }}>
              <button className="btn-secondary" onClick={addParticipantRow}>+ Add Row</button>
              <button className="btn-secondary" onClick={handleEqualise} disabled={participants.length === 0}>Equalise</button>
              <button className="btn-secondary" onClick={() => { setRetroForm({ customerNumber: '', sharePercentage: '', validFrom: '' }); setShowRetroModal(true) }}>
                + Add Retroactively
              </button>
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

      {showRetroModal && selectedGroup && (
        <div className="modal-overlay" onClick={() => setShowRetroModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add Participant Retroactively</h2>
              <button className="modal-close" onClick={() => setShowRetroModal(false)}>×</button>
            </div>
            <div className="modal-body">
              <p className="muted" style={{ marginBottom: 'var(--space-3)' }}>
                The new participant will be added to <strong>{selectedGroup.name}</strong> from the specified date.
                All events from that date onwards will be redistributed with the updated participant set.
              </p>
              <div className="field">
                <label>Customer Number *</label>
                <input value={retroForm.customerNumber} onChange={e => setRetroForm(f => ({ ...f, customerNumber: e.target.value }))} />
              </div>
              <div className="field">
                <label>Share % *</label>
                <input type="number" step="0.01" min="0" max="100" value={retroForm.sharePercentage} onChange={e => setRetroForm(f => ({ ...f, sharePercentage: e.target.value }))} />
              </div>
              <div className="field">
                <label>Valid From (historical date) *</label>
                <input type="date" value={retroForm.validFrom} onChange={e => setRetroForm(f => ({ ...f, validFrom: e.target.value }))} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowRetroModal(false)}>Cancel</button>
              <button
                className="btn-primary"
                onClick={handleAddRetroactive}
                disabled={retroSaving || !retroForm.customerNumber.trim() || !retroForm.sharePercentage || !retroForm.validFrom}
              >
                {retroSaving ? 'Adding…' : 'Add & Redistribute'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
