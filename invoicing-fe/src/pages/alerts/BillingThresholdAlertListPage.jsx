import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchTriggers, convertTriggerToTicket, fetchTickets, fetchCustomerThresholdStatuses } from '../../api/billingThreshold';

const RESPONSIBILITY_OPTIONS = [
  { value: '', label: 'All Classifications' },
  { value: 'MUNICIPALITY', label: 'Municipality' },
  { value: 'PRIVATE_COLLECTOR', label: 'Private Collector' },
  { value: 'CONTRACT_HOLDER', label: 'Contract Holder' },
  { value: 'SHARED', label: 'Shared' },
  { value: 'OWNER', label: 'Owner' },
];

const STATUS_OPTIONS = [
  { value: '', label: 'All Statuses' },
  { value: 'OPEN', label: 'Open' },
  { value: 'CONVERTED_TO_TICKET', label: 'Converted to Ticket' },
  { value: 'REVIEWED', label: 'Reviewed' },
];

const STATUS_COLORS = {
  OPEN: '#dc2626',
  CONVERTED_TO_TICKET: '#7c3aed',
  REVIEWED: '#16a34a',
};

const TICKET_STATUS_COLORS = {
  OPEN: '#dc2626',
  IN_PROGRESS: '#d97706',
  RESOLVED: '#16a34a',
};

