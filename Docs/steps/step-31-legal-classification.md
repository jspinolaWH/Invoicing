# Step 31 — Legal Classification Service

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: ClassificationRule, BillingEvent (legalClassification field)
- `Docs/structured_breakdown/03-business-logic.md` → Section: LegalClassificationService
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: "Simulation Mode Guard"

---

## Goal
Implement `LegalClassificationService`, which evaluates `ClassificationRule` records in priority order to determine whether each `BillingEvent` is `PUBLIC_LAW` or `PRIVATE_LAW`. The first matching rule wins. If no rule matches, a validation error is thrown — there is no silent default. Classification is set on the event at creation time and displayed as a badge on billing event and invoice line item views.

---

## Backend

### 31.1 ClassificationRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRule.java`

> **Requirement source:** `01-domain-model.md` — ClassificationRule entity

```java
@Entity
@Table(name = "classification_rules")
public class ClassificationRule extends BaseAuditEntity {

    @Column(name = "priority", nullable = false)
    private int priority;                      // lower number = evaluated first

    @Column(name = "customer_type_condition", length = 50)
    private String customerTypeCondition;      // null = match any customer type

    @Column(name = "product_code_condition", length = 100)
    private String productCodeCondition;       // null = match any product

    @Column(name = "region_condition", length = 100)
    private String regionCondition;            // null = match any region / municipality

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_classification", nullable = false)
    private LegalClassification resultingClassification; // PUBLIC_LAW or PRIVATE_LAW

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 255)
    private String description;
}
```

Rules are evaluated in ascending `priority` order (priority 1 is checked before priority 2). A rule matches when ALL non-null conditions match the event being classified.

---

### 31.2 ClassificationRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRuleRepository.java`

```java
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {

    // Returns all active rules in priority order — called at classification time
    @Query("SELECT r FROM ClassificationRule r WHERE r.active = true ORDER BY r.priority ASC")
    List<ClassificationRule> findAllActiveOrderedByPriority();
}
```

---

### 31.3 LegalClassificationService

**File:** `invoicing/src/main/java/com/example/invoicing/classification/LegalClassificationService.java`

> **Requirement source:** `03-business-logic.md` — LegalClassificationService

**Algorithm:**
1. Load all active `ClassificationRule` records ordered by `priority` ascending.
2. For each rule (in order):
   - If `customerTypeCondition != null` and it does not match the event's customer type → skip this rule.
   - If `productCodeCondition != null` and it does not match the event's product code → skip this rule.
   - If `regionCondition != null` and it does not match the event's municipality → skip this rule.
   - If all non-null conditions match → return `rule.resultingClassification`. Stop evaluating.
3. If no rule matched after exhausting the list → throw `ClassificationResolutionException` with message including event ID, customer type, product code, and region. This exception is blocking — the event cannot proceed to invoicing.

**Method signatures:**
```java
@Service
public class LegalClassificationService {

    // Classify a single event — throws ClassificationResolutionException if no rule matches
    public LegalClassification classify(BillingEvent event);

    // Bulk classify a list of events — returns map of eventId → classification
    // Collects all failures before throwing, so the caller sees the full failure set
    public Map<Long, LegalClassification> classifyAll(List<BillingEvent> events);

    // Re-classify without saving — used in simulation and preview
    public LegalClassification classifyDry(String customerType, String productCode, String municipality);
}
```

**Exception:**
```java
public class ClassificationResolutionException extends RuntimeException {
    private final Long eventId;
    private final String customerType;
    private final String productCode;
    private final String municipality;
}
```

---

### 31.4 ClassificationRuleController (master data CRUD)

**File:** `invoicing/src/main/java/com/example/invoicing/classification/ClassificationRuleController.java`

