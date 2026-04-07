const BASE = '/api/v1/retroactive';

export async function previewPriceAdjustment(data) {
  const res = await fetch(`${BASE}/price-adjustment/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function applyPriceAdjustment(data) {
  const res = await fetch(`${BASE}/price-adjustment/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function previewResponsibilityChange(data) {
  const res = await fetch(`${BASE}/responsibility-change/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function applyResponsibilityChange(data) {
  const res = await fetch(`${BASE}/responsibility-change/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
