# Step 28 — Seasonal Fee Config

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: SeasonalFeeConfig — "PD-288: Seasonal fees refer to payments such as monthly fees/update of seasonal fees, annual fees, block collection fees, rents. A certain basic fee has been defined for each property (e.g. permanent apartment, leisure apartment), which is billed once a year. The amount of the basic fee may vary annually."
- `Docs/structured_breakdown/03-business-logic.md` → Area 7: Scheduling — "SeasonalFeeGenerationService: Runs nightly. Fetches all SeasonalFeeConfig records where nextDueDate <= today. For each: creates a BillingEvent with origin = SEASONAL and all financial metadata inherited from the configured product. Advances nextDueDate by the frequency period."
- `Docs/structured_breakdown/02-data-layer.md` → Section: SeasonalFeeConfigRepository — "Query: active = true AND nextDueDate <= today. The nightly scheduler calls this and creates BillingEvents for each result. After generating, nextDueDate is advanced by the frequency period."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Seasonal Fees — "GET /seasonal-fee-configs / POST / PUT /{id} — CRUD for seasonal fee configurations."

---

## Goal

`SeasonalFeeConfig` automates the creation of `BillingEvent`s for time-bound recurring charges — annual property base fees, quarterly rents, monthly block collection charges. Without this entity, someone would have to manually create billing events for every property at the start of every billing period. With it, a nightly scheduler does the work automatically.

`SeasonalFeeGenerationService` is the scheduler. It queries all active configs whose `nextDueDate` has arrived, creates one `BillingEvent` per config with `origin = SEASONAL`, then advances `nextDueDate` by the configured frequency so the config is ready for the next period.

The FE provides a manual "Generate Now" button per config for testing — this calls a dedicated endpoint that triggers generation immediately without waiting for the nightly run.

---

## Backend

### 28.1 SeasonalFeeConfig Entity

**File:** `invoicing/src/main/java/com/example/invoicing/seasonalfee/SeasonalFeeConfig.java`

> **Requirement source:** `01-domain-model.md` — "SeasonalFeeConfig: holds the product, customer/property scope, billing frequency, amount, and nextDueDate."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `customerNumber` | `String` | `@Column(nullable = false, length = 20)` — the customer this fee belongs to |
| `product` | `Product` | `@ManyToOne(optional = false) @JoinColumn(name = "product_id")` — the product that drives financial metadata (account, cost centre, VAT) |
| `billingFrequency` | `BillingFrequency` | `@Enumerated(EnumType.STRING) @Column(nullable = false)` — reuse the enum from Step 23 |
| `amount` | `BigDecimal` | `@Column(nullable = false, precision = 10, scale = 2)` — the fee amount in euros (net, VAT-exclusive) |
| `nextDueDate` | `LocalDate` | `@Column(nullable = false)` — when this fee next generates a BillingEvent; advanced by the scheduler after each generation |
| `active` | `boolean` | `@Column(nullable = false)` default `true` — inactive configs are skipped by the scheduler |
| `propertyReference` | `String` | `@Column(length = 100)` nullable — the property this fee is linked to |
| `description` | `String` | `@Column(length = 255)` nullable — e.g. "Annual base fee — permanent apartment" |

```java
@Entity
@Table(name = "seasonal_fee_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalFeeConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingFrequency billingFrequency;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate nextDueDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String propertyReference;

    @Column(length = 255)
    private String description;
}
```

---

### 28.2 SeasonalFeeConfigRepository

**File:** `invoicing/src/main/java/com/example/invoicing/seasonalfee/SeasonalFeeConfigRepository.java`

> **Requirement source:** `02-data-layer.md` — "Query: active = true AND nextDueDate <= today."

```java
public interface SeasonalFeeConfigRepository
        extends JpaRepository<SeasonalFeeConfig, Long> {

    /**
     * Used by SeasonalFeeGenerationService nightly.
     * Returns all active configs whose next due date has arrived.
     */
    @Query("""
        SELECT s FROM SeasonalFeeConfig s
        WHERE s.active = true
          AND s.nextDueDate <= :today
        """)
    List<SeasonalFeeConfig> findDueForGeneration(@Param("today") LocalDate today);

    /** Find all configs for a specific customer — used by the per-customer FE page. */
    List<SeasonalFeeConfig> findByCustomerNumberOrderByNextDueDateAsc(String customerNumber);

    /** Find all active configs — used for the global list view. */
    List<SeasonalFeeConfig> findAllByActiveTrueOrderByNextDueDateAsc();
}
```

---

### 28.3 SeasonalFeeGenerationService

**File:** `invoicing/src/main/java/com/example/invoicing/seasonalfee/SeasonalFeeGenerationService.java`

