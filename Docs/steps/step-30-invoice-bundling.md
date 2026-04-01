# Step 30 — Invoice Bundling Service

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BundlingRule, InvoiceLineItem
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceBundlingService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Bundling Rules (GET/PUT `/customers/{id}/bundling-rules`)
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: "Simulation Mode Guard"

---

## Goal
Implement `InvoiceBundlingService`, which converts a flat list of `BillingEvent` records into structured `InvoiceLineItem` records according to per-customer `BundlingRule` configuration. The underlying events are never modified — only their presentation on the invoice changes. This service is called internally by `InvoiceGenerationService` (step 34); no new public endpoint is created here.

---

## Backend

### 30.1 BundlingRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRule.java`

> **Requirement source:** `01-domain-model.md` — BundlingRule entity

```java
@Entity
@Table(name = "bundling_rules")
public class BundlingRule extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "product_group_code", nullable = false, length = 100)
    private String productGroupCode;       // matches Product.productGroupCode

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private BundlingRuleType ruleType;     // SINGLE_LINE or SEPARATE

    @Column(name = "description", length = 255)
    private String description;
}
```

**Enum `BundlingRuleType`:** `SINGLE_LINE`, `SEPARATE`

- `SINGLE_LINE` — all events for this product group become one aggregated line (sum quantities and amounts).
- `SEPARATE` — each event becomes its own line item.
- No matching rule → default behaviour: one line per event (same as SEPARATE).

---

### 30.2 BundlingRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleRepository.java`

```java
public interface BundlingRuleRepository extends JpaRepository<BundlingRule, Long> {

    List<BundlingRule> findByCustomerId(Long customerId);

    Optional<BundlingRule> findByCustomerIdAndProductGroupCode(Long customerId, String productGroupCode);
}
```

---

### 30.3 InvoiceBundlingService

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/InvoiceBundlingService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceBundlingService

**Algorithm:**

1. Load all `BundlingRule` records for the customer.
2. Group the incoming `BillingEvent` list by `product.productGroupCode`.
3. For each product group:
   - Find matching `BundlingRule` for this customer + group code.
   - If `ruleType == SINGLE_LINE` (or rule matches any aggregate condition): collapse all events in this group into **one** `InvoiceLineItem`. Sum `quantity` values; sum `netAmount` values; sum `grossAmount` values. Set `bundled = true`. Use `quantity.setScale(4, ROUND_HALF_UP)` and `netAmount.setScale(4, ROUND_HALF_UP)` throughout.
   - If `ruleType == SEPARATE` (or no rule found): create one `InvoiceLineItem` per `BillingEvent`. Set `bundled = false`.
4. Handle 1-to-1 case (single event, no rule): produce one line item, `bundled = false`.
5. Preserve `legalClassification`, `accountingAccount`, and `costCenter` from the source event(s). For SINGLE_LINE bundling, all events in the group must share the same classification; if they differ, produce separate lines grouped by classification to avoid mixing PUBLIC_LAW and PRIVATE_LAW on one line.
6. Return the ordered `List<InvoiceLineItem>` (not yet persisted — the caller persists them).

**Method signatures:**

```java
@Service
public class InvoiceBundlingService {

    // Primary method — called by InvoiceGenerationService
    public List<InvoiceLineItem> bundle(List<BillingEvent> events, Long customerId);

    // Used by invoice preview endpoint
    public List<InvoiceLineItem> bundleForPreview(List<BillingEvent> events, Long customerId);
}
```

`bundleForPreview` is identical to `bundle` but returns detached (non-persisted) line items with `id = null`.

---

### 30.4 BundlingRuleService

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleService.java`

```java
public interface methods:
    List<BundlingRuleResponse> getRulesForCustomer(Long customerId);
    List<BundlingRuleResponse> replaceRulesForCustomer(Long customerId, List<BundlingRuleRequest> rules);
```

`replaceRulesForCustomer` deletes all existing rules for the customer and inserts the new set in a single transaction. This matches the PUT semantics of the endpoint.

---

### 30.5 BundlingRuleController

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleController.java`

