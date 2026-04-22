export async function fetchVatReport({ from, to } = {}) {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString() ? `?${params}` : '';
  const res = await fetch(`/api/v1/reports/vat${query}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
