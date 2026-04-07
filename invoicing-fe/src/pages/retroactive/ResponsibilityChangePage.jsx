import { useState } from 'react';
import { previewResponsibilityChange, applyResponsibilityChange } from '../../api/retroactive';

export default function ResponsibilityChangePage() {
  const [form, setForm] = useState({
    fromCustomerNumber: '',
    toCustomerNumber: '',
    eventDateFrom: '',
    eventDateTo: '',
    productId: '',
    specificEventIds: '',
    reason: '',
    internalComment: '',
  });
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirming, setConfirming] = useState(false);

  function buildPayload() {
    const specificIds = form.specificEventIds.trim()
      ? form.specificEventIds.split(',').map(s => Number(s.trim())).filter(Boolean)
      : null;
    return {
      fromCustomerNumber: form.fromCustomerNumber,
      toCustomerNumber: form.toCustomerNumber,
      eventDateFrom: form.eventDateFrom || null,
      eventDateTo: form.eventDateTo || null,
      productId: form.productId ? Number(form.productId) : null,
      specificEventIds: specificIds,
      reason: form.reason,
      internalComment: form.internalComment,
    };
  }

  async function handlePreview(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setPreview(null);
    setResult(null);
    try {
      const data = await previewResponsibilityChange(buildPayload());
      setPreview(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleApply() {
    setLoading(true);
    setError(null);
    setConfirming(false);
    try {
      const data = await applyResponsibilityChange(buildPayload());
      setResult(data);
      setPreview(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const field = (label, key, type = 'text', placeholder = '') => (
    <div style={{ marginBottom: 12 }}>
      <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>{label}</label>
      <input
        type={type}
        value={form[key]}
        placeholder={placeholder}
        onChange={ev => setForm(f => ({ ...f, [key]: ev.target.value }))}
        style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }}
      />
    </div>
  );

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 20 }}>Service Responsibility Change</h2>

      <form onSubmit={handlePreview} style={{ background: '#f9f9f9', padding: 20, borderRadius: 8, marginBottom: 24 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>{field('From Customer Number *', 'fromCustomerNumber', 'text', 'Current responsible customer')}</div>
          <div>{field('To Customer Number *', 'toCustomerNumber', 'text', 'New responsible customer')}</div>
          <div>{field('Event Date From', 'eventDateFrom', 'date')}</div>
          <div>{field('Event Date To', 'eventDateTo', 'date')}</div>
          <div>{field('Product ID (optional)', 'productId', 'number')}</div>
          <div>{field('Specific Event IDs (optional)', 'specificEventIds', 'text', 'Comma-separated IDs, overrides date range')}</div>
          <div>{field('Reason', 'reason', 'text', 'e.g. Ownership transfer')}</div>
          <div>{field('Internal Comment *', 'internalComment', 'text', 'Required for apply')}</div>
        </div>

        <button type="submit" disabled={loading} style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          {loading ? 'Loading\u2026' : 'Preview Changes'}
        </button>
      </form>

      {error && <div style={{ color: '#dc2626', marginBottom: 16, padding: 12, background: '#fee2e2', borderRadius: 6 }}>{error}</div>}

      {result && (
        <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, padding: 20, marginBottom: 24 }}>
          <h3 style={{ color: '#16a34a', marginTop: 0 }}>Responsibility Change Applied</h3>
          <p>{result.message}</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
            <Stat label="Moved in-place" value={result.movedInProgressCount} />
            <Stat label="Correction copies" value={result.correctionCopiesCreated} />
            <Stat label="Originals excluded" value={result.excludedCount} />
          </div>
          {result.createdEventIds?.length > 0 && (
            <p style={{ marginTop: 12, color: '#166534' }}>Created event IDs: {result.createdEventIds.join(', ')}</p>
          )}
        </div>
      )}

      {preview && (
        <div>
          <div style={{ background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 6, padding: 12, marginBottom: 16 }}>
            <strong>{preview.fromCustomerNumber}</strong> &rarr; <strong>{preview.toCustomerNumber}</strong>
            &nbsp;&middot;&nbsp; {preview.totalEventCount} event(s) affected
            &nbsp;&middot;&nbsp; Total transfer amount: <strong>&euro; {Number(preview.totalTransferAmount || 0).toFixed(2)}</strong>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 20 }}>
            <Stat label="In-progress (will move)" value={preview.inProgressCount} />
            <Stat label="Already billed (need copy)" value={preview.billedCount} />
            <Stat label="Total events" value={preview.totalEventCount} />
          </div>

          {preview.billedCount > 0 && (
            <div style={{ background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 6, padding: 12, marginBottom: 16 }}>
              <strong>Note:</strong> {preview.billedCount} already-billed event(s) will be excluded from the original customer and correction copies will be created for the new customer for re-invoicing.
            </div>
          )}

          <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 20, fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#f3f4f6' }}>
                {['Event ID', 'Date', 'Product', 'Quantity', 'Status', 'Fee Amount'].map(h => (
                  <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {preview.events.map(ev => (
                <tr key={ev.eventId} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: '7px 10px' }}>{ev.eventId}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.eventDate}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.productCode || '\u2014'}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.quantity}</td>
                  <td style={{ padding: '7px 10px' }}><StatusBadge status={ev.status} /></td>
                  <td style={{ padding: '7px 10px' }}>&euro; {Number(ev.totalFeeAmount || 0).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {confirming ? (
            <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 6, padding: 16 }}>
              <p style={{ margin: '0 0 12px' }}><strong>Are you sure?</strong> This will transfer {preview.totalEventCount} event(s) from <strong>{preview.fromCustomerNumber}</strong> to <strong>{preview.toCustomerNumber}</strong>.</p>
              <button onClick={handleApply} disabled={loading} style={{ padding: '8px 20px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', marginRight: 8 }}>
                {loading ? 'Applying\u2026' : 'Confirm Apply'}
              </button>
              <button onClick={() => setConfirming(false)} style={{ padding: '8px 20px', background: '#fff', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}>
                Cancel
              </button>
            </div>
          ) : (
            <button onClick={() => setConfirming(true)} style={{ padding: '8px 20px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              Apply Change
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 6, padding: 12 }}>
      <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700 }}>{value}</div>
    </div>
  );
}

function StatusBadge({ status }) {
  const colors = {
    IN_PROGRESS: '#2563eb',
    SENT: '#7c3aed',
    COMPLETED: '#16a34a',
    ERROR: '#dc2626',
  };
  return (
    <span style={{
      display: 'inline-block', padding: '2px 8px', borderRadius: 12,
      fontSize: 11, fontWeight: 600, color: '#fff',
      background: colors[status] || '#6b7280'
    }}>
      {status}
    </span>
  );
}
