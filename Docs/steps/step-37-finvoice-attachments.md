# Step 37 — FINVOICE Attachments

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → Section 3: FINVOICE — attachment handling (SHA1, base64, SEI, PDF/A, count ≤ 10, size ≤ 1MB)
- `Docs/structured_breakdown/02-data-layer.md` → Section: InvoiceAttachmentRepository
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices — POST/GET /invoices/{id}/attachments; PUT /invoice-runs/{id}/batch-attachment

---

## Goal
Implement attachment upload and retrieval for invoices. Constraints: up to 10 PDF/A files per invoice, total size ≤ 1 MB before base64 encoding, each file identified by a SHA1 hash, base64-encoded in the FINVOICE `<AttachmentDetails>` section, security class specified via SEI code. Also handle batch attachments for invoice runs: a pre-uploaded file reference (identifier only) is written to every invoice in the run — the binary is not re-transmitted in FINVOICE.

---

## Backend

### 37.1 InvoiceAttachmentRepository

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceAttachmentRepository.java`

> **Requirement source:** `02-data-layer.md` — InvoiceAttachmentRepository

```java
public interface InvoiceAttachmentRepository extends JpaRepository<InvoiceAttachment, Long> {

    List<InvoiceAttachment> findByInvoiceId(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM InvoiceAttachment a WHERE a.invoiceId = :invoiceId")
    long sumSizeBytesByInvoiceId(@Param("invoiceId") Long invoiceId);
}
```

---

### 37.2 InvoiceAttachmentService

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceAttachmentService.java`

> **Requirement source:** `05-integration-layer.md` — "Up to ten attachments; total size ≤ 1MB before encoding; SHA1 identifier; base64 content; SEI code; PDF/A only"

```java
@Service
@Transactional
public class InvoiceAttachmentService {

    private static final int MAX_ATTACHMENTS = 10;
    private static final long MAX_TOTAL_BYTES = 1_048_576L;  // 1 MB

    /**
     * Upload and attach a PDF/A file to an invoice.
     * Validates: format, count limit, total size limit, PDF/A compliance.
     */
    public InvoiceAttachmentResponse upload(Long invoiceId, MultipartFile file, String securityClass) {
        // 1. Verify invoice exists and is in DRAFT or READY status
        // 2. Verify file is PDF (MIME type = application/pdf)
        // 3. Verify count < 10
        // 4. Verify current total + file.size <= 1MB
        // 5. Compute SHA1 of file bytes
        // 6. Build attachmentIdentifier = invoiceNumber + "_" + sha1Hex
        // 7. Base64-encode the file bytes
        // 8. Save InvoiceAttachment entity
        // 9. Return response
    }

    /**
     * List attachment metadata for an invoice (no binary content in this response).
     */
    public List<InvoiceAttachmentResponse> listAttachments(Long invoiceId);

    /**
     * Compute SHA1 hex of file bytes.
     */
    private String computeSha1(byte[] bytes) {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return Hex.encodeHexString(digest.digest(bytes));
    }
}
```

**Attachment identifier format (per FINVOICE spec):**
> `AttachmentIdentifier` = SHA1 checksum of the attachment content (hex-encoded), prefixed with the invoice message ID.

Example: `INV-2024-000042_a3f9e2b1c0d8...`

---

### 37.3 PDF/A Validation

PDF/A (archive-compliant PDF) validation. Use Apache PDFBox's `PdfAValidator` or check the PDF file header for PDF/A markers.

```java
private void validatePdfA(byte[] bytes) throws AttachmentValidationException {
    // Quick check: PDF/A conformance statement in XMP metadata
    // Full check: use PDFBox PDFAValidator if available
    if (!isPdfA(bytes)) {
        throw new AttachmentValidationException(
            "Only PDF/A format is accepted for invoice attachments (FINVOICE requirement)");
    }
}
```

---

### 37.4 FINVOICE AttachmentDetails XML Generation

**Extension to `FinvoiceBuilderService`** (from step 35):

For each `InvoiceAttachment` on the invoice, append an `<AttachmentDetails>` block:

```xml
<AttachmentDetails>
  <AttachmentIdentifier>INV-2024-000042_a3f9e2b1c0d8abc123</AttachmentIdentifier>
  <AttachmentName>service-report.pdf</AttachmentName>
  <AttachmentMimeType>application/pdf</AttachmentMimeType>
  <AttachmentSecurityClass>SEI01</AttachmentSecurityClass>
  <AttachmentContent>JVBERi0xLjQKJcOkw7zDs...</AttachmentContent>
</AttachmentDetails>
```

For **batch attachments** (pre-uploaded to the external invoicing system): the `<AttachmentContent>` is omitted; only the identifier is present. The external system resolves the binary by identifier.

```xml
<!-- Batch attachment — reference only, no embedded binary -->
<AttachmentDetails>
  <AttachmentIdentifier>BATCH-2024-Q1-SCHEDULES</AttachmentIdentifier>
  <AttachmentName>schedule.pdf</AttachmentName>
  <!-- No AttachmentContent element -->
</AttachmentDetails>
```

---

### 37.5 Batch Attachment Endpoint

**Extension to `InvoiceRunController`** (step 38/41):

| Method | Path | Description | Role |
|--------|------|-------------|------|
| PUT | `/api/v1/invoice-runs/{id}/batch-attachment` | Associate a pre-uploaded attachment identifier with all invoices in the run | INVOICING_USER |

**Request:**
```json
{
  "attachmentIdentifier": "BATCH-2024-Q1-SCHEDULES",
  "filename": "schedule.pdf",
  "mimeType": "application/pdf",
  "securityClass": "SEI01"
}
```

**Response:** HTTP 200 with count of invoices updated.

---

### 37.6 InvoiceAttachmentController

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceAttachmentController.java`

Base path: `/api/v1/invoices/{id}/attachments`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/invoices/{id}/attachments` | List attachment metadata | INVOICING_USER |
| POST | `/api/v1/invoices/{id}/attachments` | Upload a PDF/A attachment | INVOICING_USER |
| GET | `/api/v1/invoices/{id}/attachments/{attachmentId}` | Download one attachment binary | INVOICING_USER |

**POST upload** — multipart/form-data:
- `file` (binary) — the PDF/A file
- `securityClass` (string, optional) — SEI code, e.g. `SEI01`

**POST response:**
```json
{
  "id": 201,
  "invoiceId": 1001,
  "attachmentIdentifier": "INV-2024-000042_a3f9e2b1c0d8abc123",
  "filename": "service-report.pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 153600,
  "securityClass": "SEI01",
  "createdAt": "2024-03-01T10:30:00Z"
}
```

**Error responses:**
- `400 Bad Request` — not PDF/A, or count would exceed 10, or size would exceed 1 MB.
- `409 Conflict` — invoice is in SENT/COMPLETED status (cannot attach after sending).

---

## Frontend

### 37.7 Attachment Upload Panel

**File:** `invoicing-fe/src/pages/invoices/components/InvoiceAttachmentsPanel.jsx`

Components:
- **AttachmentsList** — shows existing attachments: filename, size (human-readable), security class, upload date. Download button per row.
- **AttachmentUploadZone** — drag-and-drop zone or file picker. Accepts `.pdf` files only. Shows current count (e.g. "2 / 10") and remaining capacity (e.g. "Available: 742 KB of 1 MB").
- **SecurityClassSelector** — dropdown: No security class, SEI01, SEI02, SEI03.
- **UploadProgressBar** — shown during upload.

**Size/count enforcement in FE** (client-side pre-check, not a substitute for server validation):
- Count check: if `attachments.length >= 10`, disable the upload zone and show "Maximum 10 attachments reached."
- Size check: sum current sizes + selected file size; if > 1 MB, show error before upload.

**Batch Attachment section** (on Invoice Run detail page, step 38):
- Text input for `attachmentIdentifier`.
- "Apply to All Invoices" button → calls `PUT /api/v1/invoice-runs/{id}/batch-attachment`.

**API calls via `src/api/invoiceAttachments.js`:**
```js
export const listAttachments = (invoiceId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/attachments`)

