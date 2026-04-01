# Step 05 — Invoice Number Series

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: InvoiceNumberSeries
  - "Series name, prefix, format pattern"
  - "Atomic counter increment"
  - "Released number pool (never re-issued)"
- `Docs/structured_breakdown/02-data-layer.md` → Section: InvoiceNumberSeriesRepository
  - "Pessimistic locking for atomic number assignment, series by type"
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceNumberingService (Area 2)
  - "Atomic assignment with series lock"
- `Docs/structured_breakdown/04-api-layer.md` → Section 13: Invoice Number Series
  - "CRUD endpoints"
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: Simulation Mode Guard
  - "Skipped in simulation: invoice number assignment" — this service must be simulation-aware
- `Docs/structured_breakdown/07-build-order.md` → Step 1: Master Data Foundation

---

## Goal
Implement `InvoiceNumberSeries` — the entity that controls invoice number assignment. This is the most technically sensitive entity in Step 1 because:

1. Invoice numbers must be **unique and sequential** — never reused, never skipped
2. Assignment must be **atomic** under concurrent load (pessimistic write lock)
3. Numbers released (e.g. for cancelled invoices) go into a **released pool** and are never reassigned

**Prerequisite:** Step 01 must be complete (`BaseAuditEntity` must exist).

> **Important note from `06-cross-cutting.md`:** In simulation mode, `assignNextNumber()` must be a no-op — return a preview placeholder, never increment the actual counter.

---

## Backend

### 5.1 InvoiceNumberSeries Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/numberseries/InvoiceNumberSeries.java`

> **Requirement source:** `01-domain-model.md` — InvoiceNumberSeries:
> - Series name, prefix, format pattern
> - Atomic counter increment
> - Released number pool (never re-issued)

```java
@Entity
@Table(name = "invoice_number_series")
public class InvoiceNumberSeries extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String name;                // e.g. "MAIN_2024", "CREDIT_2024"

    @Column(nullable = false)
    private String prefix;             // e.g. "INV", "CR"

    @Column(nullable = false)
    private String formatPattern;      // e.g. "{PREFIX}-{YEAR}-{COUNTER:06d}"
                                       // → "INV-2024-000001"

    @Column(nullable = false)
    private Long currentCounter;       // Monotonically increasing, never decremented

    // Pool of released numbers (e.g. for cancelled invoices) — stored as CSV or JSON
    // These are never re-issued — kept for auditing only
    @ElementCollection
    @CollectionTable(name = "released_invoice_numbers", joinColumns = @JoinColumn(name = "series_id"))
    @Column(name = "released_number")
    private List<String> releasedNumbers = new ArrayList<>();
}
```

---

### 5.2 InvoiceNumberSeriesRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/numberseries/InvoiceNumberSeriesRepository.java`

> **Requirement source:** `02-data-layer.md` — InvoiceNumberSeriesRepository:
> - "Pessimistic locking for atomic number assignment"
> - This query must use `@Lock(PESSIMISTIC_WRITE)` to prevent concurrent double-assignment

```java
public interface InvoiceNumberSeriesRepository extends JpaRepository<InvoiceNumberSeries, Long> {

    // CRITICAL: Pessimistic write lock prevents concurrent threads from getting the same counter value
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceNumberSeries s WHERE s.id = :id")
    Optional<InvoiceNumberSeries> findByIdForUpdate(@Param("id") Long id);

    Optional<InvoiceNumberSeries> findByName(String name);
}
```

---

### 5.3 InvoiceNumberSeriesService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/numberseries/InvoiceNumberSeriesService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceNumberingService:
> - "Atomic assignment with series lock"
>
> `06-cross-cutting.md` — Simulation Mode Guard:
> - "Skipped in simulation: invoice number assignment"

Methods:
- `findAll()` → `List<InvoiceNumberSeries>`
- `findById(Long id)` → `InvoiceNumberSeries`
- `create(InvoiceNumberSeriesRequest)` → `InvoiceNumberSeries`
- `update(Long id, InvoiceNumberSeriesRequest)` → `InvoiceNumberSeries`

**Core method — atomic number assignment:**
```java
@Transactional
public String assignNextNumber(Long seriesId, boolean simulationMode) {
    if (simulationMode) {
        // Per 06-cross-cutting.md: never increment in simulation
        return "SIMULATION-PREVIEW";
    }

    // Pessimistic write lock — blocks concurrent threads until this transaction commits
    InvoiceNumberSeries series = repository.findByIdForUpdate(seriesId)
        .orElseThrow(() -> new EntityNotFoundException("Series not found: " + seriesId));

    long nextCounter = series.getCurrentCounter() + 1;
    series.setCurrentCounter(nextCounter);
    repository.save(series);

    return formatNumber(series, nextCounter);
}

private String formatNumber(InvoiceNumberSeries series, long counter) {
    // Apply formatPattern, e.g. "{PREFIX}-{YEAR}-{COUNTER:06d}"
    return series.getFormatPattern()
        .replace("{PREFIX}", series.getPrefix())
        .replace("{YEAR}", String.valueOf(LocalDate.now().getYear()))
        .replace("{COUNTER:06d}", String.format("%06d", counter));
}
```

