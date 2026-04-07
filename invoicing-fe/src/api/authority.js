const BASE = '/api/v1/authority/invoices';

export async function listAuthorityInvoices({ customerId, dateFrom, dateTo, page = 0, size = 25 } = {}) {
  const params = new URLSearchParams();
  if (customerId) params.set('customerId', customerId);
  if (dateFrom) params.set('dateFrom', dateFrom);
  if (dateTo) params.set('dateTo', dateTo);
  params.set('page', page);
  params.set('size', size);
  const res = await fetch(`${BASE}?${params}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getAuthorityInvoice(id) {
  const res = await fetch(`${BASE}/${id}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getAuthorityInvoiceImage(id) {
  const res = await fetch(`${BASE}/${id}/image`);
  if (!res.ok) throw new Error(await res.text());
  return res.blob();
}
