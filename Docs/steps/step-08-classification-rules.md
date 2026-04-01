# Step 08 — Classification Rules

## References to Original Requirements

- `Docs/structured_breakdown/01-domain-model.md` → Section: "ClassificationRule" — "customer type condition, product code condition, region condition, and the resulting classification (PUBLIC_LAW or PRIVATE_LAW). At event creation, the system evaluates rules in priority order and assigns the first match."
- `Docs/structured_breakdown/03-business-logic.md` → Section: "LegalClassificationService" — "Evaluates ClassificationRules in priority order. The first rule whose conditions all match determines the classification. If no rule matches, a default classification (configurable per company) is applied."
- `Docs/structured_breakdown/02-data-layer.md` → Section: implied by `ClassificationRule` entity — "active rules per company ordered by priority"
- `Docs/structured_breakdown/04-api-layer.md` → Section: none explicitly (implied CRUD for configuration master data, same pattern as `/vat-rates`, `/allocation-rules`)

---

## Goal

Implement the `ClassificationRule` entity and the `LegalClassificationService` that evaluates those rules to determine the legal classification of each billing event. This classification — PUBLIC_LAW or PRIVATE_LAW — is one of the most consequential data points in the invoicing system: it determines which accounting ledger an invoice line flows to, it controls whether combined or split invoicing applies (PD-284), and it is mandatory for FINVOICE reporting (PD-289).

Rules are evaluated in ascending priority order (lower number = higher priority). The first rule whose three conditions all match (customer type, product code, region) wins. Conditions that are `null` act as wildcards — they match everything. If no rule matches, a company-level default classification applies.

This step builds:
- The `LegalClassification` enum
- The `ClassificationRule` entity with priority and three nullable conditions
- The `ClassificationRuleRepository` with the active-rules-ordered-by-priority query
- The `LegalClassificationService` evaluation engine
- CRUD REST endpoints at `/api/v1/classification-rules`
- A React management page showing the rules table with inline priority ordering

---

## Backend

### 8.1 LegalClassification Enum

**File:** `invoicing/src/main/java/com/example/invoicing/classification/LegalClassification.java`

> **Requirement source:** `01-domain-model.md` — "resulting classification (PUBLIC_LAW or PRIVATE_LAW)"; `03-business-logic.md` — PD-284: combined vs separate invoicing based on this classification

```java
public enum LegalClassification {
    /**
     * Municipal/public-law service obligation — waste management under the
     * Waste Act (jätelaki) where the municipality is responsible.
     * Routes to public-law accounting accounts.
     */
    PUBLIC_LAW,

    /**
     * Market-based/private-law service — voluntary or contractual waste
     * services outside the municipal obligation.
     * Routes to private-law accounting accounts.
     */
    PRIVATE_LAW
}
```

---

### 8.2 ClassificationRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRule.java`

> **Requirement source:** `01-domain-model.md` — "priority-ordered list of rules, each with: customer type condition, product code condition, region condition, and the resulting classification"

```java
@Entity
@Table(name = "classification_rules",
       indexes = {
           @Index(name = "idx_class_rule_priority", columnList = "company_id, priority"),
           @Index(name = "idx_class_rule_active",   columnList = "company_id, active")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRule extends BaseAuditEntity {

    /**
     * Company this rule belongs to. Multi-tenant: each company configures its own rules.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /**
     * Evaluation order — lower number = evaluated first.
     * If two active rules have the same priority, the one with the lower ID wins
     * (consistent tiebreaker).
     * Users must be able to reorder rules — see ClassificationRuleService.reorder().
     */
    @Column(name = "priority", nullable = false)
    private int priority;

    /**
     * Condition 1: the customer type that triggers this rule.
     * NULL = matches any customer type (wildcard).
     * Source: 01-domain-model.md — "customer type condition"
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type_condition", length = 20)
    private CustomerType customerTypeCondition;

    /**
     * Condition 2: the product code (exact match) that triggers this rule.
     * NULL = matches any product.
     * Source: 01-domain-model.md — "product code condition"
     */
    @Column(name = "product_code_condition", length = 50)
    private String productCodeCondition;

    /**
     * Condition 3: the municipality or region code that triggers this rule.
     * NULL = matches any region.
     * Source: 01-domain-model.md — "region condition"
     */
    @Column(name = "region_condition", length = 50)
    private String regionCondition;

    /**
     * The classification assigned when all non-null conditions match.
     * This is what gets written to BillingEvent.legalClassification at creation time.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result_classification", nullable = false, length = 20)
    private LegalClassification resultClassification;

    /**
     * Human-readable label for this rule — shown in the management UI.
     * Example: "Business customers in Tampere region — private law"
     */
    @Column(name = "label", length = 200)
    private String label;

    /**
     * Inactive rules are never evaluated. Rules can be disabled without deletion
     * for audit/history purposes.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
```

