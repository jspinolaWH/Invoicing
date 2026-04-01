# Step 27 — Minimum Fee Config

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: MinimumFeeConfig — "PD-286: The system ensures that the minimum charge complies with the contract dates, preventing overbilling in cases where customer contracts start or end mid-season."
- `Docs/structured_breakdown/01-domain-model.md` → Section: MinimumFeeConfig — "PD-286 (PJH): The total amount used for comparison must be VAT 0% (net invoicing). If the previous owner has not met the minimum charge requirement by November, the system adjusts the final invoice accordingly."
- `Docs/structured_breakdown/03-business-logic.md` → Area 2: Invoice Generation — "MinimumFeeService: Called by InvoiceGenerationService after all line items are assembled. Computes the net (VAT 0%) total. If the net total is below the minimum: checks the contract period — if the contract started after the billing period start, or ended before the billing period end, the minimum does NOT apply."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Minimum Fee — "GET /minimum-fee-configs / POST / PUT /{id} — CRUD for minimum fee configurations per customer or globally."

---

## Goal

`MinimumFeeConfig` defines the minimum billable amount for a customer type over a billing period. If a customer's total billed services for the period fall below this threshold, `MinimumFeeService` adds an adjustment line item to make up the difference.

The comparison always uses the **net (VAT 0%) total** — PJH explicitly requires this. Contract boundary awareness is critical: if a customer's contract started mid-period, the minimum does not apply to them for that partial period. Charging a new contract-holder the full annual minimum when they only joined in November would be overbilling.

`MinimumFeeService` is purely internal — it is called by `InvoiceGenerationService` as the final step before assigning an invoice number.

---

## Backend

### 27.1 PeriodType Enum

**File:** `invoicing/src/main/java/com/example/invoicing/minimumfee/PeriodType.java`

```java
public enum PeriodType {
    ANNUAL,
    QUARTERLY
}
```

---

### 27.2 MinimumFeeConfig Entity

**File:** `invoicing/src/main/java/com/example/invoicing/minimumfee/MinimumFeeConfig.java`

> **Requirement source:** `01-domain-model.md` — "MinimumFeeConfig: holds the minimum net amount and the period type (ANNUAL / QUARTERLY). The comparison uses the net (VAT 0%) total of the billing period. If the contract started after the billing period start, or ended before the billing period end, the minimum does NOT apply."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `customerType` | `String` | `@Column(nullable = false, length = 50)` — e.g. `"RESIDENTIAL"`, `"BUSINESS"`, `"MUNICIPAL"` |
| `netAmountThreshold` | `BigDecimal` | `@Column(nullable = false, precision = 10, scale = 2)` — the minimum net amount in euros |
| `periodType` | `PeriodType` | `@Enumerated(EnumType.STRING) @Column(nullable = false)` |
| `contractStartAdjustment` | `boolean` | `@Column(nullable = false)` default `true` — when true, minimum is not applied if the contract started after the period start |
| `contractEndAdjustment` | `boolean` | `@Column(nullable = false)` default `true` — when true, minimum is not applied if the contract ended before the period end |
| `adjustmentProductCode` | `String` | `@Column(nullable = false, length = 50)` — the product code used for the adjustment line item added when the minimum is not met |
| `description` | `String` | `@Column(length = 255)` nullable — admin note, e.g. "Annual minimum for residential customers" |
| `active` | `boolean` | `@Column(nullable = false)` default `true` |

```java
@Entity
@Table(name = "minimum_fee_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinimumFeeConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 50)
    private String customerType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmountThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private boolean contractStartAdjustment = true;

    @Column(nullable = false)
    private boolean contractEndAdjustment = true;

    @Column(nullable = false, length = 50)
    private String adjustmentProductCode;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
```

---

### 27.3 MinimumFeeConfigRepository

**File:** `invoicing/src/main/java/com/example/invoicing/minimumfee/MinimumFeeConfigRepository.java`

```java
public interface MinimumFeeConfigRepository
        extends JpaRepository<MinimumFeeConfig, Long> {

    /** Used by MinimumFeeService to find the applicable config for a customer type. */
    Optional<MinimumFeeConfig> findByCustomerTypeAndPeriodTypeAndActiveTrue(
            String customerType,
            PeriodType periodType
    );

    List<MinimumFeeConfig> findAllByActiveTrue();
}
```

---

### 27.4 MinimumFeeService

**File:** `invoicing/src/main/java/com/example/invoicing/minimumfee/MinimumFeeService.java`

