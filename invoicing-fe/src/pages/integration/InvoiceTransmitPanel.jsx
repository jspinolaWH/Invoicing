import { useState, useEffect } from 'react';
import { transmitInvoice, retransmitInvoice, getTransmissionStatus, getInvoiceImage, getExternalAttachments, recallInvoice } from '../../api/integration';

export default function InvoiceTransmitPanel({ invoiceId, invoiceStatus, externalReference, allowExternalRecall, onStatusChange }) {
  const [transmitResult, setTransmitResult] = useState(null);
  const [statusResult, setStatusResult] = useState(null);
  const [attachments, setAttachments] = useState(null);
  const [loading, setLoading] = useState('');
  const [error, setError] = useState(null);
  const [showRecall, setShowRecall] = useState(false);
  const [recallForm, setRecallForm] = useState({ reason: '', internalComment: '' });
  const [recallResult, setRecallResult] = useState(null);
  const [pdfUrl, setPdfUrl] = useState(null);

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
    if (pdfUrl) { URL.revokeObjectURL(pdfUrl); setPdfUrl(null); }
    try {
      const blob = await getInvoiceImage(invoiceId);
      setPdfUrl(URL.createObjectURL(blob));
    } catch (err) {
      setError('The invoice image could not be retrieved. ' + err.message + ' Contact your administrator if the problem persists.');
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

  async function handleRetransmit() {
    setLoading('retransmit');
    setError(null);
    try {
      const result = await retransmitInvoice(invoiceId);
      setTransmitResult(result);
      if (onStatusChange) onStatusChange('SENT', result.externalReference);
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

  useEffect(() => {
    if (invoiceStatus === 'SENT' && externalReference) {
      handleFetchAttachments();
    }
  }, [invoiceStatus, externalReference]);

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
          <>
            <button onClick={handleRetransmit} disabled={loading === 'retransmit'} style={btn('#d97706')}>
              {loading === 'retransmit' ? 'Retransmitting…' : 'Retransmit (Updated Address)'}
            </button>
            {allowExternalRecall && (
              <button onClick={() => setShowRecall(true)} style={btn('#dc2626')}>
                Recall Invoice
              </button>
            )}
          </>
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
            : attachments.map((a, i) => {
                const href = a.contentBase64 ? `data:${a.mimeType};base64,${a.contentBase64}` : null;
                const isImage = a.mimeType && a.mimeType.startsWith('image/');
                const isPdf = a.mimeType === 'application/pdf';
                return (
                  <div key={i} style={{ border: '1px solid #e5e7eb', borderRadius: 6, marginBottom: 10, overflow: 'hidden' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: '#f9fafb', borderBottom: href ? '1px solid #e5e7eb' : 'none' }}>
                      <span style={{ fontWeight: 600, fontSize: 13 }}>{a.filename}</span>
                      <span style={{ fontSize: 12, color: '#6b7280' }}>{a.mimeType} &middot; {a.sizeBytes} bytes</span>
                      {a.attachmentIdentifier && <code style={{ fontSize: 11, color: '#6b7280' }}>{a.attachmentIdentifier}</code>}
                      {a.description && <span style={{ fontSize: 12, color: '#374151' }}>{a.description}</span>}
                      {href && (
                        <a href={href} download={a.filename}
                          style={{ marginLeft: 'auto', padding: '4px 10px', background: '#2563eb', color: '#fff', borderRadius: 4, fontSize: 12, textDecoration: 'none', fontWeight: 500 }}>
                          Download
                        </a>
                      )}
                    </div>
                    {href && isPdf && (
                      <iframe src={href} title={a.filename} style={{ width: '100%', height: 400, border: 'none', display: 'block' }} />
                    )}
                    {href && isImage && (
                      <img src={href} alt={a.filename} style={{ maxWidth: '100%', display: 'block' }} />
                    )}
                  </div>
                );
              })}
        </div>
      )}

      {pdfUrl && (
        <div style={{ marginBottom: 12, border: '1px solid #e5e7eb', borderRadius: 6, overflow: 'hidden' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 12px', background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
            <span style={{ fontSize: 13, fontWeight: 600 }}>Invoice PDF</span>
            <button onClick={() => { URL.revokeObjectURL(pdfUrl); setPdfUrl(null); }} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 18, color: '#6b7280', lineHeight: 1 }}>&#x2715;</button>
          </div>
          <iframe src={pdfUrl} style={{ width: '100%', height: 500, border: 'none', display: 'block' }} title="Invoice PDF" />
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
