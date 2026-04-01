# Step 24 — Billing Restrictions

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingCycle — "BillingRestriction: an override configuration that marks certain service types as IMMEDIATE — they bypass the cycle grouping entirely and are invoiced right after the event. PD-292: A one-off service such as septic tank emptying should not be delayed by other recurring services."
- `Docs/structured_breakdown/01-domain-model.md` → Section: InvoiceRun — "PD-293: The following can be used as selection criteria for invoicing events per invoicing run: municipality, minimum invoice amount, invoicing period, customer type, events of a certain type of service, reception location, service responsibility."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Billing Cycles and Restrictions — "GET /billing-restrictions, POST /billing-restrictions, PUT /billing-restrictions/{id} — Configures which service types bypass the billing cycle and go to immediate invoicing."

---

## Goal

`BillingRestriction` serves two related but distinct purposes:

1. **Immediate invoicing override** — certain service types (e.g. septic tank emptying, one-off hazardous waste collections) must not wait for the next billing cycle. A `BillingRestriction` record marks these as IMMEDIATE, causing `InvoiceRunService` to bypass cycle grouping and generate an invoice as soon as the event is recorded.

2. **Run filter criteria** — the restriction record also carries the standard filter fields from PD-293 (municipality, customerType, minimum amount, period). `InvoiceRunService` reads these to decide which events to include or exclude from a given run.

Both uses point at the same entity. The `billingType` enum distinguishes records that are pure IMMEDIATE overrides from those that are filter-only configurations.

---

## Backend

### 24.1 BillingType Enum

**File:** `invoicing/src/main/java/com/example/invoicing/billingrestriction/BillingType.java`

```java
public enum BillingType {
    /** Service bypasses billing cycles — invoiced immediately after the event. */
    IMMEDIATE,
    /** Service follows the customer's configured billing cycle (default behaviour). */
    CYCLE_BASED
}
```

---

### 24.2 BillingRestriction Entity

**File:** `invoicing/src/main/java/com/example/invoicing/billingrestriction/BillingRestriction.java`

> **Requirement source:** `01-domain-model.md` — "PD-292: A one-off service such as septic tank emptying should not be delayed by other recurring services in the invoicing process."
> **Requirement source:** `01-domain-model.md` — "PD-293: filter criteria: municipality, minimum invoice amount, invoicing period, customer type, events of a certain type of service, reception location, service responsibility."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `municipality` | `String` | `@Column(length = 100)` nullable — restrict to this municipality; null = all municipalities |
| `customerType` | `String` | `@Column(length = 50)` nullable — e.g. `"RESIDENTIAL"`, `"BUSINESS"`, `"MUNICIPAL"` |
| `serviceType` | `String` | `@Column(length = 100)` nullable — the service/event type this restriction applies to |
| `locationId` | `Long` | `@Column` nullable — restrict to a specific reception/unload location |
| `minAmount` | `BigDecimal` | `@Column(precision = 10, scale = 2)` nullable — events below this net amount are excluded from the run |
| `period` | `String` | `@Column(length = 50)` nullable — e.g. `"2025-Q1"`, `"2025-01"` — restricts the run to events in this period |
| `serviceResponsibility` | `String` | `@Column(length = 30)` nullable — `"PUBLIC_LAW"` or `"PRIVATE_LAW"` filter |
| `billingType` | `BillingType` | `@Enumerated(EnumType.STRING) @Column(nullable = false)` — IMMEDIATE or CYCLE_BASED |
| `description` | `String` | `@Column(length = 255)` nullable — admin note explaining why this restriction exists |
| `active` | `boolean` | `@Column(nullable = false)` default `true` |

```java
@Entity
@Table(name = "billing_restrictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRestriction extends BaseAuditEntity {

    @Column(length = 100)
    private String municipality;

    @Column(length = 50)
    private String customerType;

    @Column(length = 100)
    private String serviceType;

    @Column
    private Long locationId;

    @Column(precision = 10, scale = 2)
    private BigDecimal minAmount;

    @Column(length = 50)
    private String period;

    @Column(length = 30)
    private String serviceResponsibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingType billingType;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
```

