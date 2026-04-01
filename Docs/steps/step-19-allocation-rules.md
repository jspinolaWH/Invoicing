# Step 19 — Accounting Allocation Rules

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: AccountingAccount / CostCenter / VatRate — "The same product must be able to be routed to a different account and cost center based on e.g. price list or region"
- `Docs/structured_breakdown/02-data-layer.md` → Section: AccountingAllocationRuleRepository — "Query: productId = ? AND (region = ? OR region IS NULL) ORDER BY specificity DESC. Most-specific rule wins."
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data — "GET /allocation-rules / POST / PUT /{id} — CRUD for accounting allocation rules (product → account/cost centre mapping)"

---

## Goal

Define the configuration table that tells the system which `AccountingAccount` to use when a product is billed in a given region or municipality. A single product (e.g. "Container Emptying") may post to different ledger accounts depending on where the service was performed — municipal area A may use account 4001, while area B uses account 4002, and a catch-all fallback uses account 4000.

The `specificity` score is the tie-breaker: a rule with both a product AND a region set beats a rule that only specifies a product. This avoids a cascade of `if/else` in service code — the data layer does the ordering and the service takes the first match.

This entity is consumed directly by `AccountingAllocationService` (Step 20) and, transitively, by `InvoiceGenerationService` every time a FINVOICE line item needs a ledger account assignment.

---

## Backend

### 19.1 AccountingAllocationRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/allocation/AccountingAllocationRule.java`

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` (inherited from `BaseAuditEntity`) |
| `product` | `Product` | `@ManyToOne(optional = false) @JoinColumn(name = "product_id")` — the product this rule applies to |
| `region` | `String` | `@Column(length = 100)` nullable — ISO region code or municipality name; null means "any region" |
| `municipality` | `String` | `@Column(length = 100)` nullable — more granular than region; when both are set, both must match |
| `accountingAccount` | `AccountingAccount` | `@ManyToOne(optional = false) @JoinColumn(name = "accounting_account_id")` — the target ledger account |
| `specificityScore` | `Integer` | `@Column(nullable = false)` — caller sets this explicitly (0 = global fallback, 1 = product only, 2 = product+region, 3 = product+region+municipality) |
| `description` | `String` | `@Column(length = 255)` nullable — human-readable note for the admin UI |
| `active` | `boolean` | `@Column(nullable = false)` default `true` — inactive rules are excluded from all queries |

Inherits `createdBy`, `createdAt`, `lastModifiedBy`, `lastModifiedAt` from `BaseAuditEntity`.

```java
@Entity
@Table(name = "accounting_allocation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingAllocationRule extends BaseAuditEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String municipality;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id", nullable = false)
    private AccountingAccount accountingAccount;

    @Column(nullable = false)
    private Integer specificityScore;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
```

---

### 19.2 AccountingAllocationRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/allocation/AccountingAllocationRuleRepository.java`

> **Requirement source:** `02-data-layer.md` — AccountingAllocationRuleRepository: "Query: productId = ? AND (region = ? OR region IS NULL) ORDER BY specificity DESC. Most-specific rule wins — a rule with both productId and region beats one with only productId."

```java
public interface AccountingAllocationRuleRepository
        extends JpaRepository<AccountingAllocationRule, Long> {

    /**
     * Find the most specific active rule for a product/region/municipality combination.
     * ORDER BY specificityScore DESC ensures the best match is first.
     * Callers take the first element from this list.
     */
    @Query("""
        SELECT r FROM AccountingAllocationRule r
        WHERE r.product.id = :productId
          AND r.active = true
          AND (r.region IS NULL OR r.region = :region)
          AND (r.municipality IS NULL OR r.municipality = :municipality)
        ORDER BY r.specificityScore DESC
        """)
    List<AccountingAllocationRule> findMostSpecificRules(
            @Param("productId") Long productId,
            @Param("region") String region,
            @Param("municipality") String municipality
    );

    /** Used by the CRUD list endpoint — returns all rules, active and inactive. */
    List<AccountingAllocationRule> findAllByOrderBySpecificityScoreDesc();

    /** Returns all active rules for a given product (used in the FE test panel). */
    List<AccountingAllocationRule> findByProduct_IdAndActiveTrue(Long productId);
}
```

---

### 19.3 AccountingAllocationRuleService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/allocation/AccountingAllocationRuleService.java`

| Method Signature | Description |
|---|---|
| `List<AllocationRuleResponse> findAll()` | Returns all rules ordered by specificity desc |
| `AllocationRuleResponse findById(Long id)` | Returns one rule or throws `ResourceNotFoundException` |
| `AllocationRuleResponse create(AllocationRuleRequest request)` | Validates that the referenced product and account exist; calculates `specificityScore` automatically from which fields are set |
| `AllocationRuleResponse update(Long id, AllocationRuleRequest request)` | Recalculates `specificityScore` on save |
| `void delete(Long id)` | Soft-delete: sets `active = false` rather than hard delete to preserve history |
| `Optional<AccountingAllocationRule> findBestMatch(Long productId, String region, String municipality)` | Delegates to repository; returns first result from ordered list or empty if none |

The `specificityScore` is computed deterministically in the service, not provided by the caller:
- product only (region and municipality both null) → score = 1
- product + region → score = 2
- product + region + municipality → score = 3

---

### 19.4 AccountingAllocationRuleController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/allocation/AccountingAllocationRuleController.java`

