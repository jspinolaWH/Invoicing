import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchTrigger, reviewTrigger, convertTriggerToTicket } from '../../api/billingThreshold';
import { searchCustomers } from '../../api/customers';
import { getBillingEvents } from '../../api/billingEvents';

const STATUS_COLORS = {
  OPEN: '#dc2626',
  CONVERTED_TO_TICKET: '#7c3aed',
  REVIEWED: '#16a34a',
};

export default function BillingThresholdTriggerDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [trigger, setTrigger] = useState(null);
  const [customer, setCustomer] = useState(null);
  const [billingEvents, setBillingEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [decision, setDecision] = useState('');
  const [reviewing, setReviewing] = useState(false);
  const [converting, setConverting] = useState(false);
  const [showReviewForm, setShowReviewForm] = useState(false);

  useEffect(() => { load(); }, [id]);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const t = await fetchTrigger(id);
      setTrigger(t);
      loadCustomerContext(t.customerNumber, t.triggerYear);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadCustomerContext(customerNumber, triggerYear) {
    try {
      const [customerResults, eventsPage] = await Promise.all([
        searchCustomers(customerNumber).then(r => r.data),
        getBillingEvents({
          customerNumber,
          dateFrom: `${triggerYear}-01-01`,
          dateTo: `${triggerYear + 1}-01-01`,
          size: 20,
        }).then(r => r.data),
      ]);
      setCustomer(customerResults?.[0] ?? null);
      setBillingEvents(eventsPage?.content ?? []);
    } catch {
      // Customer context is supplemental — don't block the page on failure
    }
  }

  async function handleReview(e) {
    e.preventDefault();
    setReviewing(true);
    setError(null);
    try {
      const updated = await reviewTrigger(id, decision);
      setTrigger(updated);
      setShowReviewForm(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setReviewing(false);
    }
  }

  async function handleConvert() {
    if (!window.confirm('Convert this trigger to a ticket for structured case management?')) return;
    setConverting(true);
    setError(null);
    try {
      await convertTriggerToTicket(id);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setConverting(false);
    }
  }

  if (loading) return <div style={{ padding: 24, color: '#6b7280' }}>Loading…</div>;
  if (error) return <div style={{ padding: 24, color: '#dc2626' }}>{error}</div>;
  if (!trigger) return null;

  const exceedAmount = Number(trigger.annualAmount) - Number(trigger.thresholdAmount);
  const exceedPct = ((exceedAmount / Number(trigger.thresholdAmount)) * 100).toFixed(1);

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <button
        onClick={() => navigate('/alerts/triggers')}
        style={{ marginBottom: 16, padding: '5px 14px', background: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}
      >
        ← Back to Alerts
      </button>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>Trigger #{trigger.id}</h2>
        <span style={{
          display: 'inline-block', padding: '3px 10px', borderRadius: 12, fontSize: 12,
          fontWeight: 600, color: '#fff', background: STATUS_COLORS[trigger.status] || '#6b7280',
        }}>
          {trigger.status?.replace(/_/g, ' ')}
        </span>
      </div>

      {error && (
        <div style={{ color: '#dc2626', marginBottom: 16, padding: 12, background: '#fee2e2', borderRadius: 6 }}>
          {error}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 24 }}>
        <InfoCard label="Customer Number" value={trigger.customerNumber} highlight />
        <InfoCard label="Service Responsibility" value={trigger.serviceResponsibility} />
        <InfoCard label="Annual Billed Amount" value={`€ ${Number(trigger.annualAmount).toLocaleString('de-DE', { minimumFractionDigits: 2 })}`} highlight danger />
        <InfoCard label="Configured Threshold" value={`€ ${Number(trigger.thresholdAmount).toLocaleString('de-DE', { minimumFractionDigits: 2 })}`} />
        <InfoCard label="Exceeded By" value={`€ ${exceedAmount.toLocaleString('de-DE', { minimumFractionDigits: 2 })} (${exceedPct}%)`} danger />
        <InfoCard label="Year" value={trigger.triggerYear} />
      </div>

      {(trigger.auditedBy || trigger.decision) && (
        <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, padding: 16, marginBottom: 24 }}>
          <h3 style={{ margin: '0 0 8px', color: '#16a34a', fontSize: 15 }}>Review Record</h3>
          <p style={{ margin: '0 0 4px', fontSize: 13 }}><strong>Decision:</strong> {trigger.decision || '—'}</p>
          <p style={{ margin: '0 0 4px', fontSize: 13 }}><strong>Reviewed by:</strong> {trigger.auditedBy || '—'}</p>
          <p style={{ margin: 0, fontSize: 13 }}><strong>Reviewed at:</strong> {trigger.auditedAt ? new Date(trigger.auditedAt).toLocaleString() : '—'}</p>
        </div>
      )}

      <div style={{ display: 'flex', gap: 10, marginBottom: 24, flexWrap: 'wrap' }}>
        {trigger.status === 'OPEN' && (
          <>
            <button
              onClick={() => setShowReviewForm(f => !f)}
              style={{ padding: '8px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
            >
              Record Review Decision
            </button>
            <button
              onClick={handleConvert}
              disabled={converting}
              style={{ padding: '8px 18px', background: '#7c3aed', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
            >
              {converting ? 'Converting…' : 'Convert to Ticket'}
            </button>
          </>
        )}
        <button
          onClick={() => navigate(`/retroactive/responsibility-change`)}
          style={{ padding: '8px 18px', background: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer', color: '#374151' }}
        >
          Update Service Responsibility
        </button>
      </div>

      {showReviewForm && (
        <form onSubmit={handleReview} style={{ background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 8, padding: 16, marginBottom: 24 }}>
          <h3 style={{ margin: '0 0 12px', fontSize: 15 }}>Record Review Decision</h3>
          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: 4, fontSize: 13 }}>
              Decision / Justification *
            </label>
            <textarea
              value={decision}
              onChange={ev => setDecision(ev.target.value)}
              rows={3}
              required
              placeholder="e.g. Customer reviewed — responsibility updated to market-based. No further action required."
              style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #ccc', fontSize: 13, boxSizing: 'border-box' }}
            />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" disabled={reviewing} style={{ padding: '7px 18px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              {reviewing ? 'Saving…' : 'Save Decision'}
            </button>
            <button type="button" onClick={() => setShowReviewForm(false)} style={{ padding: '7px 14px', background: '#fff', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer' }}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: 16 }}>
        <h3 style={{ fontSize: 14, color: '#6b7280', margin: '0 0 8px' }}>Audit Trail</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <tr style={{ borderBottom: '1px solid #e5e7eb' }}>
              <td style={{ padding: '6px 10px', color: '#6b7280', width: 160 }}>Created at</td>
              <td style={{ padding: '6px 10px' }}>{trigger.createdAt ? new Date(trigger.createdAt).toLocaleString() : '—'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #e5e7eb' }}>
              <td style={{ padding: '6px 10px', color: '#6b7280' }}>Created by</td>
              <td style={{ padding: '6px 10px' }}>{trigger.createdBy || 'system'}</td>
            </tr>
            {trigger.auditedAt && (
              <tr style={{ borderBottom: '1px solid #e5e7eb' }}>
                <td style={{ padding: '6px 10px', color: '#6b7280' }}>Reviewed at</td>
                <td style={{ padding: '6px 10px' }}>{new Date(trigger.auditedAt).toLocaleString()}</td>
              </tr>
            )}
            {trigger.auditedBy && (
              <tr>
                <td style={{ padding: '6px 10px', color: '#6b7280' }}>Reviewed by</td>
                <td style={{ padding: '6px 10px' }}>{trigger.auditedBy}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {customer && (
        <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: 16, marginTop: 16 }}>
          <h3 style={{ fontSize: 14, color: '#6b7280', margin: '0 0 12px' }}>Customer Profile</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <InfoCard label="Name" value={customer.name || customer.companyName || '—'} />
            <InfoCard label="CVR / Tax ID" value={customer.cvr || customer.taxId || '—'} />
            <InfoCard label="Address" value={customer.address || '—'} />
            <InfoCard label="Contact" value={customer.email || customer.phone || '—'} />
          </div>
        </div>
      )}

      {billingEvents.length > 0 && (
        <div style={{ borderTop: '1px solid #e5e7eb', paddingTop: 16, marginTop: 16 }}>
          <h3 style={{ fontSize: 14, color: '#6b7280', margin: '0 0 12px' }}>
            Billing History — {trigger.triggerYear} (latest {billingEvents.length} events)
          </h3>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
            <thead>
              <tr style={{ background: '#f3f4f6' }}>
                {['Date', 'Product', 'Qty', 'Waste Fee', 'Transport Fee', 'Eco Fee', 'Status'].map(h => (
                  <th key={h} style={{ padding: '6px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {billingEvents.map(ev => (
                <tr key={ev.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: '5px 10px', whiteSpace: 'nowrap' }}>{ev.eventDate || '—'}</td>
                  <td style={{ padding: '5px 10px' }}>{ev.productName || ev.productId || '—'}</td>
                  <td style={{ padding: '5px 10px' }}>{ev.quantity}</td>
                  <td style={{ padding: '5px 10px' }}>€ {Number(ev.wasteFeePrice || 0).toFixed(2)}</td>
                  <td style={{ padding: '5px 10px' }}>€ {Number(ev.transportFeePrice || 0).toFixed(2)}</td>
                  <td style={{ padding: '5px 10px' }}>€ {Number(ev.ecoFeePrice || 0).toFixed(2)}</td>
                  <td style={{ padding: '5px 10px' }}>{ev.status || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function InfoCard({ label, value, highlight, danger }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, padding: 16 }}>
      <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 4 }}>{label}</div>
      <div style={{
        fontSize: highlight ? 18 : 15, fontWeight: highlight ? 700 : 500,
        color: danger ? '#dc2626' : '#111827',
      }}>
        {value}
      </div>
    </div>
  );
}