> **Requirement source:** `03-business-logic.md` Area 2 — "MinimumFeeService: Computes the net (VAT 0%) total. Reads the MinimumFeeConfig. If the net total is below the minimum: checks the contract period — if the contract started after the billing period start, or ended before the billing period end, the minimum does NOT apply. If the minimum applies, adds an adjustment line item for the difference."

| Method Signature | Description |
|---|---|
| `Optional<InvoiceLineItem> applyMinimumFee(Invoice invoice, String customerType, LocalDate periodStart, LocalDate periodEnd, LocalDate contractStart, LocalDate contractEnd)` | Main entry point. Returns an adjustment `InvoiceLineItem` if the minimum applies and is not met, otherwise `Optional.empty()`. |
| `BigDecimal computeNetTotal(Invoice invoice)` | Sums `amountNet` across all non-surcharge, non-adjustment line items. Uses `BigDecimal` arithmetic with scale 2. |
| `boolean isMinimumApplicable(MinimumFeeConfig config, LocalDate periodStart, LocalDate periodEnd, LocalDate contractStart, LocalDate contractEnd)` | Returns `false` if `contractStartAdjustment=true` AND `contractStart > periodStart`, or if `contractEndAdjustment=true` AND `contractEnd < periodEnd`. Otherwise returns `true`. |

**Core logic:**

```java
public Optional<InvoiceLineItem> applyMinimumFee(
        Invoice invoice,
        String customerType,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate contractStart,
        LocalDate contractEnd) {

    Optional<MinimumFeeConfig> configOpt = repository
        .findByCustomerTypeAndPeriodTypeAndActiveTrue(customerType, determinePeriodType(periodStart, periodEnd));

    if (configOpt.isEmpty()) {
        return Optional.empty();  // no minimum fee configured for this customer type
    }

    MinimumFeeConfig config = configOpt.get();

    if (!isMinimumApplicable(config, periodStart, periodEnd, contractStart, contractEnd)) {
        return Optional.empty();  // contract boundaries exempt this customer
    }

    BigDecimal netTotal = computeNetTotal(invoice);
    BigDecimal threshold = config.getNetAmountThreshold();

    if (netTotal.compareTo(threshold) >= 0) {
        return Optional.empty();  // already meets the minimum
    }

    BigDecimal adjustmentAmount = threshold.subtract(netTotal);
    InvoiceLineItem adjustmentLine = buildAdjustmentLineItem(
        config.getAdjustmentProductCode(), adjustmentAmount);
    return Optional.of(adjustmentLine);
}
```

**Integration with `InvoiceGenerationService`:**

```java
// Step 10 in the generation pipeline (after surcharge, before number assignment):
Optional<InvoiceLineItem> minFeeAdjustment = minimumFeeService.applyMinimumFee(
    invoice,
    customer.getCustomerType(),
    run.getPeriodStart(),
    run.getPeriodEnd(),
    contract.getStartDate(),
    contract.getEndDate()
);
minFeeAdjustment.ifPresent(invoice::addLineItem);
```

---

### 27.5 MinimumFeeConfigController

**File:** `invoicing/src/main/java/com/example/invoicing/minimumfee/MinimumFeeConfigController.java`

