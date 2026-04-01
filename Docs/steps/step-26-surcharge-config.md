# Step 26 — Surcharge Config

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: Invoice — "PD-294: An invoicing surcharge must be added to an invoice depending on the invoicing method, e.g. a paper invoice surcharge, email invoice surcharge, direct payment surcharge."
- `Docs/structured_breakdown/03-business-logic.md` → Area 2: Invoice Generation — "BillingeSurchargeService: Checks: global toggle on? Does the customer's delivery method have a configured surcharge product? Has the surcharge been manually removed from this specific invoice? If all checks pass, adds the surcharge product as a line item."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoicing Surcharge Configuration — "GET /invoicing-surcharge-config / PUT — Manage the global surcharge configuration."
- `Docs/structured_breakdown/01-domain-model.md` → Section: Customer / BillingProfile — "BillingProfile records the delivery channel — and this is what drives automatic surcharge assignment (PD-294)."

---

## Goal

`SurchargeConfig` defines how much to add to an invoice based on the customer's invoice delivery method. Paper invoices cost the company more to produce and mail — those costs are passed to the customer as a surcharge. Email and direct payment methods have their own (typically lower) surcharge amounts.

`BillingSurchargeService` is the internal service that `InvoiceGenerationService` calls after all line items are assembled. It checks whether the global surcharge toggle is on, looks up the applicable amount for the customer's delivery method, and — unless the surcharge has been manually removed from this specific invoice — adds a surcharge line item.

The global toggle and the per-method amounts live in `SurchargeConfig`. There is one record per delivery method.

---

## Backend

### 26.1 DeliveryMethod Enum

**File:** `invoicing/src/main/java/com/example/invoicing/surcharge/DeliveryMethod.java`

> **Note:** This enum may already exist in the `billing` or `customer` package from an earlier step. If so, reuse it — do not duplicate it.

```java
public enum DeliveryMethod {
    E_INVOICE,
    EMAIL,
    PAPER,
    DIRECT_PAYMENT
}
```

---

### 26.2 SurchargeConfig Entity

**File:** `invoicing/src/main/java/com/example/invoicing/surcharge/SurchargeConfig.java`

> **Requirement source:** `01-domain-model.md` — "PD-294: A separate product is created for the invoicing surcharge. The surcharge is shown automatically whenever the customer is to receive a paper invoice."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `deliveryMethod` | `DeliveryMethod` | `@Enumerated(EnumType.STRING) @Column(nullable = false, unique = true)` — one record per method |
| `amount` | `BigDecimal` | `@Column(nullable = false, precision = 10, scale = 2)` — the surcharge amount in euros |
| `description` | `String` | `@Column(length = 255)` — e.g. "Paper invoice surcharge" |
| `active` | `boolean` | `@Column(nullable = false)` default `true` — when false, this delivery method has no surcharge |
| `globalSurchargeEnabled` | `boolean` | `@Column(nullable = false)` default `true` — the global toggle from PD-294; when false, NO surcharges are applied for any delivery method |

```java
@Entity
@Table(name = "surcharge_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurchargeConfig extends BaseAuditEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private DeliveryMethod deliveryMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean globalSurchargeEnabled = true;
}
```

**Note on `globalSurchargeEnabled`:** PD-294 says the surcharge must be globally toggleable on/off. Rather than a separate config table, this boolean is stored on every `SurchargeConfig` record. `BillingSurchargeService` reads any one record's `globalSurchargeEnabled` value — they must all be in sync. A convenience `PUT /api/v1/surcharge-config/global-toggle` endpoint updates all records at once.

---

### 26.3 SurchargeConfigRepository

**File:** `invoicing/src/main/java/com/example/invoicing/surcharge/SurchargeConfigRepository.java`