---

### 24.3 BillingRestrictionRepository

**File:** `invoicing/src/main/java/com/example/invoicing/billingrestriction/BillingRestrictionRepository.java`

```java
public interface BillingRestrictionRepository
        extends JpaRepository<BillingRestriction, Long> {

    /** Used by InvoiceRunService to find services that must be invoiced immediately. */
    List<BillingRestriction> findByBillingTypeAndActiveTrue(BillingType billingType);

    /** Used by InvoiceRunService to load all active filter configurations for a run. */
    List<BillingRestriction> findByActiveTrue();

    /** Find all restrictions that apply to a specific service type. */
    List<BillingRestriction> findByServiceTypeAndActiveTrue(String serviceType);
}
```

---

### 24.4 BillingRestrictionService

**File:** `invoicing/src/main/java/com/example/invoicing/billingrestriction/BillingRestrictionService.java`

| Method Signature | Description |
|---|---|
| `List<BillingRestrictionResponse> findAll()` | Returns all restrictions ordered by `billingType`, then `serviceType` |
| `BillingRestrictionResponse findById(Long id)` | Returns one restriction or throws `ResourceNotFoundException` |
| `BillingRestrictionResponse create(BillingRestrictionRequest request)` | Creates and persists a new restriction |
| `BillingRestrictionResponse update(Long id, BillingRestrictionRequest request)` | Updates all fields |
| `void delete(Long id)` | Soft-delete: sets `active = false` |
| `boolean isImmediateService(String serviceType)` | Returns `true` if any active IMMEDIATE restriction exists for this service type — used by `InvoiceRunService` when categorising incoming events |
| `List<BillingRestriction> getActiveFilterRestrictions()` | Returns all active restrictions for use as InvoiceRun filter configuration |

---

### 24.5 BillingRestrictionController

**File:** `invoicing/src/main/java/com/example/invoicing/billingrestriction/BillingRestrictionController.java`

Base path: `/api/v1/billing-restrictions`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/billing-restrictions` | List all billing restrictions |
| GET | `/api/v1/billing-restrictions/{id}` | Get single restriction by ID |
| POST | `/api/v1/billing-restrictions` | Create new billing restriction |
| PUT | `/api/v1/billing-restrictions/{id}` | Update existing restriction |
| DELETE | `/api/v1/billing-restrictions/{id}` | Soft-delete (sets active = false) |

**POST /api/v1/billing-restrictions — Request Body (IMMEDIATE service type override):**
```json
{
  "serviceType": "SEPTIC_TANK_EMPTYING",
  "billingType": "IMMEDIATE",
  "municipality": null,
  "customerType": null,
  "locationId": null,
  "minAmount": null,
  "period": null,
  "serviceResponsibility": null,
  "description": "Septic tank emptying always invoiced immediately"
}
```

**POST /api/v1/billing-restrictions — Request Body (run filter):**
```json
{
  "serviceType": null,
  "billingType": "CYCLE_BASED",
  "municipality": "Helsinki",
  "customerType": "RESIDENTIAL",
  "locationId": null,
  "minAmount": 10.00,
  "period": "2025-01",
  "serviceResponsibility": "PUBLIC_LAW",
  "description": "January 2025 run — Helsinki residential public law events, min 10 EUR"
}
```

**POST /api/v1/billing-restrictions — Response Body (201 Created):**
```json
{
  "id": 5,
  "serviceType": "SEPTIC_TANK_EMPTYING",
  "billingType": "IMMEDIATE",
  "municipality": null,
  "customerType": null,
  "locationId": null,
  "minAmount": null,
  "period": null,
  "serviceResponsibility": null,
  "description": "Septic tank emptying always invoiced immediately",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2025-01-10T09:00:00Z"
}
```

---

## Frontend

### 24.6 Billing Restrictions Page

**File:** `invoicing-fe/src/pages/billing/BillingRestrictionsPage.jsx`

**Components:**
- `BillingRestrictionsTable` — columns: Service Type, Billing Type (badge: IMMEDIATE in orange, CYCLE_BASED in grey), Municipality, Customer Type, Min Amount, Period, Service Responsibility, Active, Actions (Edit / Delete)
- `BillingRestrictionModal` — Add/Edit form with fields:
  - Service Type (text, optional)
  - Billing Type (select: IMMEDIATE / CYCLE_BASED)
  - Municipality (text, optional)
  - Customer Type (select: RESIDENTIAL / BUSINESS / MUNICIPAL, optional)
  - Location ID (number, optional)
  - Min Amount (decimal, optional)
  - Period (text, e.g. "2025-Q1", optional)
  - Service Responsibility (select: PUBLIC_LAW / PRIVATE_LAW, optional)
  - Description (text, optional)
  - Active (checkbox)

**API calls via `src/api/billingRestrictions.js`:**
```js
export const getBillingRestrictions = () =>
  axios.get('/api/v1/billing-restrictions')

