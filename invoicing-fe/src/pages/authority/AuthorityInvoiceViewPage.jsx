import { useState, useEffect } from 'react';
import { listAuthorityInvoices, getAuthorityInvoiceImage } from '../../api/authority';

export default function AuthorityInvoiceViewPage() {
  const [filters, setFilters] = useState({ customerId: '', dateFrom: '', dateTo: '' });
  const [applied, setApplied] = useState({});
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [imageLoading, setImageLoading] = useState(false);

  useEffect(() => {
    fetchInvoices();
  }, [page, applied]);

  async function fetchInvoices() {
    setLoading(true);
    setError(null);
    try {
      const result = await listAuthorityInvoices({
        customerId: applied.customerId || undefined,
        dateFrom: applied.dateFrom || undefined,
        dateTo: applied.dateTo || undefined,
        page,
        size: 25,
      });
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    setPage(0);
    setApplied({ ...filters });
  }

  async function handleViewImage(invoiceId) {
    setImageLoading(true);
    try {
      const blob = await getAuthorityInvoiceImage(invoiceId);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch (err) {
      alert('Failed to load invoice image: ' + err.message);
    } finally {
      setImageLoading(false);
    }
  }

  const statusBadge = (status) => {
    const colors = { SENT: '#7c3aed', COMPLETED: '#16a34a', CANCELLED: '#6b7280' };
    return (
      <span style={{ padding: '2px 8px', borderRadius: 12, fontSize: 11, fontWeight: 700, color: '#fff', background: colors[status] || '#9ca3af' }}>
        {status}
      </span>
    );
  };

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
        <h2 style={{ margin: 0 }}>Authority Invoice View</h2>
        <span style={{ padding: '3px 10px', borderRadius: 12, fontSize: 11, fontWeight: 700, background: '#fef3c7', color: '#92400e', border: '1px solid #fcd34d' }}>
          AUTHORITY_VIEWER
        </span>
      </div>
      <p style={{ color: '#6b7280', marginBottom: 20 }}>Read-only access to sent and completed invoices.</p>

      {/* Filter bar */}
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 12, alignItems: 'flex-end', background: '#f9f9f9', padding: 16, borderRadius: 8, marginBottom: 20, flexWrap: 'wrap' }}>
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4, color: '#374151' }}>Customer ID</label>
          <input type="number" value={filters.customerId} onChange={e => setFilters(f => ({ ...f, customerId: e.target.value }))}
            placeholder="All customers" style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc', width: 140 }} />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4, color: '#374151' }}>Invoice Date From</label>
          <input type="date" value={filters.dateFrom} onChange={e => setFilters(f => ({ ...f, dateFrom: e.target.value }))}
            style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc' }} />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: 12, fontWeight: 600, marginBottom: 4, color: '#374151' }}>Invoice Date To</label>
          <input type="date" value={filters.dateTo} onChange={e => setFilters(f => ({ ...f, dateTo: e.target.value }))}
            style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #ccc' }} />
        </div>
        <button type="submit" style={{ padding: '7px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', height: 34 }}>
          Search
        </button>
      </form>

      {error && <div style={{ color: '#dc2626', padding: 12, background: '#fee2e2', borderRadius: 6, marginBottom: 16 }}>{error}</div>}

      {/* Invoice table */}
      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ background: '#f3f4f6' }}>
              {['Invoice #', 'Date', 'Due Date', 'Customer', 'Net', 'Gross', 'VAT', 'Type', 'Status', 'Actions'].map(h => (
                <th key={h} style={{ padding: '10px 12px', textAlign: 'left', fontWeight: 600, borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={10} style={{ padding: 24, textAlign: 'center', color: '#6b7280' }}>Loading...</td></tr>
            ) : data?.content?.length === 0 ? (
              <tr><td colSpan={10} style={{ padding: 24, textAlign: 'center', color: '#6b7280' }}>No invoices found.</td></tr>
            ) : data?.content?.map(inv => (
              <tr key={inv.id} style={{ borderBottom: '1px solid #f3f4f6', cursor: 'pointer', background: selected?.id === inv.id ? '#eff6ff' : 'transparent' }}
                onClick={() => setSelected(selected?.id === inv.id ? null : inv)}>
                <td style={{ padding: '9px 12px', fontWeight: 600 }}>{inv.invoiceNumber}</td>
                <td style={{ padding: '9px 12px' }}>{inv.invoiceDate}</td>
                <td style={{ padding: '9px 12px' }}>{inv.dueDate}</td>
                <td style={{ padding: '9px 12px' }}>{inv.customerName}</td>
                <td style={{ padding: '9px 12px' }}>€ {Number(inv.netAmount || 0).toFixed(2)}</td>
                <td style={{ padding: '9px 12px', fontWeight: 600 }}>€ {Number(inv.grossAmount || 0).toFixed(2)}</td>
                <td style={{ padding: '9px 12px' }}>€ {Number(inv.vatAmount || 0).toFixed(2)}</td>
                <td style={{ padding: '9px 12px' }}>{inv.invoiceType}</td>
                <td style={{ padding: '9px 12px' }}>{statusBadge(inv.status)}</td>
                <td style={{ padding: '9px 12px' }} onClick={e => e.stopPropagation()}>
                  <button onClick={() => handleViewImage(inv.id)} disabled={imageLoading}
                    style={{ padding: '4px 10px', background: '#0891b2', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 12 }}>
                    {imageLoading ? '...' : 'View PDF'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            style={{ padding: '6px 14px', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer', background: '#fff' }}>&larr; Prev</button>
          <span style={{ padding: '6px 14px', color: '#374151' }}>Page {page + 1} of {data.totalPages}</span>
          <button onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))} disabled={page >= data.totalPages - 1}
            style={{ padding: '6px 14px', border: '1px solid #d1d5db', borderRadius: 4, cursor: 'pointer', background: '#fff' }}>Next &rarr;</button>
        </div>
      )}

      {/* Expanded line items panel */}
      {selected && (
        <div style={{ marginTop: 20, background: '#f8fafc', border: '1px solid #e5e7eb', borderRadius: 8, padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>Invoice {selected.invoiceNumber} &mdash; Line Items</h3>
            <button onClick={() => setSelected(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6b7280', fontSize: 18 }}>&#x2715;</button>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#f3f4f6' }}>
                {['Description', 'Qty', 'Unit Price', 'VAT %', 'Net', 'Gross', 'Classification'].map(h => (
                  <th key={h} style={{ padding: '8px 10px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {selected.lineItems?.map(li => (
                <tr key={li.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '7px 10px' }}>{li.description}</td>
                  <td style={{ padding: '7px 10px' }}>{li.quantity}</td>
                  <td style={{ padding: '7px 10px' }}>€ {Number(li.unitPrice || 0).toFixed(2)}</td>
                  <td style={{ padding: '7px 10px' }}>{li.vatRate}%</td>
                  <td style={{ padding: '7px 10px' }}>€ {Number(li.netAmount || 0).toFixed(2)}</td>
                  <td style={{ padding: '7px 10px', fontWeight: 600 }}>€ {Number(li.grossAmount || 0).toFixed(2)}</td>
                  <td style={{ padding: '7px 10px' }}>
                    {li.legalClassification && (
                      <span style={{ padding: '2px 8px', borderRadius: 12, fontSize: 11, fontWeight: 600, background: li.legalClassification === 'PUBLIC_LAW' ? '#dbeafe' : '#f3f4f6', color: li.legalClassification === 'PUBLIC_LAW' ? '#1d4ed8' : '#374151' }}>
                        {li.legalClassification}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
