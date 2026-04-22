import { useState } from 'react';
import { syncBillingAddress } from '../../api/integration';

export default function BillingAddressSyncPage() {
  const [form, setForm] = useState({
    customerNumber: '',
    streetAddress: '',
    postalCode: '',
    city: '',
    countryCode: '',
    streetAddressAlt: '',
    cityAlt: '',
    countryCodeAlt: '',
    emailAddress: '',
    eInvoicingAddress: '',
  });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const data = await syncBillingAddress(form);
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const field = (label, key, required = false) => (
    <div style={{ marginBottom: 12 }}>
      <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>
        {label}{required && ' *'}
      </label>
      <input
        type="text"
        value={form[key]}
        onChange={ev => setForm(f => ({ ...f, [key]: ev.target.value }))}
        style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }}
      />
    </div>
  );

  const statusColors = { UPDATED: '#16a34a', SKIPPED_LOCKED: '#d97706', NOT_FOUND: '#dc2626' };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <h2 style={{ marginBottom: 6 }}>Billing Address Sync</h2>
      <p style={{ color: '#6b7280', marginBottom: 20 }}>Push a billing address update from WasteHero to this customer's record.</p>

      <form onSubmit={handleSubmit} style={{ background: '#f9f9f9', padding: 20, borderRadius: 8, marginBottom: 24 }}>
        {field('Customer Number', 'customerNumber', true)}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div>{field('Street Address', 'streetAddress')}</div>
          <div>{field('Postal Code', 'postalCode')}</div>
          <div>{field('City', 'city')}</div>
          <div>{field('Country Code (2-letter)', 'countryCode')}</div>
          <div>{field('Alt Street Address', 'streetAddressAlt')}</div>
          <div>{field('Alt City', 'cityAlt')}</div>
          <div>{field('Alt Country Code', 'countryCodeAlt')}</div>
          <div>{field('Email Address', 'emailAddress')}</div>
          <div>{field('E-Invoicing Address', 'eInvoicingAddress')}</div>
        </div>
        <button type="submit" disabled={loading} style={{ marginTop: 8, padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          {loading ? 'Syncing\u2026' : 'Sync Address'}
        </button>
      </form>

      {error && <div style={{ color: '#dc2626', padding: 12, background: '#fee2e2', borderRadius: 6 }}>{error}</div>}

      {result && (
        <div style={{ padding: 16, borderRadius: 8, border: '1px solid #e5e7eb', background: '#fff' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
            <span style={{ padding: '3px 10px', borderRadius: 12, fontSize: 12, fontWeight: 700, color: '#fff', background: statusColors[result.status] || '#6b7280' }}>
              {result.status}
            </span>
            <span style={{ fontWeight: 600 }}>Customer #{result.customerId || '\u2014'} ({result.customerNumber})</span>
          </div>
          <p style={{ margin: 0, color: '#374151' }}>{result.message}</p>
        </div>
      )}
    </div>
  );
}