> **Requirement source:** `03-business-logic.md` Area 7 — "Runs nightly. Fetches all SeasonalFeeConfig records where nextDueDate <= today. For each: creates a BillingEvent with origin = SEASONAL and all financial metadata inherited from the configured product. Advances nextDueDate by the frequency period."

| Method Signature | Description |
|---|---|
| `void runNightlyGeneration()` | `@Scheduled(cron = "0 0 2 * * *")` — runs at 2am every day. Calls `generateDueFees(LocalDate.now())`. |
| `List<BillingEvent> generateDueFees(LocalDate asOf)` | Core logic. Queries `findDueForGeneration(asOf)`, generates a `BillingEvent` for each, advances `nextDueDate`. Returns the list of created events. Accepts a date parameter so tests and manual triggers can pass any date. |
| `BillingEvent createEventFromConfig(SeasonalFeeConfig config)` | Builds a `BillingEvent` with: `eventDate = config.nextDueDate`, `status = IN_PROGRESS`, `origin = SEASONAL`, all financial metadata (account, cost centre, VAT rate by event date) resolved from the config's `product`. `wastePrice = config.amount`. |
| `void advanceNextDueDate(SeasonalFeeConfig config)` | Computes `newDate = config.nextDueDate + frequency` and saves: MONTHLY → +1 month, QUARTERLY → +3 months, ANNUAL → +1 year. |

**Scheduler configuration:**

```java
@Service
@RequiredArgsConstructor
public class SeasonalFeeGenerationService {

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runNightlyGeneration() {
        log.info("Seasonal fee generation started for date: {}", LocalDate.now());
        List<BillingEvent> created = generateDueFees(LocalDate.now());
        log.info("Seasonal fee generation completed. Created {} billing events.", created.size());
    }

    @Transactional
    public List<BillingEvent> generateDueFees(LocalDate asOf) {
        List<SeasonalFeeConfig> dueConfigs = repository.findDueForGeneration(asOf);
        List<BillingEvent> createdEvents = new ArrayList<>();
        for (SeasonalFeeConfig config : dueConfigs) {
            BillingEvent event = createEventFromConfig(config);
            createdEvents.add(billingEventRepository.save(event));
            advanceNextDueDate(config);
            repository.save(config);
        }
        return createdEvents;
    }
}
```

Enable scheduling in the main application class:

```java
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class InvoicingApplication { ... }
```

---

### 28.4 SeasonalFeeConfigController

**File:** `invoicing/src/main/java/com/example/invoicing/seasonalfee/SeasonalFeeConfigController.java`

Base path: `/api/v1/seasonal-fees`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/seasonal-fees` | List all active seasonal fee configs |
| GET | `/api/v1/seasonal-fees?customerNumber=` | List configs for a specific customer |
| GET | `/api/v1/seasonal-fees/{id}` | Get single config by ID |
| POST | `/api/v1/seasonal-fees` | Create new seasonal fee config |
| PUT | `/api/v1/seasonal-fees/{id}` | Update existing config (amount, nextDueDate, active) |
| DELETE | `/api/v1/seasonal-fees/{id}` | Soft-delete (sets active = false) |
| POST | `/api/v1/seasonal-fees/{id}/generate-now` | Manually trigger generation for this config — for testing |

**POST /api/v1/seasonal-fees — Request Body:**
```json
{
  "customerNumber": "123456",
  "productId": 12,
  "billingFrequency": "ANNUAL",
  "amount": 120.00,
  "nextDueDate": "2025-01-01",
  "propertyReference": "PROP-00789",
  "description": "Annual base fee — permanent apartment"
}
```

**POST /api/v1/seasonal-fees — Response Body (201 Created):**
```json
{
  "id": 7,
  "customerNumber": "123456",
  "productId": 12,
  "productName": "Annual Base Fee",
  "billingFrequency": "ANNUAL",
  "amount": 120.00,
  "nextDueDate": "2025-01-01",
  "propertyReference": "PROP-00789",
  "description": "Annual base fee — permanent apartment",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2025-01-02T08:00:00Z"
}
```

**POST /api/v1/seasonal-fees/{id}/generate-now — Response Body (200 OK):**
```json
{
  "configId": 7,
  "billingEventId": 1045,
  "eventDate": "2025-01-01",
  "amount": 120.00,
  "newNextDueDate": "2026-01-01",
  "message": "Billing event created successfully. Next due date advanced to 2026-01-01."
}
```

---

## Frontend

### 28.5 Seasonal Fees Page

**File:** `invoicing-fe/src/pages/billing/SeasonalFeesPage.jsx`

**Components:**
- `SeasonalFeesTable` — columns: Customer Number, Product, Billing Frequency, Amount (€), Next Due Date, Property Reference, Active (badge), Actions (Edit / Delete / Generate Now)
- `SeasonalFeeModal` — Add/Edit form with fields:
  - Customer Number (text)
  - Product (searchable dropdown from `/api/v1/products`)
  - Billing Frequency (select: MONTHLY / QUARTERLY / ANNUAL)
  - Amount (decimal input, labelled "Amount (€ net)")
  - Next Due Date (date picker)
  - Property Reference (text, optional)
  - Description (text, optional)
  - Active (checkbox)
- "Generate Now" button on each row — calls `POST /api/v1/seasonal-fees/{id}/generate-now`; on success, shows a toast with the created billing event ID and the new next due date, then refreshes the table

**Filter bar:** Customer number text input at the top — passes `?customerNumber=` to the API call.

**API calls via `src/api/seasonalFees.js`:**
```js
export const getSeasonalFees = (customerNumber) =>
  axios.get('/api/v1/seasonal-fees', { params: { customerNumber } })

