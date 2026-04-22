import { useState } from 'react';
import { previewPriceAdjustment, applyPriceAdjustment } from '../../api/retroactive';

export default function PriceAdjustmentPage() {
  const [selectionMode, setSelectionMode] = useState('dateRange'); // 'dateRange' | 'eventIds'
  const [adjustmentType, setAdjustmentType] = useState('FIXED'); // 'FIXED' | 'PERCENTAGE'
  const [form, setForm] = useState({
    customerNumber: '',
    eventDateFrom: '',
    eventDateTo: '',
    productId: '',
    serviceResponsibility: '',
    eventIdsInput: '',
    adjustmentValue: '',
    newWasteFeePrice: '',
    newTransportFeePrice: '',
    newEcoFeePrice: '',
    performedBy: '',
    reason: '',
    internalComment: '',
  });
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirming, setConfirming] = useState(false);

  function parseEventIds(raw) {
    if (!raw || !raw.trim()) return null;
    return raw.split(',').map(s => s.trim()).filter(Boolean).map(Number).filter(n => !isNaN(n));
  }

  function buildPayload() {
    const base = {
      customerNumber: form.customerNumber,
      performedBy: form.performedBy || null,
      reason: form.reason,
      internalComment: form.internalComment,
      adjustmentType,
    };

    if (selectionMode === 'eventIds') {
      base.eventIds = parseEventIds(form.eventIdsInput);
    } else {
      base.eventDateFrom = form.eventDateFrom || null;
      base.eventDateTo = form.eventDateTo || null;
      base.productId = form.productId ? Number(form.productId) : null;
      base.serviceResponsibility = form.serviceResponsibility || null;
    }

    if (adjustmentType === 'PERCENTAGE') {
      base.adjustmentValue = form.adjustmentValue !== '' ? Number(form.adjustmentValue) : null;
    } else {
      base.newWasteFeePrice = form.newWasteFeePrice !== '' ? Number(form.newWasteFeePrice) : null;
      base.newTransportFeePrice = form.newTransportFeePrice !== '' ? Number(form.newTransportFeePrice) : null;
      base.newEcoFeePrice = form.newEcoFeePrice !== '' ? Number(form.newEcoFeePrice) : null;
    }

    return base;
  }

  async function handlePreview(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setPreview(null);
    setResult(null);
    try {
      const data = await previewPriceAdjustment(buildPayload());
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
      const data = await applyPriceAdjustment(buildPayload());
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

  const toggleBtn = (active, label, onClick) => (
    <button
      type="button"
      onClick={onClick}
      style={{
        padding: '6px 16px', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer',
        background: active ? '#2563eb' : '#fff', color: active ? '#fff' : '#374151', fontWeight: active ? 700 : 400,
      }}
    >
      {label}
    </button>
  );

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 20 }}>Retroactive Price Adjustment</h2>

      <form onSubmit={handlePreview} style={{ background: '#f9f9f9', padding: 20, borderRadius: 8, marginBottom: 24 }}>

        <div style={{ marginBottom: 16 }}>
          <span style={{ fontWeight: 600, marginRight: 12 }}>Selection mode:</span>
          {toggleBtn(selectionMode === 'dateRange', 'Date Range', () => setSelectionMode('dateRange'))}
          {' '}
          {toggleBtn(selectionMode === 'eventIds', 'Specific Event IDs', () => setSelectionMode('eventIds'))}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>{field('Customer Number *', 'customerNumber', 'text', 'e.g. 123456789')}</div>
          <div>{field('Performed By', 'performedBy', 'text', 'e.g. john.doe')}</div>

          {selectionMode === 'dateRange' ? (
            <>
              <div>{field('Event Date From *', 'eventDateFrom', 'date')}</div>
              <div>{field('Event Date To *', 'eventDateTo', 'date')}</div>
              <div>{field('Product ID (optional)', 'productId', 'number')}</div>
              <div>{field('Service Responsibility (optional)', 'serviceResponsibility', 'text', 'e.g. MUNICIPAL')}</div>
            </>
          ) : (
            <div style={{ gridColumn: '1 / -1' }}>
              {field('Event IDs (comma-separated) *', 'eventIdsInput', 'text', 'e.g. 101, 102, 103')}
            </div>
          )}
        </div>

        <div style={{ marginBottom: 16 }}>
          <span style={{ fontWeight: 600, marginRight: 12 }}>Adjustment type:</span>
          {toggleBtn(adjustmentType === 'FIXED', 'Fixed Amount', () => setAdjustmentType('FIXED'))}
          {' '}
          {toggleBtn(adjustmentType === 'PERCENTAGE', 'Percentage', () => setAdjustmentType('PERCENTAGE'))}
        </div>

        {adjustmentType === 'PERCENTAGE' ? (
          <div style={{ maxWidth: 300, marginBottom: 12 }}>
            {field('Percentage Adjustment (%)', 'adjustmentValue', 'number', 'e.g. 10 for +10%, -5 for -5%')}
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginBottom: 12 }}>
            <div>{field('New Waste Fee Price', 'newWasteFeePrice', 'number', 'Leave blank to keep current')}</div>
            <div>{field('New Transport Fee Price', 'newTransportFeePrice', 'number', 'Leave blank to keep current')}</div>
            <div>{field('New Eco Fee Price', 'newEcoFeePrice', 'number', 'Leave blank to keep current')}</div>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>{field('Reason', 'reason', 'text', 'e.g. Contract renegotiation')}</div>
          <div />
        </div>
        {field('Internal Comment *', 'internalComment', 'text', 'Required for apply')}

        <button type="submit" disabled={loading} style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          {loading ? 'Loading…' : 'Preview Changes'}
        </button>
      </form>

      {error && <div style={{ color: '#dc2626', marginBottom: 16, padding: 12, background: '#fee2e2', borderRadius: 6 }}>{error}</div>}

      {result && (
        <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, padding: 20, marginBottom: 24 }}>
          <h3 style={{ color: '#16a34a', marginTop: 0 }}>Adjustment Applied</h3>
          <p>{result.message}</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
            <Stat label="Updated in-place" value={result.updatedInProgressCount} />
            <Stat label="Correction copies" value={result.correctionCopiesCreated} />
            <Stat label="Total delta" value={`€ ${Number(result.totalDelta || 0).toFixed(2)}`} />
          </div>
          {result.createdEventIds?.length > 0 && (
            <p style={{ marginTop: 12, color: '#166534' }}>Created event IDs: {result.createdEventIds.join(', ')}</p>
          )}
        </div>
      )}

      {preview && (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
            <Stat label="Total events" value={preview.totalEventCount} />
            <Stat label="In-progress" value={preview.inProgressCount} />
            <Stat label="Already billed" value={preview.billedCount} />
            <Stat label="Net delta" value={`€ ${Number(preview.totalDelta || 0).toFixed(2)}`} color={Number(preview.totalDelta) >= 0 ? '#16a34a' : '#dc2626'} />
          </div>

          {preview.billedCount > 0 && (
            <div style={{ background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 6, padding: 12, marginBottom: 16 }}>
              <strong>Note:</strong> {preview.billedCount} already-billed event(s) will be excluded and correction copies created for re-invoicing.
            </div>
          )}

          <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 20, fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#f3f4f6' }}>
                {['Event ID', 'Date', 'Product', 'Qty', 'Status', 'Current Net', 'Projected Net', 'Delta'].map(h => (
                  <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {preview.events.map(ev => (
                <tr key={ev.eventId} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: '7px 10px' }}>{ev.eventId}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.eventDate}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.productCode || '—'}</td>
                  <td style={{ padding: '7px 10px' }}>{ev.quantity}</td>
                  <td style={{ padding: '7px 10px' }}><StatusBadge status={ev.status} /></td>
                  <td style={{ padding: '7px 10px' }}>€ {Number(ev.currentNetAmount || 0).toFixed(2)}</td>
                  <td style={{ padding: '7px 10px' }}>€ {Number(ev.projectedNetAmount || 0).toFixed(2)}</td>
                  <td style={{ padding: '7px 10px', color: Number(ev.delta) >= 0 ? '#16a34a' : '#dc2626', fontWeight: 600 }}>
                    {Number(ev.delta) >= 0 ? '+' : ''}€ {Number(ev.delta || 0).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {confirming ? (
            <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 6, padding: 16 }}>
              <p style={{ margin: '0 0 12px' }}><strong>Are you sure?</strong> This will modify {preview.totalEventCount} billing event(s). This action cannot be undone.</p>
              <button onClick={handleApply} disabled={loading} style={{ padding: '8px 20px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', marginRight: 8 }}>
                {loading ? 'Applying…' : 'Confirm Apply'}
              </button>
              <button onClick={() => setConfirming(false)} style={{ padding: '8px 20px', background: '#fff', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}>
                Cancel
              </button>
            </div>
          ) : (
            <button onClick={() => setConfirming(true)} style={{ padding: '8px 20px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              Apply Adjustment
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, color }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 6, padding: 12 }}>
      <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, color: color || '#111827' }}>{value}</div>
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
