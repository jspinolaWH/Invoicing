# Step 23 — Billing Cycles

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingCycle — "BillingCycle: linked to a contract, service, or property. Holds: frequency (MONTHLY / QUARTERLY / ANNUAL), the entity it applies to, and the next billing date. The InvoiceRun service uses this to know which events fall into the current window."
- `Docs/structured_breakdown/02-data-layer.md` → Section: BillingCycleRepository — "Query: nextBillingDate <= runDate AND frequency matches the run type."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Billing Cycles and Restrictions — "GET /billing-cycles, POST /billing-cycles, PUT /billing-cycles/{id}"
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingCycle/BillingRestriction — "PD-291: Each service or event type is linked to a predefined billing cycle, such as monthly, quarterly, or annually."

---

## Goal

`BillingCycle` defines the invoicing schedule for a customer's contract, property, or service type. When `InvoiceRunService` starts a run, it queries all cycles whose `nextBillingDate` has arrived and groups the matching events into batch invoices. Without this entity, every event would need to be checked individually — the cycle provides the scheduling anchor.

This step delivers the full CRUD stack for managing billing cycles, plus the repository query that `InvoiceRunService` depends on to determine which cycles are "due" in the current run window.

---

## Backend

### 23.1 BillingFrequency Enum

**File:** `invoicing/src/main/java/com/example/invoicing/billingcycle/BillingFrequency.java`

```java
public enum BillingFrequency {
    MONTHLY,
    QUARTERLY,
    ANNUAL
}
```

---

### 23.2 BillingCycle Entity

**File:** `invoicing/src/main/java/com/example/invoicing/billingcycle/BillingCycle.java`

> **Requirement source:** `01-domain-model.md` — "BillingCycle: linked to a contract, service, or property. Holds: frequency (MONTHLY / QUARTERLY / ANNUAL), the entity it applies to, and the next billing date."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `customerNumber` | `String` | `@Column(nullable = false, length = 20)` — the 6–9 digit customer ID this cycle belongs to |
| `frequency` | `BillingFrequency` | `@Enumerated(EnumType.STRING) @Column(nullable = false)` |
| `nextBillingDate` | `LocalDate` | `@Column(nullable = false)` — when this cycle next triggers; advanced after each run |
| `description` | `String` | `@Column(length = 255)` nullable — e.g. "Monthly bin emptying", "Annual base fee" |
| `contractReference` | `String` | `@Column(length = 100)` nullable — optional link to the contract this cycle applies to |
| `propertyReference` | `String` | `@Column(length = 100)` nullable — optional link to a property |
| `serviceType` | `String` | `@Column(length = 100)` nullable — the type of service this schedule governs |
| `active` | `boolean` | `@Column(nullable = false)` default `true` — inactive cycles are excluded from run queries |

```java
@Entity
@Table(name = "billing_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCycle extends BaseAuditEntity {

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingFrequency frequency;

    @Column(nullable = false)
    private LocalDate nextBillingDate;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String contractReference;

    @Column(length = 100)
    private String propertyReference;

    @Column(length = 100)
    private String serviceType;

    @Column(nullable = false)
    private boolean active = true;
}
```

---

### 23.3 BillingCycleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/billingcycle/BillingCycleRepository.java`

> **Requirement source:** `02-data-layer.md` — "Query: nextBillingDate <= runDate AND frequency matches the run type. The invoice run service uses this to know which contracts/services/properties have a billing cycle ending in this run window."

```java
public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    /**
     * Find all active cycles due on or before the given run date.
     * Called by InvoiceRunService at the start of every billing run.
     */
    @Query("""
        SELECT c FROM BillingCycle c
        WHERE c.active = true
          AND c.nextBillingDate <= :runDate
        """)
    List<BillingCycle> findCyclesDueInRunWindow(@Param("runDate") LocalDate runDate);

    /**
     * Same query but scoped to a specific frequency — used when a run only
     * processes monthly cycles, for example.
     */
    @Query("""
        SELECT c FROM BillingCycle c
        WHERE c.active = true
          AND c.nextBillingDate <= :runDate
          AND c.frequency = :frequency
        """)
    List<BillingCycle> findCyclesDueByFrequency(
            @Param("runDate") LocalDate runDate,
            @Param("frequency") BillingFrequency frequency
    );

    /** Find all cycles for a specific customer — used on the customer profile page. */
    List<BillingCycle> findByCustomerNumberAndActiveTrue(String customerNumber);
}
```

---

### 23.4 BillingCycleService

**File:** `invoicing/src/main/java/com/example/invoicing/billingcycle/BillingCycleService.java`

| Method Signature | Description |
|---|---|
| `List<BillingCycleResponse> findAll()` | Returns all active billing cycles ordered by `nextBillingDate` ascending |
| `List<BillingCycleResponse> findByCustomer(String customerNumber)` | Returns all cycles for a specific customer |
| `BillingCycleResponse findById(Long id)` | Returns one cycle or throws `ResourceNotFoundException` |
| `BillingCycleResponse create(BillingCycleRequest request)` | Creates and persists a new cycle |
| `BillingCycleResponse update(Long id, BillingCycleRequest request)` | Updates frequency, nextBillingDate, description, active flag |
| `List<BillingCycle> findDueCycles(LocalDate runDate)` | Called by `InvoiceRunService` — delegates to `findCyclesDueInRunWindow()` |
| `void advanceNextBillingDate(BillingCycle cycle)` | After a successful run, advances `nextBillingDate` by the frequency period: MONTHLY → +1 month, QUARTERLY → +3 months, ANNUAL → +1 year |