Base path: `/api/v1/minimum-fee-config`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/minimum-fee-config` | List all active minimum fee configs |
| GET | `/api/v1/minimum-fee-config/{id}` | Get single config by ID |
| POST | `/api/v1/minimum-fee-config` | Create new minimum fee config |
| PUT | `/api/v1/minimum-fee-config/{id}` | Update an existing config |
| DELETE | `/api/v1/minimum-fee-config/{id}` | Soft-delete (sets active = false) |

**POST /api/v1/minimum-fee-config — Request Body:**
```json
{
  "customerType": "RESIDENTIAL",
  "netAmountThreshold": 50.00,
  "periodType": "ANNUAL",
  "contractStartAdjustment": true,
  "contractEndAdjustment": true,
  "adjustmentProductCode": "MIN_FEE_ADJ",
  "description": "Annual minimum fee for residential customers"
}
```

**POST /api/v1/minimum-fee-config — Response Body (201 Created):**
```json
{
  "id": 3,
  "customerType": "RESIDENTIAL",
  "netAmountThreshold": 50.00,
  "periodType": "ANNUAL",
  "contractStartAdjustment": true,
  "contractEndAdjustment": true,
  "adjustmentProductCode": "MIN_FEE_ADJ",
  "description": "Annual minimum fee for residential customers",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2025-01-10T11:00:00Z"
}
```

**PUT /api/v1/minimum-fee-config/{id} — example (raise threshold):**
```json
{
  "netAmountThreshold": 60.00,
  "description": "Annual minimum fee for residential customers — updated for 2026"
}
```

---

## Frontend

### 27.6 Minimum Fee Config Page

**File:** `invoicing-fe/src/pages/masterdata/MinimumFeeConfigPage.jsx`

**Components:**
- `MinimumFeeConfigTable` — columns: Customer Type, Threshold (€), Period Type, Contract Start Adj. (yes/no), Contract End Adj. (yes/no), Adjustment Product Code, Active, Actions (Edit / Delete)
- `MinimumFeeConfigModal` — Add/Edit form with fields:
  - Customer Type (select: RESIDENTIAL / BUSINESS / MUNICIPAL)
  - Net Amount Threshold (decimal input, labelled "Minimum net amount (€ VAT 0%)")
  - Period Type (select: ANNUAL / QUARTERLY)
  - Contract Start Adjustment (checkbox — "Exempt if contract started after period start")
  - Contract End Adjustment (checkbox — "Exempt if contract ended before period end")
  - Adjustment Product Code (text — the product code to use for the make-up line item)
  - Description (text, optional)
  - Active (checkbox)

**API calls via `src/api/minimumFeeConfig.js`:**
```js
export const getMinimumFeeConfigs = () =>
  axios.get('/api/v1/minimum-fee-config')

export const createMinimumFeeConfig = (data) =>
  axios.post('/api/v1/minimum-fee-config', data)

export const updateMinimumFeeConfig = (id, data) =>
  axios.put(`/api/v1/minimum-fee-config/${id}`, data)

export const deleteMinimumFeeConfig = (id) =>
  axios.delete(`/api/v1/minimum-fee-config/${id}`)
```

---

## Verification Checklist

1. Start the application — Hibernate creates `minimum_fee_config` table with all columns.
2. `POST /api/v1/minimum-fee-config` with `customerType=RESIDENTIAL`, `netAmountThreshold=50.00`, `periodType=ANNUAL`, both adjustment flags `true` — returns 201.
3. Run `minimumFeeService.applyMinimumFee()` for a RESIDENTIAL customer whose invoice net total is €35.00, with contract covering the full period — returns an adjustment line item for €15.00 (`50.00 - 35.00`).
4. Run `applyMinimumFee()` for a RESIDENTIAL customer whose invoice net total is €65.00 — returns `Optional.empty()` (minimum already met).
5. Run `applyMinimumFee()` for a RESIDENTIAL customer with `contractStart = periodStart + 30 days` and `contractStartAdjustment = true` — returns `Optional.empty()` (contract started mid-period, minimum does not apply).
6. Run `applyMinimumFee()` same scenario but `contractStartAdjustment = false` — returns the adjustment line item (exemption is disabled).
7. Run `applyMinimumFee()` for `customerType=BUSINESS` when no config exists for BUSINESS — returns `Optional.empty()` (no config, no minimum).
8. `computeNetTotal()` correctly sums only non-surcharge, non-adjustment line items. Create an invoice with a €5.00 paper surcharge and a €100.00 service line — confirm `computeNetTotal = 100.00`, not `105.00`.
9. Open the Minimum Fee Config page in the FE — table displays all configs.
10. Add a new config via the modal — appears in table after save.
11. Edit the threshold from 50.00 to 60.00 — change persists after reload.
12. Delete a config (soft-delete) — disappears from the active list but remains in the DB.

---

## File Checklist

### Backend
- [ ] `minimumfee/PeriodType.java` (enum)
- [ ] `minimumfee/MinimumFeeConfig.java`
- [ ] `minimumfee/MinimumFeeConfigRepository.java`
- [ ] `minimumfee/MinimumFeeService.java`
- [ ] `minimumfee/MinimumFeeConfigController.java`
- [ ] `minimumfee/dto/MinimumFeeConfigRequest.java`
- [ ] `minimumfee/dto/MinimumFeeConfigResponse.java`
- [ ] `invoice/InvoiceGenerationService.java` (modification — call `MinimumFeeService.applyMinimumFee()` at step 10)

### Frontend
- [ ] `src/api/minimumFeeConfig.js`
- [ ] `src/pages/masterdata/MinimumFeeConfigPage.jsx`
- [ ] `src/components/minimumfee/MinimumFeeConfigTable.jsx`
- [ ] `src/components/minimumfee/MinimumFeeConfigModal.jsx`
