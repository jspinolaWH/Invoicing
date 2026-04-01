# Step 29 — Invoice Entity + InvoiceLineItem Entity

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: Invoice, InvoiceLineItem
- `Docs/structured_breakdown/02-data-layer.md` → Section: InvoiceRepository, InvoiceAttachmentRepository
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT", Rule 1: "Audit Trail"

---

## Goal
Define the two core invoice entities — `Invoice` and `InvoiceLineItem` — along with the `InvoiceAttachment` sub-entity and the `InvoiceRepository` with all queries required by downstream services. This is the central data model that every invoice generation, validation, credit note, and authority access feature depends on.

---

## Backend

### 29.1 Invoice Entity

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/Invoice.java`

> **Requirement source:** `01-domain-model.md` — Invoice entity fields

```java
@Entity
@Table(name = "invoices",
    uniqueConstraints = @UniqueConstraint(columnNames = "invoice_number"))
public class Invoice extends BaseAuditEntity {

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;               // null until assigned from series

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_number_series_id")
    private InvoiceNumberSeries invoiceNumberSeries;

    @Column(name = "template_code", nullable = false)
    private String templateCode;               // sent to external invoicing system as-is

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private InvoiceLanguage language;          // FI, SV, EN

    @Enumerated(EnumType.STRING)
    @Column(name = "invoicing_mode", nullable = false)
    private InvoicingMode invoicingMode;       // GROSS or NET (from customer profile)

    @Column(name = "reverse_charge_vat", nullable = false)
    private boolean reverseChargeVat = false;

    @Column(name = "custom_text", length = 2000)
    private String customText;                 // visible on printed invoice and in FINVOICE

    @Column(name = "internal_comment", length = 2000)
    private String internalComment;            // NOT shown to customer

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_run_id")
    private InvoiceRun invoiceRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;           // non-null only for credit notes

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType = InvoiceType.STANDARD; // STANDARD or CREDIT_NOTE

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "gross_amount", precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "vat_amount", precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceAttachment> attachments = new ArrayList<>();

    @Column(name = "scheduled_send_at")
    private Instant scheduledSendAt;
}
```

**Enums:**
- `InvoiceLanguage`: `FI`, `SV`, `EN`
- `InvoicingMode`: `GROSS`, `NET`
- `InvoiceStatus`: `DRAFT`, `READY`, `SENT`, `COMPLETED`, `ERROR`, `CANCELLED`
- `InvoiceType`: `STANDARD`, `CREDIT_NOTE`

---

### 29.2 InvoiceLineItem Entity

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceLineItem.java`

> **Requirement source:** `01-domain-model.md` — InvoiceLineItem entity

```java
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "description", nullable = false, length = 500)
    private String description;                // product name in customer language

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;                // resolved from event date — NEVER today

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_classification", nullable = false)
    private LegalClassification legalClassification; // PUBLIC_LAW or PRIVATE_LAW

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id")
    private AccountingAccount accountingAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "bundled", nullable = false)
    private boolean bundled = false;           // true when aggregated from multiple events

    @Column(name = "line_order")
    private Integer lineOrder;                 // display order on invoice
}
```

---

### 29.3 InvoiceAttachment Entity

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceAttachment.java`

> **Requirement source:** `01-domain-model.md` — "up to 10 PDF attachments, max 1MB total"
> **Requirement source:** `05-integration-layer.md` — FINVOICE attachment handling (SHA1, base64, SEI code, PDF/A)

```java
@Entity
@Table(name = "invoice_attachments")
public class InvoiceAttachment extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "attachment_identifier", nullable = false, length = 100)
    private String attachmentIdentifier;       // SHA1-based per FINVOICE spec

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;                   // must be application/pdf for PDF/A

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;                    // pre-encoding size

    @Column(name = "security_class", length = 20)
    private String securityClass;              // SEI code, e.g. "SEI01"

    @Lob
    @Column(name = "content_base64")
    private String contentBase64;              // base64-encoded content for FINVOICE
}
```

---

### 29.4 InvoiceRepository

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceRepository.java`