---

### 8.3 ClassificationRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRuleRepository.java`

> **Requirement source:** `02-data-layer.md` — active rules per company ordered by priority; `03-business-logic.md` — LegalClassificationService evaluates rules in priority order

```java
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {

    /**
     * Returns all active rules for a company, sorted by priority ascending.
     * This is the primary query used by LegalClassificationService.evaluate().
     * A lower priority number is evaluated first — priority 1 beats priority 10.
     *
     * Source: 01-domain-model.md — "priority-ordered list of rules"; 03-business-logic.md
     * LegalClassificationService: "evaluates rules in priority order"
     */
    List<ClassificationRule> findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(Long companyId);

    /**
     * CRUD listing — returns all rules for a company regardless of active status,
     * ordered by priority. Used by the management UI.
     */
    List<ClassificationRule> findByCompanyIdOrderByPriorityAscIdAsc(Long companyId);

    /**
     * Existence check before inserting a rule at a given priority position.
     * Used to detect priority conflicts before a save.
     */
    boolean existsByCompanyIdAndPriorityAndIdNot(Long companyId, int priority, Long excludeId);
}
```

---

### 8.4 LegalClassificationService

**File:** `invoicing/src/main/java/com/example/invoicing/classification/LegalClassificationService.java`

> **Requirement source:** `03-business-logic.md` — "Evaluates ClassificationRules in priority order. The first rule whose conditions all match (customer type, product code, region) determines the classification. If no rule matches, a default classification (configurable per company) is applied."

```java
@Service
@RequiredArgsConstructor
public class LegalClassificationService {

    private final ClassificationRuleRepository ruleRepository;

    /**
     * Main evaluation entry point called by BillingEventService at event creation time.
     *
     * Iterates active rules in priority order (lowest number first). Each rule's
     * conditions are checked: a null condition is a wildcard and always matches.
     * Returns the classification from the first matching rule.
     * Falls back to companyDefaultClassification if no rule matches.
     *
     * Source: 03-business-logic.md LegalClassificationService
     * Source: 01-domain-model.md — "set at creation time — not re-derived at invoice time"
     *
     * @param companyId                   the company whose rules to evaluate
     * @param customerType                the customer's type
     * @param productCode                 the product code on the event
     * @param regionCode                  the municipality/region of the event
     * @param companyDefaultClassification fallback when no rule matches
     */
    public LegalClassification evaluate(Long companyId,
                                         CustomerType customerType,
                                         String productCode,
                                         String regionCode,
                                         LegalClassification companyDefaultClassification) {

        List<ClassificationRule> rules =
            ruleRepository.findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(companyId);

        for (ClassificationRule rule : rules) {
            if (matches(rule, customerType, productCode, regionCode)) {
                return rule.getResultClassification();
            }
        }

        // No rule matched — use company-level default (PD-285 configurable default)
        return companyDefaultClassification;
    }

    /**
     * A rule matches if every non-null condition equals the corresponding event field.
     * A null condition is a wildcard — it matches any value including null.
     */
    private boolean matches(ClassificationRule rule,
                             CustomerType customerType,
                             String productCode,
                             String regionCode) {
        if (rule.getCustomerTypeCondition() != null
                && rule.getCustomerTypeCondition() != customerType) {
            return false;
        }
        if (rule.getProductCodeCondition() != null
                && !rule.getProductCodeCondition().equals(productCode)) {
            return false;
        }
        if (rule.getRegionCondition() != null
                && !rule.getRegionCondition().equals(regionCode)) {
            return false;
        }
        return true;
    }
}
```

---