```java
public interface SurchargeConfigRepository
        extends JpaRepository<SurchargeConfig, Long> {

    /** Used by BillingSurchargeService to look up the surcharge for a specific delivery method. */
    Optional<SurchargeConfig> findByDeliveryMethodAndActiveTrue(DeliveryMethod deliveryMethod);

    /** Returns all records — used by the CRUD list endpoint and by the global toggle. */
    List<SurchargeConfig> findAll();

    /** Used by global toggle to update all records at once. */
    @Modifying
    @Transactional
    @Query("UPDATE SurchargeConfig s SET s.globalSurchargeEnabled = :enabled")
    void setGlobalSurchargeEnabled(@Param("enabled") boolean enabled);
}
```

---

### 26.4 BillingSurchargeService

**File:** `invoicing/src/main/java/com/example/invoicing/surcharge/BillingSurchargeService.java`

> **Requirement source:** `03-business-logic.md` Area 2 — "BillingeSurchargeService: Checks: global toggle on? Does the customer's delivery method have a configured surcharge product? Has the surcharge been manually removed from this specific invoice?"

| Method Signature | Description |
|---|---|
| `Optional<BigDecimal> resolveSurcharge(DeliveryMethod deliveryMethod)` | Main entry point used by `InvoiceGenerationService`. Returns the surcharge amount if the global toggle is on AND an active config exists for this delivery method. Returns `Optional.empty()` if no surcharge applies. |
| `boolean isGlobalSurchargeEnabled()` | Returns `true` if the global surcharge feature is on. Reads from any SurchargeConfig record. Defaults to `true` if no records exist yet. |
| `void setGlobalEnabled(boolean enabled)` | Toggles the surcharge on or off for all delivery methods at once. |

**Resolution logic:**

```java
public Optional<BigDecimal> resolveSurcharge(DeliveryMethod deliveryMethod) {
    if (!isGlobalSurchargeEnabled()) {
        return Optional.empty();
    }
    return repository.findByDeliveryMethodAndActiveTrue(deliveryMethod)
        .map(SurchargeConfig::getAmount);
}
```

`InvoiceGenerationService` calls this after bundling and before minimum fee:

```java
// Step 9 in InvoiceGenerationService pipeline:
DeliveryMethod delivery = customer.getBillingProfile().getDeliveryMethod();
Optional<BigDecimal> surcharge = billingSurchargeService.resolveSurcharge(delivery);
if (surcharge.isPresent() && !invoice.isSurchargeManuallyRemoved()) {
    invoice.addSurchargeLineItem(surchargeProduct, surcharge.get());
}
```

---

### 26.5 SurchargeConfigController

**File:** `invoicing/src/main/java/com/example/invoicing/surcharge/SurchargeConfigController.java`

Base path: `/api/v1/surcharge-config`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/surcharge-config` | List all surcharge config records (one per delivery method) |
| GET | `/api/v1/surcharge-config/{id}` | Get single config record by ID |
| POST | `/api/v1/surcharge-config` | Create a surcharge config for a new delivery method |
| PUT | `/api/v1/surcharge-config/{id}` | Update amount, description, or active flag for a delivery method |
| DELETE | `/api/v1/surcharge-config/{id}` | Soft-delete: sets `active = false` |
| PUT | `/api/v1/surcharge-config/global-toggle` | Toggle surcharge on/off globally |

**POST /api/v1/surcharge-config — Request Body:**
```json
{
  "deliveryMethod": "PAPER",
  "amount": 5.00,
  "description": "Paper invoice surcharge",
  "active": true
}
```

**POST /api/v1/surcharge-config — Response Body (201 Created):**
```json
{
  "id": 1,
  "deliveryMethod": "PAPER",
  "amount": 5.00,
  "description": "Paper invoice surcharge",
  "active": true,
  "globalSurchargeEnabled": true,
  "createdBy": "admin",
  "createdAt": "2025-01-10T10:00:00Z"
}
```

**PUT /api/v1/surcharge-config/global-toggle — Request Body:**
```json
{
  "enabled": false
}
```

**PUT /api/v1/surcharge-config/global-toggle — Response Body (200 OK):**
```json
{
  "globalSurchargeEnabled": false,
  "updatedRecords": 4
}
```

---

## Frontend

### 26.6 Surcharge Config Page

**File:** `invoicing-fe/src/pages/masterdata/SurchargeConfigPage.jsx`

**Components:**
- `GlobalSurchargeToggle` — a prominent on/off toggle at the top of the page with label "Invoice Surcharges". Calls `PUT /api/v1/surcharge-config/global-toggle`. When off, the entire table is greyed out with a banner "All invoice surcharges are currently disabled".
- `SurchargeConfigTable` — columns: Delivery Method, Amount (€), Description, Active, Actions (Edit / Delete)
- `SurchargeConfigModal` — Add/Edit form: delivery method (select), amount (decimal input), description (text), active (checkbox)

**API calls via `src/api/surchargeConfig.js`:**
```js
export const getSurchargeConfigs = () =>
  axios.get('/api/v1/surcharge-config')

