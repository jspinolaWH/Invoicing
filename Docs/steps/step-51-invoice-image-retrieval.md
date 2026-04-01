# Step 51 — Invoice Image Retrieval

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: "Invoice images (on demand)" — not cached locally; fetched from external system
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices — GET /invoices/{id}/image, GET /invoices/{id}/attachments/{attachmentId}
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: AUTHORITY_VIEWER can also access

---

## Goal
Implement on-demand invoice image retrieval from the external invoicing system. `GET /api/v1/invoices/{id}/image` fetches the PDF binary from the external system using the invoice number and streams it back to the caller. WasteHero does not cache the image. On retrieval failure: return HTTP 502/503 (not 500) with a clear message. AUTHORITY_VIEWER role can access sent/completed invoices via this endpoint.

---

## Backend

### 51.1 InvoiceImageService

**File:** `invoicing/src/main/java/com/example/invoicing/integration/InvoiceImageService.java`

> **Requirement source:** `05-integration-layer.md` — "WasteHero does not store invoice images locally. When a user wants to view an invoice, WasteHero calls the external system's image retrieval API with the invoice number."

```java
@Service
public class InvoiceImageService {

    /**
     * Fetch the invoice PDF from the external invoicing system on demand.
     * @throws InvoiceImageNotFoundException if the external system returns 404
     * @throws ExternalSystemUnavailableException if the external system is unreachable (502/503)
     */
    public byte[] fetchImage(String invoiceNumber) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                config.getImageUrl() + "/" + invoiceNumber,
                HttpMethod.GET,
                buildAuthHeaders(),
                byte[].class);

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvoiceImageNotFoundException(
                    "Invoice image not found in external system for invoice " + invoiceNumber);
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalSystemUnavailableException(
                    "External invoicing system returned " + response.getStatusCode());
            }
            return response.getBody();

        } catch (RestClientException ex) {
            throw new ExternalSystemUnavailableException(
                "Could not connect to external invoicing system: " + ex.getMessage());
        }
    }

    /**
     * Fetch a specific attachment binary from the external system.
     */
    public byte[] fetchAttachment(String invoiceNumber, String attachmentIdentifier) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                config.getAttachmentUrl() + "/" + invoiceNumber + "/" + attachmentIdentifier,
                HttpMethod.GET,
                buildAuthHeaders(),
                byte[].class);

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvoiceImageNotFoundException(
                    "Attachment " + attachmentIdentifier + " not found for invoice " + invoiceNumber);
            }
            return response.getBody();

        } catch (RestClientException ex) {
            throw new ExternalSystemUnavailableException(
                "Could not retrieve attachment from external system: " + ex.getMessage());
        }
    }
}
```

---

### 51.2 Exception Types and HTTP Mappings

```java
public class InvoiceImageNotFoundException extends RuntimeException { }
    // → HTTP 404 Not Found

public class ExternalSystemUnavailableException extends RuntimeException { }
    // → HTTP 502 Bad Gateway (the external system, not WasteHero, is the problem)
```

In `GlobalExceptionHandler`:
```java
@ExceptionHandler(InvoiceImageNotFoundException.class)
public ResponseEntity<ErrorResponse> handleImageNotFound(InvoiceImageNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("IMAGE_NOT_FOUND", ex.getMessage()));
}

@ExceptionHandler(ExternalSystemUnavailableException.class)
public ResponseEntity<ErrorResponse> handleExternalUnavailable(ExternalSystemUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("EXTERNAL_SYSTEM_UNAVAILABLE",
            "The external invoicing system is temporarily unavailable. Please try again later."));
}
```

**Why 502 not 503:** 502 Bad Gateway = WasteHero received an invalid response from the upstream external system. 503 Service Unavailable = WasteHero itself is unavailable. 502 is the correct HTTP status here.

---