### 8.5 ClassificationRuleService — CRUD Operations

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRuleService.java`

Methods:

| Method signature | Description |
|---|---|
| `findAll(Long companyId)` | All rules for a company (active and inactive), ordered by priority |
| `findById(Long id)` | Single rule lookup |
| `create(Long companyId, ClassificationRuleRequest)` | Create new rule; validates no duplicate priority |
| `update(Long id, ClassificationRuleRequest)` | Update fields; validates priority uniqueness excluding self |
| `delete(Long id)` | Hard delete (rule has no foreign key references); or use deactivation |
| `reorder(Long companyId, List<Long> orderedIds)` | Reassigns priorities 1..N based on the provided ordering — used when the UI drags rows |

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ClassificationRuleService {

    private final ClassificationRuleRepository ruleRepo;

    @Transactional(readOnly = true)
    public List<ClassificationRuleResponse> findAll(Long companyId) {
        return ruleRepo.findByCompanyIdOrderByPriorityAscIdAsc(companyId)
                       .stream()
                       .map(ClassificationRuleResponse::from)
                       .toList();
    }

    @Transactional(readOnly = true)
    public ClassificationRuleResponse findById(Long id) {
        return ClassificationRuleResponse.from(
            ruleRepo.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id)));
    }

    public ClassificationRuleResponse create(Long companyId,
                                              ClassificationRuleRequest request) {
        if (ruleRepo.existsByCompanyIdAndPriorityAndIdNot(
                companyId, request.getPriority(), -1L)) {
            throw new DuplicatePriorityException(
                "A rule with priority " + request.getPriority() + " already exists");
        }
        ClassificationRule rule = request.toEntity(companyId);
        return ClassificationRuleResponse.from(ruleRepo.save(rule));
    }

    public ClassificationRuleResponse update(Long id, ClassificationRuleRequest request) {
        ClassificationRule rule = ruleRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
        Long companyId = rule.getCompanyId();

        if (ruleRepo.existsByCompanyIdAndPriorityAndIdNot(
                companyId, request.getPriority(), id)) {
            throw new DuplicatePriorityException(
                "A rule with priority " + request.getPriority() + " already exists");
        }
        request.applyTo(rule);
        return ClassificationRuleResponse.from(ruleRepo.save(rule));
    }

    public void delete(Long id) {
        ClassificationRule rule = ruleRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
        ruleRepo.delete(rule);
    }

    /**
     * Reorders rules by reassigning sequential priorities (1, 2, 3…) in the
     * order the caller provides. Used by the FE drag-and-drop reorder action.
     */
    public List<ClassificationRuleResponse> reorder(Long companyId, List<Long> orderedIds) {
        List<ClassificationRule> rules = ruleRepo.findAllById(orderedIds);
        for (int i = 0; i < orderedIds.size(); i++) {
            Long ruleId = orderedIds.get(i);
            rules.stream()
                 .filter(r -> r.getId().equals(ruleId))
                 .findFirst()
                 .ifPresent(r -> r.setPriority(i + 1));
        }
        ruleRepo.saveAll(rules);
        return findAll(companyId);
    }
}
```

---

### 8.6 ClassificationRuleController

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRuleController.java`

> **Requirement source:** `04-api-layer.md` — same CRUD pattern as `/validation-rules`, `/vat-rates`; `06-cross-cutting.md` — FUNCTION_ADMIN role required for writes (master data configuration)

Base path: `/api/v1/classification-rules`

| Method | Path | Role required | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/classification-rules?companyId=1` | INVOICING_USER | List all rules for a company |
| GET | `/api/v1/classification-rules/{id}` | INVOICING_USER | Get single rule |
| POST | `/api/v1/classification-rules` | FUNCTION_ADMIN | Create rule |
| PUT | `/api/v1/classification-rules/{id}` | FUNCTION_ADMIN | Update rule |
| DELETE | `/api/v1/classification-rules/{id}` | FUNCTION_ADMIN | Delete rule |
| PUT | `/api/v1/classification-rules/reorder` | FUNCTION_ADMIN | Reorder all rules for a company |