export const getBillingRestrictionById = (id) =>
  axios.get(`/api/v1/billing-restrictions/${id}`)

export const createBillingRestriction = (data) =>
  axios.post('/api/v1/billing-restrictions', data)

export const updateBillingRestriction = (id, data) =>
  axios.put(`/api/v1/billing-restrictions/${id}`, data)

export const deleteBillingRestriction = (id) =>
  axios.delete(`/api/v1/billing-restrictions/${id}`)
```

---

## Verification Checklist

1. Start the application — Hibernate creates `billing_restrictions` table with all filter columns and `billing_type` enum column.
2. `POST /api/v1/billing-restrictions` with `serviceType="SEPTIC_TANK_EMPTYING"`, `billingType="IMMEDIATE"` — returns 201.
3. `POST /api/v1/billing-restrictions` with `billingType="CYCLE_BASED"`, `municipality="Helsinki"`, `minAmount=10.00` — returns 201.
4. `GET /api/v1/billing-restrictions` — returns both records.
5. Call `billingRestrictionService.isImmediateService("SEPTIC_TANK_EMPTYING")` — returns `true`.
6. Call `billingRestrictionService.isImmediateService("CONTAINER_EMPTYING")` — returns `false` (no IMMEDIATE restriction exists for this type).
7. `DELETE /api/v1/billing-restrictions/{id}` for the IMMEDIATE restriction — soft-deletes it (active=false). Call `isImmediateService("SEPTIC_TANK_EMPTYING")` again — now returns `false`.
8. `PUT /api/v1/billing-restrictions/{id}` — update `minAmount` from 10.00 to 25.00, confirm change persists.
9. Open the Billing Restrictions page in the FE — both records appear in the table, IMMEDIATE badge is orange.
10. Add a new restriction via the modal — appears in table immediately after save.
11. Delete a restriction via the delete button — record disappears from table (soft-deleted, still in DB with `active=false`).
12. Verify that when `InvoiceRunService` processes a billing run, events with a service type matching an IMMEDIATE restriction are separated into their own immediate invoices and are not grouped with cycle-based events.

---

## File Checklist

### Backend
- [ ] `billingrestriction/BillingType.java` (enum)
- [ ] `billingrestriction/BillingRestriction.java`
- [ ] `billingrestriction/BillingRestrictionRepository.java`
- [ ] `billingrestriction/BillingRestrictionService.java`
- [ ] `billingrestriction/BillingRestrictionController.java`
- [ ] `billingrestriction/dto/BillingRestrictionRequest.java`
- [ ] `billingrestriction/dto/BillingRestrictionResponse.java`

### Frontend
- [ ] `src/api/billingRestrictions.js`
- [ ] `src/pages/billing/BillingRestrictionsPage.jsx`
- [ ] `src/components/billingrestriction/BillingRestrictionsTable.jsx`
- [ ] `src/components/billingrestriction/BillingRestrictionModal.jsx`