### 51.3 InvoiceImageController

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceImageController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/invoices/{id}/image` | Fetch invoice PDF from external system | INVOICING_USER, AUTHORITY_VIEWER |
| GET | `/api/v1/invoices/{id}/attachments/{attachmentId}` | Fetch attachment binary from external system | INVOICING_USER, AUTHORITY_VIEWER |

```java
@GetMapping("/{id}/image")
@PreAuthorize("hasAnyRole('INVOICING_USER', 'FUNCTION_ADMIN', 'BILLING_MANAGER', 'AUTHORITY_VIEWER')")
public ResponseEntity<byte[]> getInvoiceImage(@PathVariable Long id) {
    Invoice invoice = invoiceRepository.findById(id).orElseThrow();

    // AUTHORITY_VIEWER: only SENT or COMPLETED invoices (PD-171)
    if (isAuthorityViewer() && invoice.getStatus() != InvoiceStatus.SENT
            && invoice.getStatus() != InvoiceStatus.COMPLETED) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(null); // handled by step-56 authority-specific logic
    }

    // Invoice must have been transmitted before an image exists
    if (invoice.getInvoiceNumber() == null) {
        throw new InvoiceImageNotFoundException("Invoice has not been assigned a number yet");
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

## Frontend

### 51.4 View PDF Button on Invoice Detail

**Extension to `InvoiceDetailPage.jsx`** (step 29):

**"View Invoice PDF" button** — visible for invoices in SENT or COMPLETED status.

Two modes:
1. **Open in browser tab** (recommended): uses `window.open` to open a blob URL.
2. **Download**: triggers a file download.

```jsx
const ViewPdfButton = ({ invoiceId, invoiceNumber }) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleViewPdf = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await axios.get(`/api/v1/invoices/${invoiceId}/image`, {
        responseType: 'blob'
      })
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      window.open(url, '_blank')
    } catch (err) {
      if (err.response?.status === 502) {
        setError('External invoicing system is temporarily unavailable. Please try again later.')
      } else if (err.response?.status === 404) {
        setError('Invoice image not found in the external system.')
      } else {
        setError('Could not retrieve invoice PDF.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <button onClick={handleViewPdf} disabled={loading}>
        {loading ? 'Loading...' : 'View Invoice PDF'}
      </button>
      {error && <p className="error">{error}</p>}
    </>
  )
}
```

**API calls (additions to `src/api/invoices.js`):**
```js
export const getInvoiceImage = (invoiceId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/image`, { responseType: 'blob' })

export const getInvoiceAttachmentBinary = (invoiceId, attachmentId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/attachments/${attachmentId}`, { responseType: 'blob' })
```

---

## Verification Checklist

1. Mock external image endpoint to return a PDF binary → `GET /api/v1/invoices/{id}/image` returns HTTP 200 with `Content-Type: application/pdf` and the binary body.
2. Mock external endpoint to return 404 → `GET /api/v1/invoices/{id}/image` returns HTTP 404 with clear error message.
3. Mock external endpoint to return 503 (external system down) → WasteHero returns HTTP 502 with "The external invoicing system is temporarily unavailable."
4. INVOICING_USER can fetch DRAFT invoice image → succeeds (restriction to SENT/COMPLETED only applies to AUTHORITY_VIEWER role).
5. AUTHORITY_VIEWER can fetch SENT invoice image → succeeds.
6. AUTHORITY_VIEWER attempts to fetch DRAFT invoice image → HTTP 403 Forbidden.
7. Invoice with no invoice number (not yet transmitted) → HTTP 404 "Invoice has not been assigned a number yet."
8. `getInvoiceAttachmentBinary(invoiceId, attachmentId)` calls the correct attachment URL with invoice number and attachment identifier.
9. Open `InvoiceDetailPage` in FE for a SENT invoice — "View Invoice PDF" button visible; click opens PDF in new browser tab.
10. Mock external failure → FE shows specific error message (not generic "Error"), matching the 502/404 distinction.

---

## File Checklist

### Backend
- [ ] `integration/InvoiceImageService.java`
- [ ] `invoice/InvoiceImageController.java`
- [ ] `integration/InvoiceImageNotFoundException.java`
- [ ] `integration/ExternalSystemUnavailableException.java`
- [ ] `common/exception/GlobalExceptionHandler.java` — add 404/502 handlers

### Frontend
- [ ] `src/pages/invoices/components/ViewPdfButton.jsx`
- [ ] `src/api/invoices.js` — add `getInvoiceImage()` and `getInvoiceAttachmentBinary()` (extends step 29)