export const uploadAttachment = (invoiceId, file, securityClass) => {
  const form = new FormData()
  form.append('file', file)
  if (securityClass) form.append('securityClass', securityClass)
  return axios.post(`/api/v1/invoices/${invoiceId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const downloadAttachment = (invoiceId, attachmentId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/attachments/${attachmentId}`, { responseType: 'blob' })

export const setBatchAttachment = (runId, data) =>
  axios.put(`/api/v1/invoice-runs/${runId}/batch-attachment`, data)
```

---

## Verification Checklist

1. `POST /api/v1/invoices/{id}/attachments` with a valid PDF/A file ≤ 500 KB → HTTP 200, attachment saved with SHA1 identifier.
2. Upload 10 attachments → 11th upload returns HTTP 400 "Maximum 10 attachments exceeded."
3. Upload files totalling 900 KB, then attempt to upload a 200 KB file → HTTP 400 "Total attachment size would exceed 1 MB."
4. Upload a non-PDF file (e.g. `.docx`) → HTTP 400 "Only PDF/A format accepted."
5. `GET /api/v1/invoices/{id}/attachments` returns metadata list without binary content (no `contentBase64` in the response).
6. `GET /api/v1/invoices/{id}/attachments/{attachmentId}` returns the binary PDF with `Content-Type: application/pdf`.
7. After uploading, generate the FINVOICE for the invoice — `<AttachmentDetails>` section contains correct identifier, filename, and base64 content.
8. `PUT /api/v1/invoice-runs/{id}/batch-attachment` — all invoices in the run have the batch identifier written to their FINVOICE (reference only, no binary).
9. Open `InvoiceAttachmentsPanel` in FE — count indicator shows "2 / 10"; remaining capacity bar updates after each upload.
10. Attempt upload via FE on an invoice in SENT status — server returns HTTP 409; FE shows appropriate error message.

---

## File Checklist

### Backend
- [ ] `invoice/InvoiceAttachmentRepository.java` (extends stub from step 29)
- [ ] `invoice/InvoiceAttachmentService.java`
- [ ] `invoice/InvoiceAttachmentController.java`
- [ ] `invoice/AttachmentValidationException.java`
- [ ] `invoice/dto/InvoiceAttachmentResponse.java`
- [ ] `invoice/dto/BatchAttachmentRequest.java`
- [ ] `finvoice/FinvoiceBuilderService.java` — add `buildAttachmentDetailsXml()` method

### Frontend
- [ ] `src/pages/invoices/components/InvoiceAttachmentsPanel.jsx`
- [ ] `src/pages/invoices/components/AttachmentsList.jsx`
- [ ] `src/pages/invoices/components/AttachmentUploadZone.jsx`
- [ ] `src/pages/invoices/components/SecurityClassSelector.jsx`
- [ ] `src/api/invoiceAttachments.js`