export const getSeasonalFeeById = (id) =>
  axios.get(`/api/v1/seasonal-fees/${id}`)

export const createSeasonalFee = (data) =>
  axios.post('/api/v1/seasonal-fees', data)

export const updateSeasonalFee = (id, data) =>
  axios.put(`/api/v1/seasonal-fees/${id}`, data)

export const deleteSeasonalFee = (id) =>
  axios.delete(`/api/v1/seasonal-fees/${id}`)

export const generateNow = (id) =>
  axios.post(`/api/v1/seasonal-fees/${id}/generate-now`)
```

---

## Verification Checklist

1. Start the application with `@EnableScheduling` — confirm no startup errors related to the scheduler.
2. `POST /api/v1/seasonal-fees` with `billingFrequency=ANNUAL`, `nextDueDate=2025-01-01` — returns 201.
3. `POST /api/v1/seasonal-fees/{id}/generate-now` — returns 200 with `billingEventId` and `newNextDueDate=2026-01-01`.
4. Confirm the created `BillingEvent` has `status = IN_PROGRESS`, `origin = SEASONAL`, `wastePrice = 120.00`, and financial metadata (account, cost centre, VAT rate) populated from the product.
5. Call `generate-now` again immediately — confirm a SECOND billing event is NOT created because `nextDueDate` was advanced to 2026-01-01 and is no longer <= today.
6. Set `nextDueDate` back to today via `PUT /api/v1/seasonal-fees/{id}`. Call `generate-now` again — a new event is created and `nextDueDate` advances again.
7. Call `seasonalFeeGenerationService.generateDueFees(LocalDate.of(2025, 1, 1))` directly in a unit test with a config whose `nextDueDate = 2025-01-01` — confirm one event is created.
8. Call `generateDueFees(LocalDate.of(2024, 12, 31))` for the same config — confirm no events are created (date not yet due).
9. Test QUARTERLY frequency: `nextDueDate=2025-03-31`, `billingFrequency=QUARTERLY`. Call `generate-now` — `newNextDueDate` should be `2025-06-30`.
10. Test MONTHLY frequency: `nextDueDate=2025-01-31`. Call `generate-now` — `newNextDueDate` should be `2025-02-28`.
11. Set `active=false` on a config. Call `generateDueFees(today)` — the inactive config is skipped, no event created.
12. Open the Seasonal Fees page in the FE — table shows all configs with next due dates.
13. Click "Generate Now" on a config — toast shows created event ID and new next due date; the next due date in the table updates.
14. `GET /api/v1/seasonal-fees?customerNumber=123456` — returns only configs for that customer.

---

## File Checklist

### Backend
- [ ] `seasonalfee/SeasonalFeeConfig.java`
- [ ] `seasonalfee/SeasonalFeeConfigRepository.java`
- [ ] `seasonalfee/SeasonalFeeGenerationService.java`
- [ ] `seasonalfee/SeasonalFeeConfigController.java`
- [ ] `seasonalfee/dto/SeasonalFeeConfigRequest.java`
- [ ] `seasonalfee/dto/SeasonalFeeConfigResponse.java`
- [ ] `seasonalfee/dto/GenerateNowResponse.java`
- [ ] `InvoicingApplication.java` (modification — add `@EnableScheduling`)

### Frontend
- [ ] `src/api/seasonalFees.js`
- [ ] `src/pages/billing/SeasonalFeesPage.jsx`
- [ ] `src/components/seasonalfee/SeasonalFeesTable.jsx`
- [ ] `src/components/seasonalfee/SeasonalFeeModal.jsx`