Base path: `/api/v1/classification-rules`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/classification-rules` | List all rules ordered by priority | INVOICING_USER |
| POST | `/api/v1/classification-rules` | Create a new rule | FUNCTION_ADMIN |
| PUT | `/api/v1/classification-rules/{id}` | Update a rule | FUNCTION_ADMIN |
| DELETE | `/api/v1/classification-rules/{id}` | Deactivate a rule (soft delete) | FUNCTION_ADMIN |

**POST request body:**
```json
{
  "priority": 10,
  "customerTypeCondition": "MUNICIPALITY",
  "productCodeCondition": null,
  "regionCondition": null,
  "resultingClassification": "PUBLIC_LAW",
  "description": "All municipal customers → public law"
}
```

**GET response (array):**
```json
[
  {
    "id": 1,
    "priority": 10,
    "customerTypeCondition": "MUNICIPALITY",
    "productCodeCondition": null,
    "regionCondition": null,
    "resultingClassification": "PUBLIC_LAW",
    "active": true,
    "description": "All municipal customers → public law"
  },
  {
    "id": 2,
    "priority": 20,
    "customerTypeCondition": null,
    "productCodeCondition": null,
    "regionCondition": null,
    "resultingClassification": "PRIVATE_LAW",
    "active": true,
    "description": "Default: private law"
  }
]
```

---

## Frontend

### 31.5 Classification Badge Component

**File:** `invoicing-fe/src/components/ClassificationBadge.jsx`

Renders a coloured badge based on the `legalClassification` string value:
- `PUBLIC_LAW` → blue badge with text "Public Law"
- `PRIVATE_LAW` → orange badge with text "Private Law"
- Unknown/null → grey badge with text "Unclassified"

Used in:
- `BillingEventsTable` (step 10 column)
- `InvoiceLineItemsTable` (step 29 column)

**No API call** — purely a display component that receives `classification` as a prop.

### 31.6 Classification Rules Admin Page

**File:** `invoicing-fe/src/pages/masterdata/ClassificationRulesPage.jsx`

Components:
- **ClassificationRulesTable** — columns: Priority, Customer Type Condition, Product Code Condition, Region Condition, Resulting Classification (badge), Active, Actions (Edit / Deactivate).
- **ClassificationRuleForm** — modal: priority (number), customerTypeCondition (text, optional), productCodeCondition (text, optional), regionCondition (text, optional), resultingClassification (select: PUBLIC_LAW / PRIVATE_LAW), description (text, optional).
- Rules are displayed in ascending priority order matching server-side evaluation order.

**API calls via `src/api/classificationRules.js`:**
```js
export const getClassificationRules = () =>
  axios.get('/api/v1/classification-rules')

export const createClassificationRule = (data) =>
  axios.post('/api/v1/classification-rules', data)

export const updateClassificationRule = (id, data) =>
  axios.put(`/api/v1/classification-rules/${id}`, data)

export const deactivateClassificationRule = (id) =>
  axios.delete(`/api/v1/classification-rules/${id}`)
```

---

## Verification Checklist

1. Create two rules: priority 10 = MUNICIPALITY customer type → PUBLIC_LAW; priority 20 = no conditions → PRIVATE_LAW.
2. Call `LegalClassificationService.classify()` with a MUNICIPALITY customer event → returns `PUBLIC_LAW`.
3. Call `classify()` with a BUSINESS customer event → falls through to rule 20 → returns `PRIVATE_LAW`.
4. Delete both rules and call `classify()` → `ClassificationResolutionException` is thrown.
5. Call `classifyAll()` with a mix of events where some cannot be classified → all failures are collected and reported together (not fail-fast on first).
6. `GET /api/v1/classification-rules` — returns rules in ascending priority order.
7. `POST /api/v1/classification-rules` — creates a rule; subsequent GET shows it in correct priority position.
8. Deactivate a rule via `DELETE` — it no longer appears in `findAllActiveOrderedByPriority()` results.
9. Open `ClassificationRulesPage` in FE — table shows rules with colour-coded classification badges.
10. In `BillingEventsTable` and `InvoiceLineItemsTable`, classification badge renders correctly for PUBLIC_LAW (blue) and PRIVATE_LAW (orange).

---

## File Checklist

### Backend
- [ ] `classification/ClassificationRule.java`
- [ ] `classification/ClassificationRuleRepository.java`
- [ ] `classification/LegalClassificationService.java`
- [ ] `classification/ClassificationRuleController.java`
- [ ] `classification/ClassificationResolutionException.java`
- [ ] `classification/dto/ClassificationRuleRequest.java`
- [ ] `classification/dto/ClassificationRuleResponse.java`

### Frontend
- [ ] `src/components/ClassificationBadge.jsx`
- [ ] `src/pages/masterdata/ClassificationRulesPage.jsx`
- [ ] `src/pages/masterdata/components/ClassificationRulesTable.jsx`
- [ ] `src/pages/masterdata/components/ClassificationRuleForm.jsx`
- [ ] `src/api/classificationRules.js`
