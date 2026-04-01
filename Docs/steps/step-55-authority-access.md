# Step 55 — Authority Invoice Access (Read-Only)

## References to Original Requirements
- `Docs/structured_breakdown/04-api-layer.md` → Section: Authority Access — GET /authority/customers/{id}/invoices, GET /authority/invoices/{id}/image
- `Docs/structured_breakdown/02-data-layer.md` → Section: InvoiceRepository — "Find invoices by customer for authority view"
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: AUTHORITY_VIEWER — read-only, only SENT and COMPLETED invoices

---

## Goal
Implement `AuthorityInvoiceViewController` with two read-only endpoints restricted to AUTHORITY_VIEWER role. Only SENT and COMPLETED invoices are accessible — no DRAFT, READY, ERROR, or CANCELLED invoices. No mutation endpoints exist in this scope. The FE is a separate route `/authority` with its own minimal layout showing only the invoice list; no edit, delete, or credit buttons are visible.

---

## Backend

### 55.1 AuthorityInvoiceViewController

**File:** `invoicing/src/main/java/com/example/invoicing/authority/AuthorityInvoiceViewController.java`

> **Requirement source:** `04-api-layer.md` — "These two endpoints are the entire authority interface. No write operations. No draft invoice access. Only sent and completed invoices."

```java
@RestController
@RequestMapping("/api/v1/authority")
@PreAuthorize("hasRole('AUTHORITY_VIEWER')")
public class AuthorityInvoiceViewController {

    /**
     * Returns all SENT and COMPLETED invoices for a customer.
     * Paginated, sorted by invoiceDate DESC.
     * DRAFT, READY, ERROR, CANCELLED invoices are NEVER returned.
     */
    @GetMapping("/customers/{customerId}/invoices")
    public ResponseEntity<Page<AuthorityInvoiceResponse>> getCustomerInvoices(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending());
        Page<Invoice> invoices = invoiceRepository
            .findSentOrCompletedByCustomerId(customerId, pageable);
        return ResponseEntity.ok(invoices.map(AuthorityInvoiceResponse::from));
    }
}
```

---

### 55.2 AuthorityInvoiceResponse DTO

**File:** `invoicing/src/main/java/com/example/invoicing/authority/AuthorityInvoiceResponse.java`

The authority view DTO must NEVER include:
- `internalComment` (not shown to customer or authority)
- Line item details beyond minimal information
- Status values other than SENT or COMPLETED (enforced by the query)

```java
public class AuthorityInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private InvoiceStatus status;           // only SENT or COMPLETED
    private String customerName;
    private String customText;              // customer-visible text only
    // NO internalComment field

    public static AuthorityInvoiceResponse from(Invoice invoice) {
        // ... map fields, explicitly exclude internalComment
    }
}
```

---

### 55.3 Security Configuration

The `/api/v1/authority/**` path requires AUTHORITY_VIEWER role. This is enforced at the method level with `@PreAuthorize` and also at the security config level:

```java
.requestMatchers("/api/v1/authority/**").hasRole("AUTHORITY_VIEWER")
```

AUTHORITY_VIEWER has no access to any other API paths. Any attempt to call non-authority endpoints as AUTHORITY_VIEWER returns HTTP 403.

---

