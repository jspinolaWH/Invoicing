import { useState, useEffect } from 'react';
import {
  fetchThresholdConfigs,
  createThresholdConfig,
  updateThresholdConfig,
  deleteThresholdConfig,
} from '../../api/billingThreshold';

const SERVICE_RESPONSIBILITY_OPTIONS = [
  'MUNICIPALITY',
  'PRIVATE_COLLECTOR',
  'CONTRACT_HOLDER',
  'SHARED',
  'OWNER',
];

const EMPTY_FORM = {
  serviceResponsibility: SERVICE_RESPONSIBILITY_OPTIONS[0],
  annualEuroLimit: '',
  description: '',
  active: true,
};

export default function BillingThresholdConfigPage() {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    try {
      setConfigs(await fetchThresholdConfigs());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    try {
      const payload = { ...form, annualEuroLimit: Number(form.annualEuroLimit) };
      if (editingId) {
        await updateThresholdConfig(editingId, payload);
      } else {
        await createThresholdConfig(payload);
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      setShowForm(false);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Deactivate this threshold config?')) return;
    try {
      await deleteThresholdConfig(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  function startEdit(config) {
    setForm({
      serviceResponsibility: config.serviceResponsibility,
      annualEuroLimit: config.annualEuroLimit,
      description: config.description || '',
      active: config.active,
    });
    setEditingId(config.id);
    setShowForm(true);
  }

  function cancelForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setShowForm(false);
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <h2 style={{ margin: 0 }}>Billing Threshold Configuration</h2>
        {!showForm && (
          <button
            onClick={() => setShowForm(true)}
            style={{ padding: '8px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
          >
            + Add Threshold
          </button>
        )}
      </div>

      {error && (
        <div style={{ color: '#dc2626', marginBottom: 16, padding: 12, background: '#fee2e2', borderRadius: 6 }}>
          {error}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} style={{ background: '#f9f9f9', padding: 20, borderRadius: 8, marginBottom: 24 }}>
          <h3 style={{ marginTop: 0 }}>{editingId ? 'Edit Threshold' : 'New Threshold'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 12 }}>
            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Service Responsibility *</label>
              <select
                value={form.serviceResponsibility}
                onChange={ev => setForm(f => ({ ...f, serviceResponsibility: ev.target.value }))}
                style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }}
              >
                {SERVICE_RESPONSIBILITY_OPTIONS.map(o => <option key={o} value={o}>{o}</option>)}
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Annual Euro Limit (€) *</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.annualEuroLimit}
                onChange={ev => setForm(f => ({ ...f, annualEuroLimit: ev.target.value }))}
                style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }}
                required
              />
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Description</label>
              <input
                type="text"
                value={form.description}
                onChange={ev => setForm(f => ({ ...f, description: ev.target.value }))}
                style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }}
              />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              {editingId ? 'Update' : 'Create'}
            </button>
            <button type="button" onClick={cancelForm} style={{ padding: '8px 16px', background: '#fff', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p style={{ color: '#6b7280' }}>Loading…</p>
      ) : configs.length === 0 ? (
        <p style={{ color: '#6b7280' }}>No threshold configs configured yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead>
            <tr style={{ background: '#f3f4f6' }}>
              {['Service Responsibility', 'Annual Euro Limit', 'Description', 'Created By', ''].map(h => (
                <th key={h} style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {configs.map(c => (
              <tr key={c.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                <td style={{ padding: '8px 12px', fontWeight: 600 }}>{c.serviceResponsibility}</td>
                <td style={{ padding: '8px 12px' }}>€ {Number(c.annualEuroLimit).toLocaleString('de-DE', { minimumFractionDigits: 2 })}</td>
                <td style={{ padding: '8px 12px', color: '#6b7280' }}>{c.description || '—'}</td>
                <td style={{ padding: '8px 12px', color: '#6b7280', fontSize: 12 }}>{c.createdBy || '—'}</td>
                <td style={{ padding: '8px 12px' }}>
                  <button onClick={() => startEdit(c)} style={{ marginRight: 8, padding: '3px 10px', fontSize: 12, background: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer' }}>Edit</button>
                  <button onClick={() => handleDelete(c.id)} style={{ padding: '3px 10px', fontSize: 12, background: '#fee2e2', border: '1px solid #fca5a5', borderRadius: 4, cursor: 'pointer', color: '#dc2626' }}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
