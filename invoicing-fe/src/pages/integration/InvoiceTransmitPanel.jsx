import { useState } from 'react';
import { transmitInvoice, getTransmissionStatus, getInvoiceImage, getExternalAttachments, recallInvoice } from '../../api/integration';

export default function InvoiceTransmitPanel({ invoiceId, invoiceStatus, externalReference, onStatusChange }) {
  const [transmitResult, setTransmitResult] = useState(null);
  const [statusResult, setStatusResult] = useState(null);
  const [attachments, setAttachments] = useState(null);
  const [loading, setLoading] = useState('');
  const [error, setError] = useState(null);
  const [showRecall, setShowRecall] = useState(false);
  const [recallForm, setRecallForm] = useState({ reason: '', internalComment: '' });
  const [recallResult, setRecallResult] = useState(null);

  async function handleTransmit() {
    setLoading('transmit');
    setError(null);
    try {
      const result = await transmitInvoice(invoiceId);
      setTransmitResult(result);
      if (onStatusChange) onStatusChange('SENT', result.externalReference);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function handleFetchStatus() {
    setLoading('status');
    setError(null);
    try {
      const result = await getTransmissionStatus(invoiceId);
      setStatusResult(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function handleFetchImage() {
    setLoading('image');
    setError(null);
    try {
      const blob = await getInvoiceImage(invoiceId);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function handleFetchAttachments() {
    setLoading('attachments');
    setError(null);
    try {
      const result = await getExternalAttachments(invoiceId);
      setAttachments(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  async function handleRecall(e) {
    e.preventDefault();
    setLoading('recall');
    setError(null);
    try {
      const result = await recallInvoice(invoiceId, recallForm);
      setRecallResult(result);
      setShowRecall(false);
      if (onStatusChange) onStatusChange('CANCELLED');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading('');
    }
  }

  const isSent = invoiceStatus === 'SENT';
  const isReady = invoiceStatus === 'READY';
  const hasExtRef = !!externalReference || transmitResult?.externalReference;

  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 20, marginTop: 20 }}>
      <h3 style={{ marginTop: 0, marginBottom: 16 }}>Transmission &amp; Integration</h3>

      {error && <div style={{ color: '#dc2626', padding: 10, background: '#fee2e2', borderRadius: 6, marginBottom: 12 }}>{error}</div>}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
        {isReady && (
          <button onClick={handleTransmit} disabled={loading === 'transmit'} style={btn('#2563eb')}>
            {loading === 'transmit' ? 'Transmitting\u2026' : 'Transmit to Operator'}
          </button>
        )}
        {(isSent || hasExtRef) && (
          <>
            <button onClick={handleFetchStatus} disabled={loading === 'status'} style={btn('#7c3aed')}>
              {loading === 'status' ? 'Checking\u2026' : 'Check Delivery Status'}
            </button>
            <button onClick={handleFetchImage} disabled={loading === 'image'} style={btn('#0891b2')}>
              {loading === 'image' ? 'Loading\u2026' : 'View Invoice Image (PDF)'}
            </button>
            <button onClick={handleFetchAttachments} disabled={loading === 'attachments'} style={btn('#059669')}>
              {loading === 'attachments' ? 'Loading\u2026' : 'Fetch External Attachments'}
            </button>
          </>
        )}
        {isSent && (
          <button onClick={() => setShowRecall(true)} style={btn('#dc2626')}>
            Recall Invoice
          </button>
        )}
      </div>

      {transmitResult && (
        <div style={{ background: '#f0fdf4', borderRadius: 6, padding: 12, marginBottom: 12 }}>
          <strong>Transmitted</strong> &middot; Ref: <code>{transmitResult.externalReference}</code>
          &nbsp;&middot; Status: {transmitResult.status} &middot; At: {new Date(transmitResult.transmittedAt).toLocaleString()}
        </div>
      )}

      {statusResult && (
        <div style={{ background: '#eff6ff', borderRadius: 6, padding: 12, marginBottom: 12 }}>
          <strong>Delivery Status:</strong> {statusResult.deliveryStatus}
          {statusResult.externalReference && <> &middot; Ref: <code>{statusResult.externalReference}</code></>}
        </div>
      )}

      {attachments !== null && (
        <div style={{ marginBottom: 12 }}>
          {attachments.length === 0
            ? <p style={{ color: '#6b7280' }}>No external attachments found.</p>
            : attachments.map((a, i) => (
                <div key={i} style={{ padding: 8, border: '1px solid #e5e7eb', borderRadius: 4, marginBottom: 6 }}>
                  {a.filename} ({a.mimeType}, {a.sizeBytes} bytes)
                </div>
              ))}
        </div>
      )}

      {recallResult && (
        <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 6, padding: 12, marginBottom: 12 }}>
          <strong>Invoice Recalled</strong> &middot; {recallResult.message}
        </div>
      )}

      {showRecall && (
        <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 8, padding: 16 }}>
          <h4 style={{ marginTop: 0 }}>Recall Invoice</h4>
          <form onSubmit={handleRecall}>
            <div style={{ marginBottom: 10 }}>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Reason *</label>
              <input value={recallForm.reason} onChange={e => setRecallForm(f => ({ ...f, reason: e.target.value }))}
                style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }} required />
            </div>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>Internal Comment</label>
              <input value={recallForm.internalComment} onChange={e => setRecallForm(f => ({ ...f, internalComment: e.target.value }))}
                style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc' }} />
            </div>
            <button type="submit" disabled={loading === 'recall'} style={btn('#dc2626')}>
              {loading === 'recall' ? 'Recalling\u2026' : 'Confirm Recall'}
            </button>
            <button type="button" onClick={() => setShowRecall(false)} style={{ ...btn('#fff'), color: '#374151', border: '1px solid #ccc', marginLeft: 8 }}>
              Cancel
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

function btn(bg) {
  return { padding: '7px 16px', background: bg, color: bg === '#fff' ? '#374151' : '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 13, fontWeight: 500 };
}
