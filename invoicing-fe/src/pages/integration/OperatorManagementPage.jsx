import { useState } from 'react';
import { triggerOperatorBatch } from '../../api/integration';

export default function OperatorManagementPage() {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirming, setConfirming] = useState(false);

  async function handleTrigger() {
    setLoading(true);
    setError(null);
    setConfirming(false);
    try {
      const data = await triggerOperatorBatch();
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ maxWidth: 700, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 6 }}>E-Invoice Operator Management</h2>
      <p style={{ color: '#6b7280', marginBottom: 24 }}>
        The daily batch runs automatically at 02:00. Use the button below to trigger it manually.
        <br />
        <strong>START</strong> registers new e-invoice customers with the operator.
        <strong> TERMINATE</strong> deregisters customers whose delivery method changed.
      </p>

      <div style={{ background: '#f9f9f9', padding: 20, borderRadius: 8, marginBottom: 24 }}>
        <h3 style={{ marginTop: 0 }}>Daily Batch</h3>
        {!confirming ? (
          <button onClick={() => setConfirming(true)} style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
            Trigger Batch Now
          </button>
        ) : (
          <div style={{ background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 6, padding: 16 }}>
            <p style={{ margin: '0 0 12px' }}>This will process all pending START and TERMINATE registrations with the e-invoice operator. Continue?</p>
            <button onClick={handleTrigger} disabled={loading} style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', marginRight: 8 }}>
              {loading ? 'Running\u2026' : 'Confirm Run'}
            </button>
            <button onClick={() => setConfirming(false)} style={{ padding: '8px 20px', background: '#fff', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}>
              Cancel
            </button>
          </div>
        )}
      </div>

      {error && <div style={{ color: '#dc2626', padding: 12, background: '#fee2e2', borderRadius: 6, marginBottom: 16 }}>{error}</div>}

      {result && (
        <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, padding: 20 }}>
          <h3 style={{ color: '#16a34a', marginTop: 0 }}>Batch Complete</h3>
          <p>{result.message}</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 12 }}>
            {[['Started', result.startedCount], ['Terminated', result.terminatedCount], ['Failed', result.failedCount]].map(([label, val]) => (
              <div key={label} style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 6, padding: 12, textAlign: 'center' }}>
                <div style={{ fontSize: 12, color: '#6b7280' }}>{label}</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: label === 'Failed' && val > 0 ? '#dc2626' : '#111827' }}>{val}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
