# Step 01 — Foundation Setup + VAT Rates

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: VatRate entity
- `Docs/structured_breakdown/02-data-layer.md` → Section: VatRateRepository
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data endpoints
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 9: "Event Date Drives VAT Rate — Not Today's Date"
- `Docs/structured_breakdown/07-build-order.md` → Step 1: Master Data Foundation

---

## Goal
Establish the project foundation (audit base, JPA auditing) and implement the first and most critical reference-data entity: `VatRate`. This entity is downstream-critical because every BillingEvent and invoice line item must resolve VAT by the **event date**, not today's date (see cross-cutting rule 9).

---

## Backend

### 1.1 Enable JPA Auditing

**File:** `invoicing/src/main/java/com/example/invoicing/InvoicingApplication.java`

Add `@EnableJpaAuditing` to the main class so Spring Data can populate `createdAt`, `lastModifiedAt`, etc. on all entities.

```java
@SpringBootApplication
@EnableJpaAuditing
public class InvoicingApplication { ... }
```

---

### 1.2 BaseAuditEntity

**File:** `invoicing/src/main/java/com/example/invoicing/common/audit/BaseAuditEntity.java`

All 17+ domain entities in this system inherit from this class.

> **Requirement source:** `06-cross-cutting.md` — "Base fields on all entities: createdBy, createdAt, lastModifiedBy, lastModifiedAt"

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    private String lastModifiedBy;

    @LastModifiedDate
    private Instant lastModifiedAt;
}
```

Also create an `AuditorAwareImpl` that returns a placeholder user (e.g., `"system"`) until Spring Security is wired in Step 6.

---

### 1.3 VatRate Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/vat/VatRate.java`

> **Requirement source:** `01-domain-model.md` — VatRate entity:
> - Fields: rate (decimal), validFrom, validTo
> - "VAT resolved BY EVENT DATE (not today's date)"
> - "Accounts with validity periods"

```java
@Entity
@Table(name = "vat_rates")
public class VatRate extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;               // e.g. "VAT_24", "VAT_0", "VAT_255"

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;           // e.g. 24.00, 0.00, 25.50 — NEVER double/float

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = true)
    private LocalDate validTo;         // null = currently valid, no end date
}
```

**Why BigDecimal:** `06-cross-cutting.md` implies financial precision. The example "25.5%" confirms decimal support is required.

---

### 1.4 VatRateRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/vat/VatRateRepository.java`

> **Requirement source:** `02-data-layer.md` — VatRateRepository:
> - "CRITICAL: resolved BY EVENT DATE, not by today's date"
> - Query: `WHERE validFrom <= :eventDate AND (validTo IS NULL OR validTo >= :eventDate)`

```java
public interface VatRateRepository extends JpaRepository<VatRate, Long> {

    // The core query used by BillingEventService, InvoiceGenerationService, etc.
    @Query("SELECT v FROM VatRate v WHERE v.validFrom <= :eventDate AND (v.validTo IS NULL OR v.validTo >= :eventDate)")
    List<VatRate> findByEventDate(@Param("eventDate") LocalDate eventDate);

    // Also needed: find all currently active rates
    @Query("SELECT v FROM VatRate v WHERE v.validFrom <= :today AND (v.validTo IS NULL OR v.validTo >= :today)")
    List<VatRate> findActive(@Param("today") LocalDate today);
}
```

---

### 1.5 VatRateService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/vat/VatRateService.java`

