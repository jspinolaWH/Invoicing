# Step 56 — Authority Invoice Image Retrieval

## References to Original Requirements
- `Docs/structured_breakdown/04-api-layer.md` → Section: Authority Access — GET /authority/invoices/{id}/image
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: "Invoice images (on demand)" — same retrieval logic as step 51
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: AUTHORITY_VIEWER — read-only, only SENT and COMPLETED

---

## Goal
Implement `GET /api/v1/authority/invoices/{id}/image` — the authority-scoped image retrieval endpoint. Uses the same `InvoiceImageService` as step 51 but enforces an additional access rule: returns HTTP 403 if the invoice status is DRAFT, READY, CANCELLED, or ERROR. Only SENT and COMPLETED invoices are accessible to the AUTHORITY_VIEWER role. The FE "View PDF" button in the authority portal calls this endpoint.

---

## Backend

### 56.1 Authority Image Endpoint

**Extension to `AuthorityInvoiceViewController`** (from step 55):

```java
/**
 * Fetch the invoice PDF for an AUTHORITY_VIEWER.
 * Returns 403 if the invoice is not SENT or COMPLETED.
 * Returns 404 if the image is not found in the external system.
 * Returns 502 if the external system is unavailable.
 */
@GetMapping("/invoices/{id}/image")
public ResponseEntity<byte[]> getAuthorityInvoiceImage(@PathVariable Long id) {
    Invoice invoice = invoiceRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Invoice " + id + " not found"));

    // Strict status check — AUTHORITY_VIEWER must never see non-transmitted invoices
    if (invoice.getStatus() != InvoiceStatus.SENT &&
        invoice.getStatus() != InvoiceStatus.COMPLETED) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(null);
        // Do NOT reveal the invoice's actual status in the error body
        // (to avoid information leakage about invoice existence in non-sent states)
    }

    if (invoice.getInvoiceNumber() == null) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    byte[] pdfBytes = imageService.fetchImage(invoice.getInvoiceNumber());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"invoice-" + invoice.getInvoiceNumber() + ".pdf\"")
        .body(pdfBytes);
}
```

---

### 56.2 Difference from Step 51

| Aspect | Step 51 (`/api/v1/invoices/{id}/image`) | Step 56 (`/api/v1/authority/invoices/{id}/image`) |
|--------|------------------------------------------|---------------------------------------------------|
| Roles | INVOICING_USER, FUNCTION_ADMIN, BILLING_MANAGER, AUTHORITY_VIEWER | AUTHORITY_VIEWER only |
| Status restriction | None (any transmitted invoice) | SENT or COMPLETED only → 403 for others |
| Route prefix | `/api/v1/invoices/` | `/api/v1/authority/invoices/` |
| Error on DRAFT | Returns image if one exists | Returns 403 (no information leakage) |

The underlying `InvoiceImageService.fetchImage()` is shared — the difference is only in the access control layer.

---

### 56.3 Controller Endpoint Table

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/authority/invoices/{id}/image` | Fetch invoice PDF (SENT/COMPLETED only) | AUTHORITY_VIEWER |

**Responses:**
- `200 OK` — PDF binary, `Content-Type: application/pdf`
- `403 Forbidden` — invoice is not SENT or COMPLETED (returns empty body, no status revealed)
- `404 Not Found` — image not found in external system (from `InvoiceImageNotFoundException`)
- `502 Bad Gateway` — external system unavailable (from `ExternalSystemUnavailableException`)

---

## Frontend

### 56.4 View PDF Button in Authority Portal

**Extension to `AuthorityInvoiceList.jsx`** (step 55):

The `ViewPdfButton` component in the authority portal passes `useAuthorityEndpoint` prop, which switches the API call URL:

```jsx
const ViewPdfButton = ({ invoiceId, useAuthorityEndpoint = false }) => {
  const endpoint = useAuthorityEndpoint
    ? `/api/v1/authority/invoices/${invoiceId}/image`
    : `/api/v1/invoices/${invoiceId}/image`

  const handleViewPdf = async () => {
    try {
      const response = await axios.get(endpoint, { responseType: 'blob' })
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      window.open(url, '_blank')
    } catch (err) {
      if (err.response?.status === 403) {
        alert('This invoice is not available for viewing.')
      } else if (err.response?.status === 404) {
        alert('Invoice PDF not found in the invoicing system.')
      } else if (err.response?.status === 502) {
        alert('The invoicing system is temporarily unavailable. Please try again later.')
      }
    }
  }

  return <button onClick={handleViewPdf}>View Invoice</button>
}
```

**API calls (additions to `src/api/authority.js`):**
```js
export const getAuthorityInvoiceImage = (invoiceId) =>
  axios.get(`/api/v1/authority/invoices/${invoiceId}/image`, { responseType: 'blob' })
```

---

## Verification Checklist

1. AUTHORITY_VIEWER `GET /api/v1/authority/invoices/{id}/image` for a SENT invoice → 200 OK with PDF binary.
2. AUTHORITY_VIEWER request for a COMPLETED invoice → 200 OK.
3. AUTHORITY_VIEWER request for a DRAFT invoice → 403 Forbidden. Response body is empty (no status disclosed).
4. AUTHORITY_VIEWER request for a CANCELLED invoice → 403 Forbidden.
5. INVOICING_USER `GET /api/v1/authority/invoices/{id}/image` → 403 Forbidden (wrong role for authority endpoint).
6. AUTHORITY_VIEWER `GET /api/v1/invoices/{id}/image` (non-authority endpoint) → 403 Forbidden (AUTHORITY_VIEWER has no access to standard endpoints).
7. External system returns 404 → authority endpoint returns 404 with standard error message.
8. External system returns 503 → authority endpoint returns 502.
9. Open authority portal in FE — "View Invoice" button present per row; clicking opens PDF in new tab for SENT invoices.
10. Authority portal does NOT show a "View Invoice" button for any non-SENT/COMPLETED status (because such invoices never appear in the authority list to begin with, per step 55).

---

## File Checklist

### Backend
- [ ] `authority/AuthorityInvoiceViewController.java` — add `getAuthorityInvoiceImage()` method (extends step 55)

### Frontend
- [ ] `src/pages/invoices/components/ViewPdfButton.jsx` — add `useAuthorityEndpoint` prop (extends step 51)
- [ ] `src/api/authority.js` — add `getAuthorityInvoiceImage()` (extends step 55)