Base path: `/api/v1/allocation-rules`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/allocation-rules` | List all allocation rules |
| GET | `/api/v1/allocation-rules/{id}` | Get single allocation rule by ID |
| POST | `/api/v1/allocation-rules` | Create new allocation rule |
| PUT | `/api/v1/allocation-rules/{id}` | Update existing allocation rule |
| DELETE | `/api/v1/allocation-rules/{id}` | Soft-delete (sets active = false) |
| GET | `/api/v1/allocation-rules/resolve?productId=&region=&municipality=` | Resolve the winning rule for a product/region combination (used by FE test panel) |

**POST /api/v1/allocation-rules — Request Body:**
```json
{
  "productId": 7,
  "region": "Uusimaa",
  "municipality": "Helsinki",
  "accountingAccountId": 3,
  "description": "Helsinki waste fee → account 4001"
}
```

**POST /api/v1/allocation-rules — Response Body (201 Created):**
```json
{
  "id": 12,
  "productId": 7,
  "productName": "Container Emptying",
  "region": "Uusimaa",
  "municipality": "Helsinki",
  "accountingAccountId": 3,
  "accountingAccountCode": "4001",
  "accountingAccountName": "Waste Treatment Revenue",
  "specificityScore": 3,
  "description": "Helsinki waste fee → account 4001",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2025-01-15T09:30:00Z"
}
```

**GET /api/v1/allocation-rules/resolve — Response Body (200 OK):**
```json
{
  "matchedRuleId": 12,
  "specificityScore": 3,
  "accountingAccountCode": "4001",
  "accountingAccountName": "Waste Treatment Revenue",
  "matchReason": "Matched on product + region + municipality"
}
```

---

## Frontend

### 19.5 Allocation Rules List Page

**File:** `invoicing-fe/src/pages/masterdata/AllocationRulesPage.jsx`

**Components:**
- `AllocationRulesTable` — columns: Product, Region, Municipality, Account Code, Account Name, Specificity Score, Status (Active/Inactive), Actions (Edit / Delete)
- `AllocationRuleModal` — Add/Edit form with fields: product selector (dropdown from `/api/v1/products`), region (text input), municipality (text input), accounting account selector (dropdown from `/api/v1/accounting-accounts`), description (text). Specificity score is computed and displayed read-only.
- `ResolveTestPanel` — at the bottom of the page: product selector + region + municipality inputs → "Test" button → calls `/api/v1/allocation-rules/resolve` → displays the winning rule and matched account

**API calls via `src/api/allocationRules.js`:**
```js
export const getAllocationRules = () =>
  axios.get('/api/v1/allocation-rules')

export const getAllocationRuleById = (id) =>
  axios.get(`/api/v1/allocation-rules/${id}`)

export const createAllocationRule = (data) =>
  axios.post('/api/v1/allocation-rules', data)

export const updateAllocationRule = (id, data) =>
  axios.put(`/api/v1/allocation-rules/${id}`, data)

export const deleteAllocationRule = (id) =>
  axios.delete(`/api/v1/allocation-rules/${id}`)

export const resolveAllocationRule = (productId, region, municipality) =>
  axios.get('/api/v1/allocation-rules/resolve', {
    params: { productId, region, municipality }
  })
```

---

## Verification Checklist

1. Start the application — Hibernate creates the `accounting_allocation_rules` table with columns `product_id`, `region`, `municipality`, `accounting_account_id`, `specificity_score`, `description`, `active`.
2. `POST /api/v1/allocation-rules` with `productId=1, region=null, municipality=null` — response includes `specificityScore=1`.
3. `POST /api/v1/allocation-rules` with `productId=1, region="Uusimaa", municipality=null` — response includes `specificityScore=2`.
4. `POST /api/v1/allocation-rules` with `productId=1, region="Uusimaa", municipality="Helsinki"` — response includes `specificityScore=3`.
5. `GET /api/v1/allocation-rules/resolve?productId=1&region=Uusimaa&municipality=Helsinki` — returns the rule with `specificityScore=3`, not score 1 or 2.
6. `GET /api/v1/allocation-rules/resolve?productId=1&region=Uusimaa&municipality=Espoo` — returns the rule with `specificityScore=2` (region matches, no municipality rule exists for Espoo).
7. `GET /api/v1/allocation-rules/resolve?productId=1&region=Lapland&municipality=Rovaniemi` — returns the fallback rule with `specificityScore=1` (no region rule for Lapland).
8. `DELETE /api/v1/allocation-rules/{id}` — sets `active=false`; the rule no longer appears in resolve queries but is still returned by `GET /api/v1/allocation-rules`.
9. Open the Allocation Rules page in the FE — table renders all rules with product and account names resolved.
10. Use the Resolve Test Panel — enter product + region + municipality, click Test, verify the winning rule and account are displayed correctly.
11. Add a new rule via the modal — confirm `specificityScore` is calculated automatically and displayed read-only.
12. Edit an existing rule (change region) — confirm `specificityScore` recalculates correctly.

---

## File Checklist

### Backend
- [ ] `masterdata/allocation/AccountingAllocationRule.java`
- [ ] `masterdata/allocation/AccountingAllocationRuleRepository.java`
- [ ] `masterdata/allocation/AccountingAllocationRuleService.java`
- [ ] `masterdata/allocation/AccountingAllocationRuleController.java`
- [ ] `masterdata/allocation/dto/AllocationRuleRequest.java`
- [ ] `masterdata/allocation/dto/AllocationRuleResponse.java`
- [ ] `masterdata/allocation/dto/AllocationResolveResponse.java`

### Frontend
- [ ] `src/api/allocationRules.js`
- [ ] `src/pages/masterdata/AllocationRulesPage.jsx`
- [ ] `src/components/allocation/AllocationRulesTable.jsx`
- [ ] `src/components/allocation/AllocationRuleModal.jsx`
- [ ] `src/components/allocation/ResolveTestPanel.jsx`