**POST/PUT request body:**
```json
{
  "companyId": 1,
  "priority": 10,
  "label": "Business customers in Tampere — private law",
  "customerTypeCondition": "BUSINESS",
  "productCodeCondition": null,
  "regionCondition": "TAMPERE",
  "resultClassification": "PRIVATE_LAW",
  "active": true
}
```

**GET list response body:**
```json
[
  {
    "id": 1,
    "companyId": 1,
    "priority": 10,
    "label": "Business customers in Tampere — private law",
    "customerTypeCondition": "BUSINESS",
    "productCodeCondition": null,
    "regionCondition": "TAMPERE",
    "resultClassification": "PRIVATE_LAW",
    "active": true,
    "createdBy": "admin",
    "lastModifiedAt": "2025-01-15T08:00:00Z"
  },
  {
    "id": 2,
    "companyId": 1,
    "priority": 20,
    "label": "All private customers — public law (default for private households)",
    "customerTypeCondition": "PRIVATE",
    "productCodeCondition": null,
    "regionCondition": null,
    "resultClassification": "PUBLIC_LAW",
    "active": true,
    "createdBy": "admin",
    "lastModifiedAt": "2025-01-15T08:01:00Z"
  }
]
```

**PUT /reorder request body:**
```json
{
  "companyId": 1,
  "orderedIds": [2, 1, 3]
}
```

The rule with ID 2 becomes priority 1, rule 1 becomes priority 2, rule 3 becomes priority 3.

**Duplicate priority error (409):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A rule with priority 10 already exists"
}
```

```java
@RestController
@RequestMapping("/api/v1/classification-rules")
@RequiredArgsConstructor
public class ClassificationRuleController {

    private final ClassificationRuleService ruleService;