**Released number pool:**
```java
@Transactional
public void releaseNumber(Long seriesId, String number) {
    // Mark a number as released (e.g. cancelled invoice)
    // Per 01-domain-model.md: "Released number pool (never re-issued)"
    // We record it but never re-assign it
    InvoiceNumberSeries series = repository.findById(seriesId).orElseThrow(...);
    series.getReleasedNumbers().add(number);
    repository.save(series);
}
```

---

### 5.4 InvoiceNumberSeriesController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/numberseries/InvoiceNumberSeriesController.java`

Base path: `/api/v1/invoice-number-series`

| Method | Path | Body / Params | Description |
|--------|------|----------------|-------------|
| GET | `/api/v1/invoice-number-series` | — | All series |
| GET | `/api/v1/invoice-number-series/{id}` | — | Single series |
| POST | `/api/v1/invoice-number-series` | request body | Create series |
| PUT | `/api/v1/invoice-number-series/{id}` | request body | Update series (name, prefix, pattern only — NOT counter) |
| POST | `/api/v1/invoice-number-series/{id}/assign` | `?simulation=true/false` | Assign and return next number |

> The `assign` endpoint is for testing only at this stage. In production it will be called internally by `InvoiceGenerationService`.

**Series request body (POST/PUT):**
```json
{
  "name": "MAIN_2024",
  "prefix": "INV",
  "formatPattern": "{PREFIX}-{YEAR}-{COUNTER:06d}"
}
```

**Series response body:**
```json
{
  "id": 1,
  "name": "MAIN_2024",
  "prefix": "INV",
  "formatPattern": "{PREFIX}-{YEAR}-{COUNTER:06d}",
  "currentCounter": 42,
  "releasedNumbersCount": 0,
  "createdBy": "system",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

**Assign response:**
```json
{
  "assignedNumber": "INV-2024-000043",
  "seriesId": 1,
  "simulationMode": false
}
```

---

## Frontend

### 5.5 Invoice Number Series Page

**File:** `invoicing-fe/src/pages/masterdata/InvoiceNumberSeriesPage.jsx`

Components:
- **SeriesTable** — columns: Name, Prefix, Format Pattern, Current Counter, Released Count, Actions (Edit / Test Assign)
- **SeriesModal** — form for Add/Edit: name (text), prefix (text), formatPattern (text with placeholder helper)
- **Test Assign Panel** — appears when clicking "Test Assign" on a row:
  - Shows a simulation checkbox (default checked for safety)
  - Button "Assign Number"
  - Shows the returned number in a highlighted box
  - Counter increments if simulation=false — warns the user before doing so

Add nav item under Master Data in `Layout.jsx`.

API calls in `src/api/invoiceNumberSeries.js`:
```js
export const getSeries = () => axios.get('/api/v1/invoice-number-series')
export const getSeriesById = (id) => axios.get(`/api/v1/invoice-number-series/${id}`)
export const createSeries = (data) => axios.post('/api/v1/invoice-number-series', data)
export const updateSeries = (id, data) => axios.put(`/api/v1/invoice-number-series/${id}`, data)
export const assignNumber = (id, simulation) =>
  axios.post(`/api/v1/invoice-number-series/${id}/assign`, null, { params: { simulation } })
```

---

## Verification Checklist

1. `mvn spring-boot:run` — Hibernate creates `invoice_number_series` and `released_invoice_numbers` tables
2. `POST /api/v1/invoice-number-series` — create series with `prefix=INV`, `formatPattern={PREFIX}-{YEAR}-{COUNTER:06d}`
3. `POST /api/v1/invoice-number-series/1/assign?simulation=false` → `"INV-2024-000001"` (counter was 0)
4. `POST /api/v1/invoice-number-series/1/assign?simulation=false` → `"INV-2024-000002"` (counter incremented)
5. `POST /api/v1/invoice-number-series/1/assign?simulation=true` → `"SIMULATION-PREVIEW"` (counter stays at 2)
6. `GET /api/v1/invoice-number-series/1` → `currentCounter: 2` (simulation did not change it)
7. **Concurrent test:** Call assign 3x simultaneously (e.g. with Postman Runner) → 3 distinct sequential numbers, no duplicates
8. Open FE Invoice Number Series page — table shows series with counter
9. Click "Test Assign" → panel opens, simulation checkbox checked by default
10. Assign in simulation mode — counter in table does NOT increment after page refresh
11. Assign without simulation — counter increments, assigned number shown

---

## File Checklist

### Backend
- [ ] `masterdata/numberseries/InvoiceNumberSeries.java`
- [ ] `masterdata/numberseries/InvoiceNumberSeriesRepository.java`
- [ ] `masterdata/numberseries/InvoiceNumberSeriesService.java`
- [ ] `masterdata/numberseries/InvoiceNumberSeriesController.java`
- [ ] `masterdata/numberseries/dto/InvoiceNumberSeriesRequest.java`
- [ ] `masterdata/numberseries/dto/InvoiceNumberSeriesResponse.java`
- [ ] `masterdata/numberseries/dto/AssignNumberResponse.java`

### Frontend
- [ ] `src/api/invoiceNumberSeries.js`
- [ ] `src/pages/masterdata/InvoiceNumberSeriesPage.jsx`
- [ ] `src/App.jsx` — add route for `/master-data/invoice-number-series`
- [ ] `src/components/Layout.jsx` — add nav item