export const createSurchargeConfig = (data) =>
  axios.post('/api/v1/surcharge-config', data)

export const updateSurchargeConfig = (id, data) =>
  axios.put(`/api/v1/surcharge-config/${id}`, data)

export const deleteSurchargeConfig = (id) =>
  axios.delete(`/api/v1/surcharge-config/${id}`)

export const setGlobalSurchargeEnabled = (enabled) =>
  axios.put('/api/v1/surcharge-config/global-toggle', { enabled })
```

---

## Verification Checklist

1. Start the application — Hibernate creates the `surcharge_config` table with `delivery_method` unique constraint.
2. `POST /api/v1/surcharge-config` with `deliveryMethod=PAPER`, `amount=5.00` — returns 201.
3. `POST /api/v1/surcharge-config` with `deliveryMethod=EMAIL`, `amount=2.00` — returns 201.
4. Attempt to `POST` a second PAPER record — confirm 400 or 409 due to unique constraint.
5. Call `billingSurchargeService.resolveSurcharge(PAPER)` — returns `Optional.of(5.00)`.
6. Call `billingSurchargeService.resolveSurcharge(E_INVOICE)` — returns `Optional.empty()` (no config record for E_INVOICE).
7. `PUT /api/v1/surcharge-config/global-toggle` with `{"enabled": false}` — all records updated; response shows `updatedRecords: 2`.
8. Call `billingSurchargeService.resolveSurcharge(PAPER)` again — returns `Optional.empty()` (global toggle is off).
9. Re-enable global toggle. Run invoice generation for a PAPER customer — confirm the surcharge line item is present on the generated invoice.
10. Call `POST /api/v1/invoices/{id}/remove-surcharge` (from Step 4 API layer) on that invoice. Run generation again — confirm `invoice.isSurchargeManuallyRemoved()` is true and the surcharge line is absent.
11. Open the Surcharge Config page in the FE — table shows PAPER and EMAIL entries.
12. Toggle global surcharge OFF — table greys out and banner appears.
13. Toggle back ON — table is active again.
14. Edit PAPER surcharge from 5.00 to 6.50 — change is saved and reflected in the table.

---

## File Checklist

### Backend
- [ ] `surcharge/DeliveryMethod.java` (enum — or reuse from existing package)
- [ ] `surcharge/SurchargeConfig.java`
- [ ] `surcharge/SurchargeConfigRepository.java`
- [ ] `surcharge/BillingSurchargeService.java`
- [ ] `surcharge/SurchargeConfigController.java`
- [ ] `surcharge/dto/SurchargeConfigRequest.java`
- [ ] `surcharge/dto/SurchargeConfigResponse.java`
- [ ] `surcharge/dto/GlobalToggleRequest.java`
- [ ] `surcharge/dto/GlobalToggleResponse.java`

### Frontend
- [ ] `src/api/surchargeConfig.js`
- [ ] `src/pages/masterdata/SurchargeConfigPage.jsx`
- [ ] `src/components/surcharge/SurchargeConfigTable.jsx`
- [ ] `src/components/surcharge/SurchargeConfigModal.jsx`
- [ ] `src/components/surcharge/GlobalSurchargeToggle.jsx`