> **Requirement source:** `02-data-layer.md` — InvoiceRepository section

```java
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Open invoices for a customer — used when billing address changes (PD-281)
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('DRAFT', 'READY', 'SENT')")
    List<Invoice> findOpenByCustomerId(@Param("customerId") Long customerId);

    // All invoices in a run — used for run status view (PD-273)
    List<Invoice> findByInvoiceRunId(Long invoiceRunId);

    // Credit notes for an invoice — full crediting history (PD-269)
    @Query("SELECT i FROM Invoice i WHERE i.originalInvoice.id = :invoiceId AND i.invoiceType = 'CREDIT_NOTE'")
    List<Invoice> findCreditNotesByOriginalInvoiceId(@Param("invoiceId") Long invoiceId);

    // Authority view — only sent/completed invoices (PD-171)
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('SENT', 'COMPLETED') ORDER BY i.invoiceDate DESC")
    Page<Invoice> findSentOrCompletedByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    // Find by invoice number (must be unique)
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
```

---

### 29.5 InvoiceService

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceService.java`

Method signatures:
- `findById(Long id)` → `InvoiceResponse`
- `findByRunId(Long runId)` → `List<InvoiceResponse>`
- `updateCustomText(Long id, String customText)` → `InvoiceResponse`
- `bulkUpdateCustomText(List<Long> ids, String customText)` → `void`
- `removeSurcharge(Long id)` → `InvoiceResponse`
- `findOpenByCustomerId(Long customerId)` → `List<Invoice>` (used internally by sync services)
- `findSentOrCompletedByCustomerId(Long customerId, Pageable pageable)` → `Page<InvoiceResponse>`

---

### 29.6 InvoiceController

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceController.java`