    @GetMapping
    public ResponseEntity<List<ClassificationRuleResponse>> list(
            @RequestParam Long companyId) {
        return ResponseEntity.ok(ruleService.findAll(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationRuleResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClassificationRuleResponse> create(
            @RequestBody @Valid ClassificationRuleRequest request) {
        ClassificationRuleResponse created =
            ruleService.create(request.getCompanyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassificationRuleResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ClassificationRuleRequest request) {
        return ResponseEntity.ok(ruleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<ClassificationRuleResponse>> reorder(
            @RequestBody @Valid ReorderRequest request) {
        return ResponseEntity.ok(
            ruleService.reorder(request.getCompanyId(), request.getOrderedIds()));
    }
}
```

---

### 8.7 DTOs

**File:** `invoicing/src/main/java/com/example/invoicing/classification/dto/ClassificationRuleRequest.java`

```java
@Data
public class ClassificationRuleRequest {

    @NotNull
    private Long companyId;

    @Min(1)
    private int priority;

    @Size(max = 200)
    private String label;

    private CustomerType customerTypeCondition;     // null = wildcard
    private String productCodeCondition;            // null = wildcard
    private String regionCondition;                 // null = wildcard

    @NotNull
    private LegalClassification resultClassification;

    private boolean active = true;

    public ClassificationRule toEntity(Long companyId) {
        ClassificationRule rule = new ClassificationRule();
        rule.setCompanyId(companyId);
        applyTo(rule);
        return rule;
    }

    public void applyTo(ClassificationRule rule) {
        rule.setPriority(priority);
        rule.setLabel(label);
        rule.setCustomerTypeCondition(customerTypeCondition);
        rule.setProductCodeCondition(productCodeCondition);
        rule.setRegionCondition(regionCondition);
        rule.setResultClassification(resultClassification);
        rule.setActive(active);
    }
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/classification/dto/ClassificationRuleResponse.java`

```java
@Data
@Builder
public class ClassificationRuleResponse {
    private Long id;
    private Long companyId;
    private int priority;
    private String label;
    private CustomerType customerTypeCondition;
    private String productCodeCondition;
    private String regionCondition;
    private LegalClassification resultClassification;
    private boolean active;
    private String createdBy;
    private Instant lastModifiedAt;

    public static ClassificationRuleResponse from(ClassificationRule r) {
        return ClassificationRuleResponse.builder()
            .id(r.getId())
            .companyId(r.getCompanyId())
            .priority(r.getPriority())
            .label(r.getLabel())
            .customerTypeCondition(r.getCustomerTypeCondition())
            .productCodeCondition(r.getProductCodeCondition())
            .regionCondition(r.getRegionCondition())
            .resultClassification(r.getResultClassification())
            .active(r.isActive())
            .createdBy(r.getCreatedBy())
            .lastModifiedAt(r.getLastModifiedAt())
            .build();
    }
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/classification/dto/ReorderRequest.java`

```java
@Data
public class ReorderRequest {
    @NotNull private Long companyId;
    @NotEmpty private List<Long> orderedIds;
}
```

---

## Frontend

### 8.8 classificationRules.js — API Client

**File:** `invoicing-fe/src/api/classificationRules.js`

```js
import axios from './axios'

export const getClassificationRules = (companyId) =>
  axios.get('/api/v1/classification-rules', { params: { companyId } })

export const createClassificationRule = (data) =>
  axios.post('/api/v1/classification-rules', data)

export const updateClassificationRule = (id, data) =>
  axios.put(`/api/v1/classification-rules/${id}`, data)

export const deleteClassificationRule = (id) =>
  axios.delete(`/api/v1/classification-rules/${id}`)

export const reorderClassificationRules = (companyId, orderedIds) =>
  axios.put('/api/v1/classification-rules/reorder', { companyId, orderedIds })
```

---

### 8.9 ClassificationRulesPage.jsx

**File:** `invoicing-fe/src/pages/config/ClassificationRulesPage.jsx`

**Purpose:** FUNCTION_ADMIN page for managing the ordered list of classification rules that determine whether each billing event is PUBLIC_LAW or PRIVATE_LAW.

**Component structure:**

```
ClassificationRulesPage
├── PageHeader               — "Classification Rules"
│   └── AddRuleButton
├── CompanyDefaultBanner     — read-only notice: "Default when no rule matches: PUBLIC_LAW"
├── ClassificationRulesTable — priority-ordered table
│   ├── Column: Priority     — number, drag handle on the left for reordering
│   ├── Column: Label
│   ├── Column: Customer Type  — "Any" when null
│   ├── Column: Product Code   — "Any" when null
│   ├── Column: Region         — "Any" when null
│   ├── Column: Result         — PUBLIC_LAW / PRIVATE_LAW badge
│   ├── Column: Active         — toggle switch (calls PUT inline on toggle)
│   └── Column: Actions        — Edit | Delete buttons
└── ClassificationRuleModal  — Add / Edit form
    ├── PriorityInput          — number, hint text: "Lower number = evaluated first"
    ├── LabelInput
    ├── CustomerTypeSelect     — options: Any (null), PRIVATE, BUSINESS, MUNICIPALITY, AUTHORITY
    ├── ProductCodeInput       — text, placeholder "Leave blank to match any product"
    ├── RegionInput            — text, placeholder "Leave blank to match any region"
    ├── ResultSelect           — PUBLIC_LAW / PRIVATE_LAW
    └── ActiveCheckbox
```

**Key behaviours:**

- On mount: calls `getClassificationRules(companyId)`, renders table rows sorted by `priority`
- Drag-and-drop row reordering: on drop, collects new order of IDs, calls `reorderClassificationRules`. Show optimistic UI update; rollback on error
- The `priority` column shows the number but the drag handle is the primary reorder mechanism; direct priority number edits are also allowed via the modal
- If a new rule's priority duplicates an existing one, the API returns 409 — display "Priority already in use. Choose a different number or drag to reorder."
- Delete button: shows confirmation "Delete this rule? Events will fall through to lower-priority rules or the company default."
- Active toggle: calls PUT inline with `active` flipped; row grays out when `active = false`
- Rule result badges: PUBLIC_LAW shown in blue, PRIVATE_LAW shown in orange (distinct, not red/green to avoid accessibility confusion)

**State shape:**
```js
const [rules, setRules] = useState([])       // sorted by priority
const [modalOpen, setModalOpen] = useState(false)
const [editingRule, setEditingRule] = useState(null) // null = create mode
```

**API calls:**
```js
// Load
const { data } = await getClassificationRules(companyId)
setRules(data)

// Reorder (drag-and-drop)
const newOrderIds = newOrder.map(r => r.id)
const { data: reordered } = await reorderClassificationRules(companyId, newOrderIds)
setRules(reordered)

// Create
const { data: created } = await createClassificationRule({ companyId, ...formData })
setRules(prev => [...prev, created].sort((a, b) => a.priority - b.priority))

// Update
const { data: updated } = await updateClassificationRule(editingRule.id, { companyId, ...formData })
setRules(prev => prev.map(r => r.id === updated.id ? updated : r)
                     .sort((a, b) => a.priority - b.priority))

// Delete
await deleteClassificationRule(id)
setRules(prev => prev.filter(r => r.id !== id))
```

---

### 8.10 Route Registration

**File:** `invoicing-fe/src/App.jsx` — add:

```jsx
<Route path="/config/classification-rules" element={<ClassificationRulesPage />} />
```

Add to sidebar nav under a "Configuration" section.

---

## Verification Checklist

1. `POST /api/v1/classification-rules` — create a rule with `priority: 10`, `customerTypeCondition: BUSINESS`, `regionCondition: null`, `resultClassification: PRIVATE_LAW` — returns 201 with the created rule
2. `POST /api/v1/classification-rules` — create a rule with `priority: 20`, `customerTypeCondition: PRIVATE`, `resultClassification: PUBLIC_LAW` — returns 201
3. `POST /api/v1/classification-rules` — create a wildcard fallback with `priority: 99`, all conditions null, `resultClassification: PUBLIC_LAW` — returns 201
4. `GET /api/v1/classification-rules?companyId=1` — returns all three rules ordered `priority: 10, 20, 99`
5. `POST /api/v1/classification-rules` with `priority: 10` again — returns 409 "A rule with priority 10 already exists"
6. Call `LegalClassificationService.evaluate(companyId, BUSINESS, "PROD_A", "TAMPERE", PUBLIC_LAW)` — rule at priority 10 matches (`BUSINESS`, no product filter, no region filter) → returns `PRIVATE_LAW`
7. Call `LegalClassificationService.evaluate(companyId, PRIVATE, "PROD_A", "ESPOO", PUBLIC_LAW)` — priority 10 does not match (PRIVATE ≠ BUSINESS); priority 20 matches (PRIVATE) → returns `PUBLIC_LAW`
8. Call `LegalClassificationService.evaluate(companyId, MUNICIPALITY, "PROD_A", "ESPOO", PUBLIC_LAW)` — no rule matches MUNICIPALITY; falls back to default `PUBLIC_LAW`
9. `PUT /api/v1/classification-rules/1` — update `active: false`; repeat step 6 — priority 10 rule is inactive, so evaluation falls through to priority 20 → returns `PUBLIC_LAW`
10. `PUT /api/v1/classification-rules/reorder` with `orderedIds: [3, 1, 2]` — returns rules with new priorities 1, 2, 3 assigned respectively; verify DB priorities updated
11. `DELETE /api/v1/classification-rules/1` — returns 204; `GET` list no longer includes that rule
12. Open FE at `/config/classification-rules` — table renders all active rules ordered by priority
13. Click "Add Rule" — modal opens; fill in all fields; submit — new row appears in table at correct priority position
14. Edit a rule — change priority to an existing value — 409 error displayed below priority field
15. Toggle a rule's Active switch off — row visually grays out; refresh page — rule still inactive
16. Drag a row to a new position — table reorders optimistically; refresh page — new order persists

---

## File Checklist

### Backend
- [ ] `classification/LegalClassification.java`
- [ ] `classification/ClassificationRule.java`
- [ ] `classification/ClassificationRuleRepository.java`
- [ ] `classification/LegalClassificationService.java`
- [ ] `classification/ClassificationRuleService.java`
- [ ] `classification/ClassificationRuleController.java`
- [ ] `classification/dto/ClassificationRuleRequest.java`
- [ ] `classification/dto/ClassificationRuleResponse.java`
- [ ] `classification/dto/ReorderRequest.java`
- [ ] `common/exception/DuplicatePriorityException.java`

### Frontend
- [ ] `src/api/classificationRules.js`
- [ ] `src/pages/config/ClassificationRulesPage.jsx`
- [ ] `src/pages/config/components/ClassificationRulesTable.jsx`
- [ ] `src/pages/config/components/ClassificationRuleModal.jsx`
- [ ] `src/App.jsx` — add route `/config/classification-rules`