export default function BillingThresholdAlertListPage() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ serviceResponsibility: '', status: '', customerNumber: '' });
  const [customerFilter, setCustomerFilter] = useState('');
  const [triggers, setTriggers] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [converting, setConverting] = useState(null);
  const [tab, setTab] = useState('triggers');

  useEffect(() => { loadTriggers(); }, []);
  useEffect(() => { if (tab === 'tickets') loadTickets(); }, [tab]);
  useEffect(() => { if (tab === 'customers') loadCustomers(); }, [tab]);

  async function loadTriggers() {
    setLoading(true);
    setError(null);
    try {
      setTriggers(await fetchTriggers(filters));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadTickets() {
    setLoading(true);
    setError(null);
    try {
      setTickets(await fetchTickets());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadCustomers(exceededFilter) {
    setLoading(true);
    setError(null);
    try {
      const exceeded = exceededFilter === 'exceeded' ? true : exceededFilter === 'not_exceeded' ? false : undefined;
      setCustomers(await fetchCustomerThresholdStatuses({ exceeded }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleConvert(triggerId) {
    if (!window.confirm('Convert this trigger to a ticket?')) return;
    setConverting(triggerId);
    try {
      await convertTriggerToTicket(triggerId);
      await loadTriggers();
    } catch (err) {
      setError(err.message);
    } finally {
      setConverting(null);
    }
  }

  function handleFilterChange(key, value) {
    setFilters(f => ({ ...f, [key]: value }));
  }

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 20 }}>Billing Threshold Alerts</h2>

      {error && (
        <div style={{ color: '#dc2626', marginBottom: 16, padding: 12, background: '#fee2e2', borderRadius: 6 }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', gap: 0, marginBottom: 24, borderBottom: '2px solid #e5e7eb' }}>
        {[['triggers', 'Triggers'], ['tickets', 'Tickets'], ['customers', 'Customers']].map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            style={{
              padding: '8px 20px', border: 'none', cursor: 'pointer', fontWeight: tab === key ? 700 : 400,
              background: 'none', borderBottom: tab === key ? '2px solid #2563eb' : '2px solid transparent',
              color: tab === key ? '#2563eb' : '#374151', marginBottom: -2,
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'triggers' && (
        <>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Classification</label>
              <select
                value={filters.serviceResponsibility}
                onChange={ev => handleFilterChange('serviceResponsibility', ev.target.value)}
                style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc', fontSize: 13 }}
              >
                {RESPONSIBILITY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Status</label>
              <select
                value={filters.status}
                onChange={ev => handleFilterChange('status', ev.target.value)}
                style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc', fontSize: 13 }}
              >
                {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Customer #</label>
              <input
                type="text"
                value={filters.customerNumber}
                onChange={ev => handleFilterChange('customerNumber', ev.target.value)}
                placeholder="e.g. 123456789"
                style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc', fontSize: 13, width: 140 }}
              />
            </div>
            <button
              onClick={loadTriggers}
              disabled={loading}
              style={{ padding: '7px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}
            >
              {loading ? 'Loading…' : 'Search'}
            </button>
          </div>

          {triggers.length === 0 && !loading ? (
            <p style={{ color: '#6b7280' }}>No triggers found for the selected filters.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#f3f4f6' }}>
                  {['Customer #', 'Classification', 'Annual Amount', 'Threshold', 'Year', 'Status', 'Audited By', ''].map(h => (
                    <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {triggers.map(t => (
                  <tr key={t.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: '7px 10px', fontWeight: 600 }}>{t.customerNumber}</td>
                    <td style={{ padding: '7px 10px' }}>{t.serviceResponsibility}</td>
                    <td style={{ padding: '7px 10px', color: '#dc2626', fontWeight: 600 }}>€ {Number(t.annualAmount).toLocaleString('de-DE', { minimumFractionDigits: 2 })}</td>
                    <td style={{ padding: '7px 10px' }}>€ {Number(t.thresholdAmount).toLocaleString('de-DE', { minimumFractionDigits: 2 })}</td>
                    <td style={{ padding: '7px 10px' }}>{t.triggerYear}</td>
                    <td style={{ padding: '7px 10px' }}>
                      <StatusBadge status={t.status} colors={STATUS_COLORS} />
                    </td>
                    <td style={{ padding: '7px 10px', color: '#6b7280', fontSize: 12 }}>{t.auditedBy || '—'}</td>
                    <td style={{ padding: '7px 10px' }}>
                      <button
                        onClick={() => navigate(`/alerts/triggers/${t.id}`)}
                        style={{ marginRight: 6, padding: '3px 10px', fontSize: 12, background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 4, cursor: 'pointer', color: '#1d4ed8' }}
                      >
                        View
                      </button>
                      {t.status === 'OPEN' && (
                        <button
                          onClick={() => handleConvert(t.id)}
                          disabled={converting === t.id}
                          style={{ padding: '3px 10px', fontSize: 12, background: '#f5f3ff', border: '1px solid #c4b5fd', borderRadius: 4, cursor: 'pointer', color: '#7c3aed' }}
                        >
                          {converting === t.id ? '…' : 'Convert to Ticket'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {tab === 'tickets' && (
        <>
          {loading ? (
            <p style={{ color: '#6b7280' }}>Loading…</p>
          ) : tickets.length === 0 ? (
            <p style={{ color: '#6b7280' }}>No tickets yet. Convert a trigger to create one.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#f3f4f6' }}>
                  {['Ticket ID', 'Customer #', 'Classification', 'Status', 'Assigned To', 'Created At', ''].map(h => (
                    <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {tickets.map(tk => (
                  <tr key={tk.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: '7px 10px', color: '#6b7280' }}>#{tk.id}</td>
                    <td style={{ padding: '7px 10px', fontWeight: 600 }}>{tk.customerNumber}</td>
                    <td style={{ padding: '7px 10px' }}>{tk.serviceResponsibility}</td>
                    <td style={{ padding: '7px 10px' }}>
                      <StatusBadge status={tk.status} colors={TICKET_STATUS_COLORS} />
                    </td>
                    <td style={{ padding: '7px 10px', color: '#6b7280' }}>{tk.assignedTo || '—'}</td>
                    <td style={{ padding: '7px 10px', color: '#6b7280', fontSize: 12, whiteSpace: 'nowrap' }}>
                      {tk.createdAt ? new Date(tk.createdAt).toLocaleString() : '—'}
                    </td>
                    <td style={{ padding: '7px 10px' }}>
                      <button
                        onClick={() => navigate(`/alerts/triggers/${tk.triggerId}`)}
                        style={{ padding: '3px 10px', fontSize: 12, background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 4, cursor: 'pointer', color: '#1d4ed8' }}
                      >
                        View Trigger
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {tab === 'customers' && (
        <>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16, alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Threshold Status</label>
              <select
                value={customerFilter}
                onChange={ev => {
                  setCustomerFilter(ev.target.value);
                  loadCustomers(ev.target.value);
                }}
                style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc', fontSize: 13 }}
              >
                <option value="">All Customers</option>
                <option value="exceeded">Exceeded</option>
                <option value="not_exceeded">Not Exceeded</option>
              </select>
            </div>
            <button
              onClick={() => loadCustomers(customerFilter)}
              disabled={loading}
              style={{ padding: '7px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}
            >
              {loading ? 'Loading…' : 'Refresh'}
            </button>
          </div>

          {loading ? (
            <p style={{ color: '#6b7280' }}>Loading…</p>
          ) : customers.length === 0 ? (
            <p style={{ color: '#6b7280' }}>No customers found for the selected filter.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#f3f4f6' }}>
                  {['Customer #', 'Annual Amount (YTD)', 'Status', ''].map(h => (
                    <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {customers.map(c => (
                  <tr key={c.customerNumber} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: '7px 10px', fontWeight: 600 }}>{c.customerNumber}</td>
                    <td style={{ padding: '7px 10px', color: c.exceeded ? '#dc2626' : '#111827', fontWeight: c.exceeded ? 600 : 400 }}>
                      € {Number(c.annualAmount).toLocaleString('de-DE', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ padding: '7px 10px' }}>
                      <span style={{
                        display: 'inline-block', padding: '2px 8px', borderRadius: 12,
                        fontSize: 11, fontWeight: 600, color: '#fff',
                        background: c.exceeded ? '#dc2626' : '#16a34a',
                      }}>
                        {c.exceeded ? (c.triggerStatus?.replace(/_/g, ' ') || 'EXCEEDED') : 'NOT EXCEEDED'}
                      </span>
                    </td>
                    <td style={{ padding: '7px 10px' }}>
                      {c.triggerId && (
                        <button
                          onClick={() => navigate(`/alerts/triggers/${c.triggerId}`)}
                          style={{ padding: '3px 10px', fontSize: 12, background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 4, cursor: 'pointer', color: '#1d4ed8' }}
                        >
                          View Trigger
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}

function StatusBadge({ status, colors }) {
  return (
    <span style={{
      display: 'inline-block', padding: '2px 8px', borderRadius: 12,
      fontSize: 11, fontWeight: 600, color: '#fff',
      background: colors[status] || '#6b7280',
    }}>
      {status?.replace(/_/g, ' ')}
    </span>
  );
}
