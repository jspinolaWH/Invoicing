# Step 25 — Bundling Rules

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BundlingRule — "BundlingRule: per customer, maps a product group code to either SINGLE_LINE (aggregate all into one) or SEPARATE (each event on its own line). Applied at invoice generation time. Never affects the underlying BillingEvent records."
- `Docs/structured_breakdown/03-business-logic.md` → Area 2: Invoice Generation — "InvoiceBundlingService: Reads the customer's BundlingRules. Groups events by the rule that matches their product group. For SINGLE_LINE: aggregates quantities and amounts. For SEPARATE: each event becomes its own line item."
- `Docs/structured_breakdown/04-api-layer.md` → Section: Bundling Rules — "GET /customers/{id}/bundling-rules, PUT /customers/{id}/bundling-rules"

---

## Goal

`BundlingRule` controls how a customer's billing events are presented on their invoice. For a customer who empties bins 30 times a month, consolidating all 30 events into a single invoice line (SINGLE_LINE) is cleaner than listing each event separately. For premium services, each event might need to be itemised (SEPARATE).

The rules are per-customer and per-product-group. The entire rule set for a customer is replaced atomically with a single PUT — partial updates are not supported. `InvoiceBundlingService` reads these rules at invoice generation time; the underlying `BillingEvent` records are never modified regardless of the bundling setting.

---

## Backend

### 25.1 BundlingType Enum

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingType.java`

```java
public enum BundlingType {
    /** Aggregate all events for this product group into one invoice line. */
    SINGLE_LINE,
    /** Each event for this product group gets its own separate invoice line. */
    SEPARATE
}
```

---

### 25.2 BundlingRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRule.java`

> **Requirement source:** `01-domain-model.md` — "PD-290: For one customer, all waste bin emptyings are wanted on the same line, basic payments on their own line and all waste pallet imports on their own separate lines."

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited) |
| `customerNumber` | `String` | `@Column(nullable = false, length = 20)` — the customer this rule belongs to |
| `productGroup` | `String` | `@Column(nullable = false, length = 100)` — the product group code this rule governs (e.g. `"CONTAINER_EMPTYING"`, `"BASE_FEES"`, `"PALLET_DELIVERY"`) |
| `bundlingType` | `BundlingType` | `@Enumerated(EnumType.STRING) @Column(nullable = false)` |
| `description` | `String` | `@Column(length = 255)` nullable — admin note |

Unique constraint on (`customerNumber`, `productGroup`) — one rule per product group per customer.

```java
@Entity
@Table(
    name = "bundling_rules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_number", "product_group"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundlingRule extends BaseAuditEntity {

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @Column(nullable = false, length = 100)
    private String productGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BundlingType bundlingType;

    @Column(length = 255)
    private String description;
}
```

---

### 25.3 BundlingRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleRepository.java`

```java
public interface BundlingRuleRepository
        extends JpaRepository<BundlingRule, Long> {

    /** Returns all rules for a customer — used by InvoiceBundlingService at generation time. */
    List<BundlingRule> findByCustomerNumber(String customerNumber);

    /** Delete all rules for a customer — called before replacing the full rule set via PUT. */
    @Modifying
    @Transactional
    void deleteByCustomerNumber(String customerNumber);

    /** Find the rule for a specific product group for a customer. */
    Optional<BundlingRule> findByCustomerNumberAndProductGroup(
            String customerNumber,
            String productGroup
    );
}
```

---

### 25.4 BundlingRuleService

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleService.java`

| Method Signature | Description |
|---|---|
| `List<BundlingRuleResponse> findByCustomer(String customerNumber)` | Returns all rules for the specified customer |
| `List<BundlingRuleResponse> replaceAll(String customerNumber, List<BundlingRuleRequest> rules)` | Deletes all existing rules for the customer, then inserts the full new list in a single transaction. This is the only write operation — no partial update. |
| `BundlingType resolveForProductGroup(String customerNumber, String productGroup)` | Returns the `BundlingType` for a specific product group. If no rule exists for this product group, returns `SEPARATE` as the default (one event per line). |

**Replace-all transaction:**

```java
@Transactional
public List<BundlingRuleResponse> replaceAll(String customerNumber,
                                              List<BundlingRuleRequest> requests) {
    repository.deleteByCustomerNumber(customerNumber);
    List<BundlingRule> newRules = requests.stream()
        .map(r -> BundlingRule.builder()
            .customerNumber(customerNumber)
            .productGroup(r.getProductGroup())
            .bundlingType(r.getBundlingType())
            .description(r.getDescription())
            .build())
        .collect(Collectors.toList());
    return repository.saveAll(newRules)
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
}
```

---

### 25.5 BundlingRuleController

**File:** `invoicing/src/main/java/com/example/invoicing/bundling/BundlingRuleController.java`

Base path: `/api/v1/customers/{customerNumber}/bundling-rules`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/customers/{customerNumber}/bundling-rules` | Returns all bundling rules for a customer |
| PUT | `/api/v1/customers/{customerNumber}/bundling-rules` | Replaces all bundling rules for a customer atomically |