### 55.4 Controller Endpoint Table

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/authority/customers/{id}/invoices` | SENT/COMPLETED invoices for a customer, paginated | AUTHORITY_VIEWER |

**GET response:**
```json
{
  "content": [
    {
      "id": 1001,
      "invoiceNumber": "PL2024000042",
      "invoiceDate": "2024-03-01",
      "dueDate": "2024-03-15",
      "grossAmount": 148.80,
      "netAmount": 120.00,
      "vatAmount": 28.80,
      "status": "SENT",
      "customerName": "Virtanen Oy",
      "customText": null
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Query parameters:**
- `page` (default 0)
- `size` (default 20)
- Date filter (optional): `dateFrom`, `dateTo` — restricts `invoiceDate` range.

---

## Frontend

### 55.5 Authority Portal — Separate Route

**File:** `invoicing-fe/src/pages/authority/AuthorityPortal.jsx`

This is a completely separate route (`/authority`) with a minimal layout. The Authority portal MUST NOT show:
- Billing events
- Invoice runs
- Customer management
- Any edit, delete, credit, or recall buttons
- Internal comments
- DRAFT, READY, ERROR, or CANCELLED invoices

**Components:**
- **AuthorityLayout** — minimal sidebar with only "Invoice Search" navigation. No other menu items.
- **CustomerSearchBar** — search field to find a customer by name or customer number.
- **AuthorityInvoiceList** — renders `InvoiceListTable` restricted to SENT/COMPLETED invoices. Columns: Invoice Number, Date, Amount (gross), Status. No checkboxes, no action buttons, no bulk actions.
- **ViewPdfButton** — "View Invoice" button per row → calls step-56 image retrieval (`GET /api/v1/authority/invoices/{id}/image`).

**Route setup (`App.jsx`):**
```jsx
<Route path="/authority" element={<AuthorityLayout />}>
  <Route index element={<AuthorityPortal />} />
</Route>
```

The `/authority` route must only be accessible to users with the AUTHORITY_VIEWER role. Other roles attempting to access `/authority` are redirected.

**File:** `invoicing-fe/src/pages/authority/AuthorityInvoiceList.jsx`

```jsx
const AuthorityInvoiceList = ({ customerId }) => {
  const [invoices, setInvoices] = useState([])
  useEffect(() => {
    getAuthorityInvoices(customerId).then(res => setInvoices(res.data.content))
  }, [customerId])

  return (
    <table>
      <thead>
        <tr>
          <th>Invoice Number</th><th>Date</th><th>Amount</th><th>Status</th><th>View</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map(inv => (
          <tr key={inv.id}>
            <td>{inv.invoiceNumber}</td>
            <td>{inv.invoiceDate}</td>
            <td>{formatCurrency(inv.grossAmount)}</td>
            <td><StatusBadge status={inv.status} /></td>
            <td><ViewPdfButton invoiceId={inv.id} useAuthorityEndpoint /></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
```

**API calls via `src/api/authority.js`:**
```js
export const getAuthorityInvoices = (customerId, params) =>
  axios.get(`/api/v1/authority/customers/${customerId}/invoices`, { params })
```

---

## Verification Checklist

1. `GET /api/v1/authority/customers/{id}/invoices` returns only SENT and COMPLETED invoices — DRAFT and CANCELLED invoices for the same customer are not included.
2. AUTHORITY_VIEWER role: GET request succeeds.
3. INVOICING_USER role attempting `GET /api/v1/authority/customers/{id}/invoices` → HTTP 403 Forbidden (authority endpoints require AUTHORITY_VIEWER).
4. AUTHORITY_VIEWER attempting `GET /api/v1/invoices/{id}` (non-authority endpoint) → HTTP 403 Forbidden.
5. Response JSON does NOT contain `internalComment` field.
6. Pagination: 25 invoices; request with `size=10` → 3 pages; first page has 10 items sorted by `invoiceDate` DESC.
7. Date filter: `dateFrom=2024-01-01&dateTo=2024-01-31` returns only invoices in January 2024.
8. Open `/authority` route in FE as AUTHORITY_VIEWER — minimal layout with no edit/credit/cancel buttons visible.
9. Non-AUTHORITY_VIEWER user navigates to `/authority` → redirected to login or 403 page.
10. `AuthorityInvoiceList` renders correctly with status badges; "View Invoice" button visible per row (calls step-56 endpoint).

---

## File Checklist

### Backend
- [ ] `authority/AuthorityInvoiceViewController.java`
- [ ] `authority/AuthorityInvoiceResponse.java`
- [ ] `invoice/InvoiceRepository.java` — `findSentOrCompletedByCustomerId()` (may already exist from step 29)
- [ ] `common/security/SecurityConfig.java` — add AUTHORITY_VIEWER role restriction for `/api/v1/authority/**`

### Frontend
- [ ] `src/pages/authority/AuthorityPortal.jsx`
- [ ] `src/pages/authority/AuthorityLayout.jsx`
- [ ] `src/pages/authority/AuthorityInvoiceList.jsx`
- [ ] `src/App.jsx` — add `/authority` route
- [ ] `src/api/authority.js`
