const BASE = '/api/v1';

export async function transmitInvoice(id) {
  const res = await fetch(`${BASE}/invoices/${id}/transmit`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getTransmissionStatus(id) {
  const res = await fetch(`${BASE}/invoices/${id}/transmission-status`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getInvoiceImage(id) {
  const res = await fetch(`${BASE}/invoices/${id}/image`);
  if (!res.ok) throw new Error(await res.text());
  return res.blob();
}

export async function getExternalAttachments(id) {
  const res = await fetch(`${BASE}/invoices/${id}/external-attachments`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function retransmitInvoice(id) {
  const res = await fetch(`${BASE}/invoices/${id}/retransmit`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function recallInvoice(id, data) {
  const res = await fetch(`${BASE}/invoices/${id}/recall`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function syncBillingAddress(data) {
  const res = await fetch(`${BASE}/sync/billing-address`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function triggerOperatorBatch() {
  const res = await fetch(`${BASE}/einvoice-operator/trigger`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