**GET /api/v1/customers/{customerNumber}/bundling-rules — Response Body (200 OK):**
```json
[
  {
    "id": 1,
    "customerNumber": "123456",
    "productGroup": "CONTAINER_EMPTYING",
    "bundlingType": "SINGLE_LINE",
    "description": "Aggregate all bin emptyings"
  },
  {
    "id": 2,
    "customerNumber": "123456",
    "productGroup": "BASE_FEES",
    "bundlingType": "SINGLE_LINE",
    "description": "Single line for base service fees"
  },
  {
    "id": 3,
    "customerNumber": "123456",
    "productGroup": "PALLET_DELIVERY",
    "bundlingType": "SEPARATE",
    "description": "Each pallet delivery on its own line"
  }
]
```

**PUT /api/v1/customers/{customerNumber}/bundling-rules — Request Body:**
```json
[
  {
    "productGroup": "CONTAINER_EMPTYING",
    "bundlingType": "SINGLE_LINE",
    "description": "Aggregate all bin emptyings"
  },
  {
    "productGroup": "BASE_FEES",
    "bundlingType": "SINGLE_LINE",
    "description": "Single line for base service fees"
  },
  {
    "productGroup": "PALLET_DELIVERY",
    "bundlingType": "SEPARATE",
    "description": "Each pallet delivery on its own line"
  }
]
```

**PUT Response Body (200 OK):** Same structure as GET response, with newly assigned IDs.

---

## Frontend

### 25.6 Bundling Rules Page (per Customer)

**File:** `invoicing-fe/src/pages/customers/BundlingRulesPage.jsx`

This page is accessed from the customer profile page, not from the main nav sidebar. The customer number is taken from the route parameter.

**Route:** `/customers/:customerNumber/bundling-rules`

**Components:**
- `BundlingRulesTable` — columns: Product Group, Bundling Type (toggle: SINGLE_LINE / SEPARATE), Description
- Each row's bundling type is a toggle button — clicking it flips the value locally
- "Save Changes" button at the bottom — submits the full modified list via `PUT /api/v1/customers/{customerNumber}/bundling-rules`
- "Add Product Group" button — opens a small inline form to add a new row (product group text input + bundling type selector)
- "Remove" button on each row — removes the row from the local list (takes effect on Save)

**Design note:** The page works like a spreadsheet editor — the user makes all changes locally and submits once. This matches the PUT-replaces-all API contract.

**API calls via `src/api/bundlingRules.js`:**
```js
export const getBundlingRules = (customerNumber) =>
  axios.get(`/api/v1/customers/${customerNumber}/bundling-rules`)

export const replaceBundlingRules = (customerNumber, rules) =>
  axios.put(`/api/v1/customers/${customerNumber}/bundling-rules`, rules)
```

---

## Verification Checklist

1. Start the application — Hibernate creates `bundling_rules` table with `customer_number`, `product_group`, `bundling_type` columns and a unique constraint on `(customer_number, product_group)`.
2. `PUT /api/v1/customers/123456/bundling-rules` with the three-item example above — returns 200 with all three rules and assigned IDs.
3. `GET /api/v1/customers/123456/bundling-rules` — returns the same three rules.
4. `PUT /api/v1/customers/123456/bundling-rules` with only one item (`CONTAINER_EMPTYING` as `SEPARATE`) — GET afterwards returns only one rule; the other two have been deleted.
5. Attempt to `PUT` two rules for the same customer with the same `productGroup` value — confirm 400 Bad Request (unique constraint violation) is returned with a clear message.
6. Call `bundlingRuleService.resolveForProductGroup("123456", "PALLET_DELIVERY")` when a SEPARATE rule exists — returns `SEPARATE`.
7. Call `resolveForProductGroup("123456", "UNKNOWN_GROUP")` when no rule exists for that group — returns `SEPARATE` (default fallback).
8. Run `InvoiceBundlingService` for customer 123456 with 5 CONTAINER_EMPTYING events — confirm the result is 1 aggregated line item with summed quantities and amounts.
9. Run `InvoiceBundlingService` for the same customer with 3 PALLET_DELIVERY events — confirm the result is 3 separate line items.
10. Open the Bundling Rules page in the FE for customer 123456 — table shows all three product groups with toggle buttons.
11. Click the SINGLE_LINE toggle on PALLET_DELIVERY to flip it to SEPARATE — confirm the UI updates optimistically.
12. Click "Save Changes" — `PUT` is called, response confirms the change; reload the page and confirm the new value persists.
13. Add a new product group row, enter "SPECIALIST_WORK" as SEPARATE, click Save — appears in list after save.

---

## File Checklist

### Backend
- [ ] `bundling/BundlingType.java` (enum)
- [ ] `bundling/BundlingRule.java`
- [ ] `bundling/BundlingRuleRepository.java`
- [ ] `bundling/BundlingRuleService.java`
- [ ] `bundling/BundlingRuleController.java`
- [ ] `bundling/dto/BundlingRuleRequest.java`
- [ ] `bundling/dto/BundlingRuleResponse.java`

### Frontend
- [ ] `src/api/bundlingRules.js`
- [ ] `src/pages/customers/BundlingRulesPage.jsx`
- [ ] `src/components/bundling/BundlingRulesTable.jsx`
