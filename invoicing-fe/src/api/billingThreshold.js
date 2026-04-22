const BASE = '/api/v1/billing-threshold';

// --- Threshold Config ---

export async function fetchThresholdConfigs() {
  const res = await fetch(`${BASE}/config`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function createThresholdConfig(data) {
  const res = await fetch(`${BASE}/config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function updateThresholdConfig(id, data) {
  const res = await fetch(`${BASE}/config/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function deleteThresholdConfig(id) {
  const res = await fetch(`${BASE}/config/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(await res.text());
}

// --- Triggers ---

export async function fetchTriggers({ serviceResponsibility, status, customerNumber } = {}) {
  const params = new URLSearchParams();
  if (serviceResponsibility) params.set('serviceResponsibility', serviceResponsibility);
  if (status) params.set('status', status);
  if (customerNumber) params.set('customerNumber', customerNumber);
  const query = params.toString() ? `?${params}` : '';
  const res = await fetch(`${BASE}/triggers${query}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function fetchTrigger(id) {
  const res = await fetch(`${BASE}/triggers/${id}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function reviewTrigger(id, decision) {
  const res = await fetch(`${BASE}/triggers/${id}/review`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// --- Tickets ---

export async function fetchTickets() {
  const res = await fetch(`${BASE}/tickets`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function fetchTicket(id) {
  const res = await fetch(`${BASE}/tickets/${id}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function convertTriggerToTicket(triggerId) {
  const res = await fetch(`${BASE}/tickets/convert/${triggerId}`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// --- Customer Threshold Status ---

export async function fetchCustomerThresholdStatuses({ exceeded, year } = {}) {
  const params = new URLSearchParams();
  if (exceeded !== undefined && exceeded !== null) params.set('exceeded', exceeded);
  if (year) params.set('year', year);
  const query = params.toString() ? `?${params}` : '';
  const res = await fetch(`${BASE}/customers${query}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