---

### 23.5 BillingCycleController

**File:** `invoicing/src/main/java/com/example/invoicing/billingcycle/BillingCycleController.java`

Base path: `/api/v1/billing-cycles`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/billing-cycles` | List all active billing cycles |
| GET | `/api/v1/billing-cycles?customerNumber=` | Filter cycles for a specific customer |
| GET | `/api/v1/billing-cycles/{id}` | Get single billing cycle by ID |
| POST | `/api/v1/billing-cycles` | Create a new billing cycle |
| PUT | `/api/v1/billing-cycles/{id}` | Update an existing billing cycle |

**POST /api/v1/billing-cycles — Request Body:**
```json
{
  "customerNumber": "123456",
  "frequency": "MONTHLY",
  "nextBillingDate": "2025-02-01",
  "description": "Monthly bin emptying",
  "contractReference": "CONTRACT-2024-001",
  "propertyReference": null,
  "serviceType": "CONTAINER_EMPTYING"
}
```

**POST /api/v1/billing-cycles — Response Body (201 Created):**
```json
{
  "id": 8,
  "customerNumber": "123456",
  "frequency": "MONTHLY",
  "nextBillingDate": "2025-02-01",
  "description": "Monthly bin emptying",
  "contractReference": "CONTRACT-2024-001",
  "propertyReference": null,
  "serviceType": "CONTAINER_EMPTYING",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2025-01-10T08:00:00Z"
}
```

**PUT /api/v1/billing-cycles/{id} — Request Body (example: change to quarterly):**
```json
{
  "frequency": "QUARTERLY",
  "nextBillingDate": "2025-04-01",
  "description": "Quarterly bin emptying",
  "active": true
}
```

---

## Frontend

### 23.6 Billing Cycles Page

**File:** `invoicing-fe/src/pages/billing/BillingCyclesPage.jsx`

**Components:**
- `BillingCyclesTable` — columns: Customer Number, Frequency, Next Billing Date, Description, Service Type, Active (badge), Actions (Edit)
- `BillingCycleModal` — Add/Edit form with fields: customer number (text), frequency (select: MONTHLY / QUARTERLY / ANNUAL), next billing date (date picker), description (text), contract reference (text, optional), service type (text, optional), active (checkbox)
- Filter bar at top: customer number text input → filters table client-side or calls API with `?customerNumber=`

**API calls via `src/api/billingCycles.js`:**
```js
export const getBillingCycles = (customerNumber) =>
  axios.get('/api/v1/billing-cycles', { params: { customerNumber } })

export const getBillingCycleById = (id) =>
  axios.get(`/api/v1/billing-cycles/${id}`)

export const createBillingCycle = (data) =>
  axios.post('/api/v1/billing-cycles', data)

export const updateBillingCycle = (id, data) =>
  axios.put(`/api/v1/billing-cycles/${id}`, data)
```

---

## Verification Checklist

1. Start the application — Hibernate creates the `billing_cycles` table with all columns including `customer_number`, `frequency`, `next_billing_date`, `active`.
2. `POST /api/v1/billing-cycles` with `frequency=MONTHLY`, `nextBillingDate=2025-01-31` — returns 201 with the created record.
3. `GET /api/v1/billing-cycles?customerNumber=123456` — returns only cycles belonging to customer 123456.
4. Call `BillingCycleRepository.findCyclesDueInRunWindow(2025-02-01)` — returns cycles with `nextBillingDate <= 2025-02-01`. Cycles with `nextBillingDate=2025-03-01` are NOT returned.
5. Call `findCyclesDueByFrequency(2025-02-01, MONTHLY)` — returns only MONTHLY cycles due; QUARTERLY and ANNUAL cycles due on the same date are excluded.
6. Call `advanceNextBillingDate()` on a MONTHLY cycle with `nextBillingDate=2025-01-31` — confirm `nextBillingDate` becomes `2025-02-28` (February end-of-month).
7. Call `advanceNextBillingDate()` on a QUARTERLY cycle — `nextBillingDate` advances by 3 months.
8. Call `advanceNextBillingDate()` on an ANNUAL cycle — `nextBillingDate` advances by 1 year.
9. Set a cycle's `active=false` via `PUT /api/v1/billing-cycles/{id}`. Call `findCyclesDueInRunWindow()` — the inactive cycle does not appear.
10. Open the Billing Cycles page in the FE — table renders all cycles with correct frequency and next billing date.
11. Add a new cycle via the modal — it appears in the table after save.
12. Edit frequency from MONTHLY to QUARTERLY via the modal — the change is reflected immediately in the table.

---

## File Checklist

### Backend
- [ ] `billingcycle/BillingFrequency.java` (enum)
- [ ] `billingcycle/BillingCycle.java`
- [ ] `billingcycle/BillingCycleRepository.java`
- [ ] `billingcycle/BillingCycleService.java`
- [ ] `billingcycle/BillingCycleController.java`
- [ ] `billingcycle/dto/BillingCycleRequest.java`
- [ ] `billingcycle/dto/BillingCycleResponse.java`

### Frontend
- [ ] `src/api/billingCycles.js`
- [ ] `src/pages/billing/BillingCyclesPage.jsx`
- [ ] `src/components/billingcycle/BillingCyclesTable.jsx`
- [ ] `src/components/billingcycle/BillingCycleModal.jsx`
