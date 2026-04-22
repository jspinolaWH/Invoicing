const BASE = '/api/v1';

export async function getRemindersForInvoice(invoiceId) {
  const res = await fetch(`${BASE}/invoices/${invoiceId}/payment-reminders`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function createReminder(invoiceId, message) {
  const res = await fetch(`${BASE}/invoices/${invoiceId}/payment-reminders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function sendReminder(reminderId) {
  const res = await fetch(`${BASE}/payment-reminders/${reminderId}/send`, { method: 'POST' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getRemindersForCustomer(customerId) {
  const res = await fetch(`${BASE}/customers/${customerId}/payment-reminders`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