Methods:
- `findAll()` → `List<VatRate>`
- `findByEventDate(LocalDate)` → `List<VatRate>`
- `findActive()` → `List<VatRate>` (shorthand for today's date)
- `create(VatRateRequest)` → `VatRate`
- `update(Long id, VatRateRequest)` → `VatRate`
- `delete(Long id)` → void (throw if used by existing events — deferred validation for now)

---

### 1.6 VatRateController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/vat/VatRateController.java`

> **Requirement source:** `04-api-layer.md` — Section 12: Accounting Master Data
> - "CRUD endpoints for each" of: accounting-accounts, cost-centers, vat-rates, allocation-rules

Base path: `/api/v1/vat-rates`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/vat-rates` | List all VAT rates |
| GET | `/api/v1/vat-rates?eventDate=YYYY-MM-DD` | Rates valid on a specific event date |
| GET | `/api/v1/vat-rates?active=true` | Currently active rates only |
| POST | `/api/v1/vat-rates` | Create new VAT rate |
| PUT | `/api/v1/vat-rates/{id}` | Update VAT rate |
| DELETE | `/api/v1/vat-rates/{id}` | Delete VAT rate |

**Request body (POST/PUT):**
```json
{
  "code": "VAT_24",
  "rate": 24.00,
  "validFrom": "2024-01-01",
  "validTo": null
}
```

**Response body:**
```json
{
  "id": 1,
  "code": "VAT_24",
  "rate": 24.00,
  "validFrom": "2024-01-01",
  "validTo": null,
  "createdBy": "system",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

---

## Frontend

### 1.7 Project Setup — React Router + Layout Shell

**File:** `invoicing-fe/src/main.jsx` — wrap app in `<BrowserRouter>`
**File:** `invoicing-fe/src/App.jsx` — define routes
**File:** `invoicing-fe/src/components/Layout.jsx` — sidebar nav + main content area

Install dependencies:
```bash
npm install react-router-dom axios
```

Sidebar nav items (to be expanded per step):
- Master Data
  - VAT Rates (Step 1)
  - Accounting Accounts (Step 2)
  - Cost Centers (Step 3)
  - Products (Step 4)
  - Invoice Number Series (Step 5)

---

### 1.8 VAT Rates Page

**File:** `invoicing-fe/src/pages/masterdata/VatRatesPage.jsx`

Components:
- **VatRatesTable** — columns: Code, Rate (%), Valid From, Valid To, Actions (Edit / Delete)
- **VatRateModal** — form for Add/Edit: code (text), rate (number), validFrom (date), validTo (date, optional)
- **eventDate filter** — date input at top of page → calls `GET /api/v1/vat-rates?eventDate=`

API calls via `src/api/vatRates.js`:
```js
export const getVatRates = (eventDate) => axios.get('/api/v1/vat-rates', { params: { eventDate } })
export const createVatRate = (data) => axios.post('/api/v1/vat-rates', data)
export const updateVatRate = (id, data) => axios.put(`/api/v1/vat-rates/${id}`, data)
export const deleteVatRate = (id) => axios.delete(`/api/v1/vat-rates/${id}`)
```

Configure Axios base URL in `src/api/axios.js`:
```js
import axios from 'axios'
const instance = axios.create({ baseURL: 'http://localhost:8080' })
export default instance
```

> **CORS:** Add `@CrossOrigin(origins = "http://localhost:5173")` to the controller (dev only) or configure a global `CorsConfigurationSource` bean.

---

## Verification Checklist

1. `mvn spring-boot:run` — app starts, Hibernate creates `vat_rates` table in PostgreSQL
2. `POST /api/v1/vat-rates` — create a rate with `validFrom=2024-01-01`, `validTo=null`
3. `POST /api/v1/vat-rates` — create a second rate with `validFrom=2013-01-01`, `validTo=2023-12-31`
4. `GET /api/v1/vat-rates?eventDate=2022-06-15` — should return only the second rate (validTo covers June 2022)
5. `GET /api/v1/vat-rates?eventDate=2024-06-15` — should return only the first rate
6. `npm run dev` — FE starts at `http://localhost:5173`
7. Open VAT Rates page — table loads data from BE
8. Add a new VAT rate via modal — appears in table after save
9. Edit a VAT rate — changes reflected
10. Delete a VAT rate — removed from table

---

## File Checklist

### Backend
- [ ] `InvoicingApplication.java` — add `@EnableJpaAuditing`
- [ ] `common/audit/BaseAuditEntity.java`
- [ ] `common/audit/AuditorAwareImpl.java`
- [ ] `masterdata/vat/VatRate.java`
- [ ] `masterdata/vat/VatRateRepository.java`
- [ ] `masterdata/vat/VatRateService.java`
- [ ] `masterdata/vat/VatRateController.java`
- [ ] `masterdata/vat/dto/VatRateRequest.java`
- [ ] `masterdata/vat/dto/VatRateResponse.java`

### Frontend
- [ ] `src/api/axios.js`
- [ ] `src/api/vatRates.js`
- [ ] `src/components/Layout.jsx`
- [ ] `src/App.jsx` (with routes)
- [ ] `src/pages/masterdata/VatRatesPage.jsx`
