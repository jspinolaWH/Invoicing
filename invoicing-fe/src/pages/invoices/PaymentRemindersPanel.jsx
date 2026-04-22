import { useState, useEffect } from 'react';
import { getRemindersForInvoice, createReminder, sendReminder } from '../../api/paymentReminders';

const STATUS_COLORS = { PENDING: '#d97706', SENT: '#16a34a', FAILED: '#dc2626' };

export default function PaymentRemindersPanel({ invoiceId, invoiceStatus }) {
  const [reminders, setReminders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [creating, setCreating] = useState(false);
  const [message, setMessage] = useState('');
  const [actionLoading, setActionLoading] = useState(null);

  const canCreate = invoiceStatus === 'SENT' || invoiceStatus === 'COMPLETED';

  async function load() {
    setLoading(true);
    try {
      const data = await getRemindersForInvoice(invoiceId);
      setReminders(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [invoiceId]);

  async function handleCreate(e) {
    e.preventDefault();
    setActionLoading('create');
    setError(null);
    try {
      await createReminder(invoiceId, message || null);
      setMessage('');
      setCreating(false);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(null);
    }
  }

  async function handleSend(reminderId) {
    setActionLoading(reminderId);
    setError(null);
    try {
      await sendReminder(reminderId);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(null);
    }
  }

  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 20, marginTop: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0 }}>Payment Reminders</h3>
        {canCreate && !creating && (
          <button onClick={() => setCreating(true)} style={btn('#2563eb')}>
            + New Reminder
          </button>
        )}
      </div>

      {error && (
        <div style={{ color: '#dc2626', padding: 10, background: '#fee2e2', borderRadius: 6, marginBottom: 12 }}>
          {error}
        </div>
      )}

      {creating && (
        <form onSubmit={handleCreate} style={{ background: '#f9fafb', borderRadius: 6, padding: 16, marginBottom: 16 }}>
          <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Custom Message (optional)</label>
          <textarea
            value={message}
            onChange={e => setMessage(e.target.value)}
            rows={3}
            placeholder="Leave blank to use the default reminder message"
            style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', marginBottom: 10, boxSizing: 'border-box' }}
          />
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" disabled={actionLoading === 'create'} style={btn('#2563eb')}>
              {actionLoading === 'create' ? 'Creating…' : 'Create Reminder'}
            </button>
            <button type="button" onClick={() => setCreating(false)} style={btn('#6b7280')}>Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <p style={{ color: '#6b7280' }}>Loading…</p>
      ) : reminders.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: 13 }}>No payment reminders yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ background: '#f9fafb' }}>
              <th style={th}>#</th>
              <th style={th}>Status</th>
              <th style={th}>Recipient</th>
              <th style={th}>Method</th>
              <th style={th}>Sent At</th>
              <th style={th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {reminders.map(r => (
              <tr key={r.id} style={{ borderTop: '1px solid #f3f4f6' }}>
                <td style={td}>{r.reminderNumber}</td>
                <td style={td}>
                  <span style={{ padding: '2px 8px', borderRadius: 10, fontSize: 11, fontWeight: 700, color: '#fff', background: STATUS_COLORS[r.status] || '#6b7280' }}>
                    {r.status}
                  </span>
                </td>
                <td style={td}>{r.recipientAddress || '—'}</td>
                <td style={td}>{r.deliveryMethod || '—'}</td>
                <td style={td}>{r.sentAt ? new Date(r.sentAt).toLocaleString() : '—'}</td>
                <td style={td}>
                  {r.status === 'PENDING' && (
                    <button
                      onClick={() => handleSend(r.id)}
                      disabled={actionLoading === r.id}
                      style={btn('#059669')}
                    >
                      {actionLoading === r.id ? 'Sending…' : 'Send'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

const th = { padding: '8px 12px', textAlign: 'left', fontWeight: 600, fontSize: 12 };
const td = { padding: '8px 12px' };
function btn(bg) {
  return { padding: '5px 14px', background: bg, color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 12, fontWeight: 500 };
}