Base path: `/api/v1/customers/{customerId}/bundling-rules`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/customers/{customerId}/bundling-rules` | Get all bundling rules for a customer | INVOICING_USER |
| PUT | `/api/v1/customers/{customerId}/bundling-rules` | Replace all bundling rules for a customer | INVOICING_USER |

**GET response:**
```json
[
  {
    "id": 101,
    "productGroupCode": "BIN_EMPTYING",
    "ruleType": "SINGLE_LINE",
    "description": "All bin emptyings on one line"
  },
  {
    "id": 102,
    "productGroupCode": "PALLET_IMPORT",
    "ruleType": "SEPARATE",
    "description": "Each pallet import on its own line"
  }
]
```

**PUT request body:**
```json
[
  {
    "productGroupCode": "BIN_EMPTYING",
    "ruleType": "SINGLE_LINE",
    "description": "All bin emptyings on one line"
  },
  {
    "productGroupCode": "BASE_FEE",
    "ruleType": "SINGLE_LINE",
    "description": "Base fees aggregated"
  }
]
```

---

## Frontend

### 30.6 Bundling Rules Configuration Page

**File:** `invoicing-fe/src/pages/customers/BundlingRulesPage.jsx`

Components:
- **BundlingRulesTable** — columns: Product Group Code, Rule Type (SINGLE_LINE / SEPARATE), Description, Actions (Edit / Delete).
- **BundlingRuleForm** — modal or inline form: product group code (text input), rule type (select dropdown: "Aggregate to one line" / "One line per event"), description (optional text).
- **SaveRulesButton** — triggers PUT to replace the full rule set. Shows confirmation: "Replacing all bundling rules — are you sure?"

**Invoice preview bundled line items display** (`InvoiceLineItemsTable.jsx` from step 29):
- Rows with `bundled: true` show a "BUNDLED" badge and a tooltip listing the count of source events (if available from the preview payload).

**API calls via `src/api/bundlingRules.js`:**
```js
export const getBundlingRules = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/bundling-rules`)

export const replaceBundlingRules = (customerId, rules) =>
  axios.put(`/api/v1/customers/${customerId}/bundling-rules`, rules)
```

---

## Verification Checklist

1. `POST` a `BundlingRule` via the PUT endpoint for customer 1: `BIN_EMPTYING → SINGLE_LINE`, `PALLET_IMPORT → SEPARATE`.
2. Call `GET /api/v1/customers/1/bundling-rules` — returns two rules.
3. Build a test list of 3 `BIN_EMPTYING` events and 2 `PALLET_IMPORT` events. Call `InvoiceBundlingService.bundle()` — expect 1 bundled `BIN_EMPTYING` line item (quantity = sum) and 2 separate `PALLET_IMPORT` line items.
4. Verify the `BIN_EMPTYING` line item has `bundled = true` and `netAmount` equals the sum of the three events' net amounts.
5. Verify the `PALLET_IMPORT` line items have `bundled = false`.
6. Test with no bundling rule: 2 events with no matching rule → expect 2 separate line items.
7. Test cross-classification protection: 2 `BIN_EMPTYING` events with different `legalClassification` values under `SINGLE_LINE` rule → expect 2 separate lines grouped by classification.
8. `BigDecimal` arithmetic: verify no floating-point rounding errors in summed quantities and amounts; all values use scale 4 with `ROUND_HALF_UP`.
9. Open `BundlingRulesPage` in the FE — table renders existing rules; add a new rule via form and save → re-fetch shows updated rules.
10. In the invoice preview (step 34), bundled line items show the "BUNDLED" badge.

---

## File Checklist

### Backend
- [ ] `bundling/BundlingRule.java`
- [ ] `bundling/BundlingRuleType.java` (enum)
- [ ] `bundling/BundlingRuleRepository.java`
- [ ] `bundling/BundlingRuleService.java`
- [ ] `bundling/InvoiceBundlingService.java`
- [ ] `bundling/BundlingRuleController.java`
- [ ] `bundling/dto/BundlingRuleRequest.java`
- [ ] `bundling/dto/BundlingRuleResponse.java`

### Frontend
- [ ] `src/pages/customers/BundlingRulesPage.jsx`
- [ ] `src/pages/customers/components/BundlingRulesTable.jsx`
- [ ] `src/pages/customers/components/BundlingRuleForm.jsx`
- [ ] `src/api/bundlingRules.js`