Base path: `/api/v1/invoices`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/invoices/{id}` | Full invoice details with line items | INVOICING_USER |
| PATCH | `/api/v1/invoices/{id}/text` | Update custom text on one invoice | INVOICING_USER |
| POST | `/api/v1/invoices/bulk-text` | Set same text on multiple invoices | INVOICING_USER |
| POST | `/api/v1/invoices/{id}/remove-surcharge` | Remove invoicing surcharge | INVOICING_USER |

**GET /api/v1/invoices/{id} response:**
```json
{
  "id": 1001,
  "invoiceNumber": "PL2024000042",
  "templateCode": "WASTE_STANDARD",
  "language": "FI",
  "invoicingMode": "NET",
  "reverseChargeVat": false,
  "customText": "Thank you for your business.",
  "status": "DRAFT",
  "invoiceDate": "2024-03-01",
  "dueDate": "2024-03-15",
  "netAmount": 120.00,
  "grossAmount": 148.80,
  "vatAmount": 28.80,
  "invoiceType": "STANDARD",
  "originalInvoiceId": null,
  "lineItems": [
    {
      "id": 5001,
      "description": "Waste container emptying",
      "quantity": 4,
      "unitPrice": 30.00,
      "vatRate": 24.00,
      "netAmount": 120.00,
      "grossAmount": 148.80,
      "legalClassification": "PUBLIC_LAW",
      "accountingAccountCode": "3200",
      "costCenterCode": "KP-01",
      "bundled": true
    }
  ],
  "attachments": []
}
```

**PATCH /api/v1/invoices/{id}/text request:**
```json
{
  "customText": "Special notice: summer schedule applies."
}
```

**POST /api/v1/invoices/bulk-text request:**
```json
{
  "invoiceIds": [1001, 1002, 1003],
  "customText": "Summer schedule applies to all services."
}
```

---

## Frontend

### 29.7 Invoice Detail Page

**File:** `invoicing-fe/src/pages/invoices/InvoiceDetailPage.jsx`

Components:
- **InvoiceHeader** — displays invoice number, date, status badge (colour-coded), language, invoicing mode, customer name. Shows "CREDIT NOTE" banner if `invoiceType === 'CREDIT_NOTE'`.
- **InvoiceLineItemsTable** — columns: Description, Qty, Unit Price, VAT %, Net Amount, Gross Amount, Classification badge, Account, Cost Center. Bundled rows show a "B" badge.
- **InvoiceAmountSummary** — net total, VAT total, gross total displayed below the line items table.
- **InvoiceCustomTextPanel** — text area for `customText` (visible to customer). Inline edit with save button.
- **InvoiceAttachmentsList** — lists attached files; covered in detail in step-37.

**API calls via `src/api/invoices.js`:**
```js
export const getInvoice = (id) => axios.get(`/api/v1/invoices/${id}`)
export const updateInvoiceText = (id, customText) => axios.patch(`/api/v1/invoices/${id}/text`, { customText })
export const bulkUpdateInvoiceText = (invoiceIds, customText) => axios.post('/api/v1/invoices/bulk-text', { invoiceIds, customText })
export const removeSurcharge = (id) => axios.post(`/api/v1/invoices/${id}/remove-surcharge`)
```

---

## Verification Checklist

1. Run `mvn spring-boot:run` — Hibernate creates `invoices`, `invoice_line_items`, `invoice_attachments` tables with all columns and the UNIQUE constraint on `invoice_number`.
2. Insert a test invoice via SQL and call `GET /api/v1/invoices/{id}` — response includes `lineItems` and `attachments` arrays.
3. Verify the UNIQUE constraint: attempt to insert two invoices with the same `invoice_number` — database rejects with a constraint violation.
4. Call `PATCH /api/v1/invoices/{id}/text` — `customText` updates; `internalComment` is NOT returned in response JSON.
5. Call `POST /api/v1/invoices/bulk-text` with three IDs — all three invoices have updated `customText`.
6. Query `findOpenByCustomerId` via a test — only DRAFT/READY/SENT invoices are returned; COMPLETED and CANCELLED are excluded.
7. Query `findSentOrCompletedByCustomerId` — only SENT and COMPLETED invoices are returned (authority-safe).
8. Query `findCreditNotesByOriginalInvoiceId` — returns only invoices with `invoiceType = CREDIT_NOTE` linked to the original.
9. Open the Invoice Detail page in the FE — line items table renders with classification badges and bundled indicator.
10. Edit `customText` in the FE custom text panel — save succeeds and the updated text is reflected without a page reload.

---

## File Checklist

### Backend
- [ ] `invoice/Invoice.java`
- [ ] `invoice/InvoiceLineItem.java`
- [ ] `invoice/InvoiceAttachment.java`
- [ ] `invoice/InvoiceRepository.java`
- [ ] `invoice/InvoiceService.java`
- [ ] `invoice/InvoiceController.java`
- [ ] `invoice/dto/InvoiceResponse.java`
- [ ] `invoice/dto/InvoiceLineItemResponse.java`
- [ ] `invoice/dto/UpdateCustomTextRequest.java`
- [ ] `invoice/dto/BulkCustomTextRequest.java`
- [ ] `invoice/enums/InvoiceStatus.java`
- [ ] `invoice/enums/InvoiceType.java`
- [ ] `invoice/enums/InvoiceLanguage.java`
- [ ] `invoice/enums/InvoicingMode.java`
- [ ] `invoice/enums/LegalClassification.java`

### Frontend
- [ ] `src/pages/invoices/InvoiceDetailPage.jsx`
- [ ] `src/pages/invoices/components/InvoiceHeader.jsx`
- [ ] `src/pages/invoices/components/InvoiceLineItemsTable.jsx`
- [ ] `src/pages/invoices/components/InvoiceAmountSummary.jsx`
- [ ] `src/pages/invoices/components/InvoiceCustomTextPanel.jsx`
- [ ] `src/api/invoices.js`
