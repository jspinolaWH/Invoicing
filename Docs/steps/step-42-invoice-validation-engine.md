# Step 42 — Invoice Validation Engine

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: ValidationRule
- `Docs/structured_breakdown/02-data-layer.md` → Section: ValidationRuleRepository
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceValidationEngine
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 6: "Validation Report Pattern"

---

## Goal
Implement `InvoiceValidationEngine`, which evaluates all active `ValidationRule` records against a set of invoice line items. Four rule types are supported: `MANDATORY_FIELD`, `PRICE_CONSISTENCY`, `QUANTITY_THRESHOLD`, and `CLASSIFICATION`. Each rule is either BLOCKING (halts invoice progression) or WARNING (reported but does not block). Returns a `ValidationReport` consumed by the invoice generation orchestrator and the simulation service.

---

## Backend

### 42.1 ValidationRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRule.java`

> **Requirement source:** `01-domain-model.md` — ValidationRule entity

```java
@Entity
@Table(name = "validation_rules")
public class ValidationRule extends BaseAuditEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private ValidationRuleType ruleType;    // MANDATORY_FIELD | PRICE_CONSISTENCY | QUANTITY_THRESHOLD | CLASSIFICATION

    @Column(name = "configuration", columnDefinition = "TEXT", nullable = false)
    private String configuration;           // JSON: e.g. {"field":"costCenter"} or {"threshold":30}

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;              // BLOCKING or WARNING

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 500)
    private String description;             // human-readable explanation

    @Column(name = "company_id")
    private Long companyId;                 // optional scope — null = applies to all companies
}
```

**`ValidationRuleType` enum:** `MANDATORY_FIELD`, `PRICE_CONSISTENCY`, `QUANTITY_THRESHOLD`, `CLASSIFICATION`

**`Severity` enum:** `BLOCKING`, `WARNING`

---

### 42.2 ValidationRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRuleRepository.java`

> **Requirement source:** `02-data-layer.md` — ValidationRuleRepository

```java
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

    // All active rules for a company — called at the start of every validation pass
    @Query("SELECT r FROM ValidationRule r WHERE r.active = true " +
           "AND (r.companyId = :companyId OR r.companyId IS NULL) " +
           "ORDER BY r.severity DESC, r.id ASC")
    List<ValidationRule> findActiveForCompany(@Param("companyId") Long companyId);
}
```

Blocking rules are ordered before Warning rules (DESC on severity) so blocking failures are reported first.

---

### 42.3 InvoiceValidationEngine

**File:** `invoicing/src/main/java/com/example/invoicing/validation/InvoiceValidationEngine.java`

> **Requirement source:** `03-business-logic.md` — InvoiceValidationEngine

```java
@Service
public class InvoiceValidationEngine {

    /**
     * Evaluate all active validation rules against the invoice and its line items.
     * Returns a ValidationReport — never throws. All failures are collected.
     */
    public ValidationReport validate(Invoice invoice, Long companyId) {
        List<ValidationRule> rules = ruleRepository.findActiveForCompany(companyId);
        List<ValidationFailure> failures = new ArrayList<>();

        for (ValidationRule rule : rules) {
            for (InvoiceLineItem line : invoice.getLineItems()) {
                Optional<String> failure = evaluate(rule, line, invoice);
                failure.ifPresent(msg -> failures.add(new ValidationFailure(
                    rule.getId(), rule.getRuleType(), rule.getSeverity(),
                    line.getId(), msg)));
            }
        }

        return ValidationReport.of(invoice.getLineItems().size(), failures);
    }

    private Optional<String> evaluate(ValidationRule rule, InvoiceLineItem line, Invoice invoice) {
        return switch (rule.getRuleType()) {
            case MANDATORY_FIELD     -> evaluateMandatoryField(rule, line);
            case PRICE_CONSISTENCY   -> evaluatePriceConsistency(rule, line);
            case QUANTITY_THRESHOLD  -> evaluateQuantityThreshold(rule, line);
            case CLASSIFICATION      -> evaluateClassification(rule, line);
        };
    }
}
```

---

### 42.4 Rule Implementations

**MANDATORY_FIELD** — checks that a specific field on `InvoiceLineItem` is not null/blank.

Configuration JSON: `{"field": "costCenter"}`

```java
private Optional<String> evaluateMandatoryField(ValidationRule rule, InvoiceLineItem line) {
    String field = parseConfig(rule.getConfiguration()).get("field").asText();
    Object value = getFieldValue(line, field);
    if (value == null || (value instanceof String s && s.isBlank())) {
        return Optional.of("Field '" + field + "' is required but missing on line item " + line.getId());
    }
    return Optional.empty();
}
```

**PRICE_CONSISTENCY** — checks that `unitPrice` is positive (unless the invoice is a credit note, in which case negative is valid).

Configuration JSON: `{}` (no extra config needed)

```java
private Optional<String> evaluatePriceConsistency(ValidationRule rule, InvoiceLineItem line) {
    boolean isCreditNote = line.getInvoice().getInvoiceType() == InvoiceType.CREDIT_NOTE;
    if (!isCreditNote && line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        return Optional.of("Unit price must be positive on non-credit invoice, line " + line.getId());
    }
    return Optional.empty();
}
```

**QUANTITY_THRESHOLD** — checks that `quantity` does not exceed a configured maximum.

Configuration JSON: `{"threshold": 30}`

```java
private Optional<String> evaluateQuantityThreshold(ValidationRule rule, InvoiceLineItem line) {
    BigDecimal threshold = new BigDecimal(parseConfig(rule.getConfiguration()).get("threshold").asText());
    if (line.getQuantity().compareTo(threshold) > 0) {
        return Optional.of("Quantity " + line.getQuantity() + " exceeds threshold "
            + threshold + " on line " + line.getId());
    }
    return Optional.empty();
}
```

**CLASSIFICATION** — checks that `legalClassification` is set (not null).

Configuration JSON: `{}` (no extra config needed)

```java
private Optional<String> evaluateClassification(ValidationRule rule, InvoiceLineItem line) {
    if (line.getLegalClassification() == null) {
        return Optional.of("Legal classification is not set on line item " + line.getId());
    }
    return Optional.empty();
}
```

---

### 42.5 ValidationReport Return Structure

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationReport.java`

```java
public class ValidationReport {
    private int totalChecked;
    private int passed;
    private List<ValidationFailure> failures;  // both BLOCKING and WARNING

    public boolean hasBlockingFailures() {
        return failures.stream().anyMatch(f -> f.getSeverity() == Severity.BLOCKING);
    }

    public List<ValidationFailure> getBlockingFailures() {
        return failures.stream().filter(f -> f.getSeverity() == Severity.BLOCKING).toList();
    }

    public List<ValidationFailure> getWarnings() {
        return failures.stream().filter(f -> f.getSeverity() == Severity.WARNING).toList();
    }
}

public class ValidationFailure {
    private Long ruleId;
    private ValidationRuleType ruleType;
    private Severity severity;
    private Long lineItemId;
    private String description;
}
```

---

### 42.6 ValidationRuleController (CRUD)

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRuleController.java`

Base path: `/api/v1/validation-rules`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/validation-rules` | List all active rules | INVOICING_USER |
| POST | `/api/v1/validation-rules` | Create a new rule | FUNCTION_ADMIN |
| PUT | `/api/v1/validation-rules/{id}` | Update a rule | FUNCTION_ADMIN |
| DELETE | `/api/v1/validation-rules/{id}` | Deactivate a rule | FUNCTION_ADMIN |

**POST request:**
```json
{
  "ruleType": "QUANTITY_THRESHOLD",
  "configuration": "{\"threshold\": 30}",
  "severity": "WARNING",
  "description": "Warn when more than 30 bin emptying events on one line"
}
```

**GET response (array):**
```json
[
  {
    "id": 1,
    "ruleType": "MANDATORY_FIELD",
    "configuration": "{\"field\":\"costCenter\"}",
    "severity": "BLOCKING",
    "active": true,
    "description": "Cost center is required on all invoice lines"
  }
]
```

---

## Frontend

### 42.7 Validation Rules Admin Page

**File:** `invoicing-fe/src/pages/masterdata/ValidationRulesPage.jsx`

Components:
- **ValidationRulesTable** — columns: Rule Type, Severity (BLOCKING badge red / WARNING badge amber), Description, Active, Actions (Edit / Deactivate).
- **ValidationRuleForm** — modal: rule type (select), severity (radio: BLOCKING / WARNING), description (text), configuration (JSON text area with syntax hint per type).
- **ConfigurationHint** — displayed below the JSON area, changes based on selected rule type:
  - `MANDATORY_FIELD`: hint = `{"field": "costCenter"}`
  - `QUANTITY_THRESHOLD`: hint = `{"threshold": 30}`
  - Others: hint = `{}`

**Validation failures display** in `InvoiceRunDetailPage` (step 38) and `SimulationResultsPage` (step 40): already covered by `ValidationFailuresTable` component.

**API calls via `src/api/validationRules.js`:**
```js
export const getValidationRules = () => axios.get('/api/v1/validation-rules')
export const createValidationRule = (data) => axios.post('/api/v1/validation-rules', data)
export const updateValidationRule = (id, data) => axios.put(`/api/v1/validation-rules/${id}`, data)
export const deactivateValidationRule = (id) => axios.delete(`/api/v1/validation-rules/${id}`)
```

---

## Verification Checklist

1. Create a MANDATORY_FIELD BLOCKING rule for `costCenter`. Generate an invoice line with `costCenter = null` → `ValidationReport.hasBlockingFailures()` = true.
2. Create a QUANTITY_THRESHOLD WARNING rule with threshold 30. Generate a line with quantity 35 → report has one WARNING, no BLOCKING failures. Invoice generation continues.
3. Create a CLASSIFICATION BLOCKING rule. Generate a line with `legalClassification = null` → blocking failure.
4. All failures are collected before returning (not fail-fast on first) — test with 3 invalid lines → report contains 3 failure entries.
5. PRICE_CONSISTENCY rule: standard invoice with negative unit price → blocking failure. Credit note invoice with negative unit price → no failure.
6. `findActiveForCompany` returns only `active = true` rules; deactivated rule is excluded.
7. BLOCKING rules appear before WARNING rules in the report (ordered by severity DESC).
8. Open `ValidationRulesPage` in FE — table shows rules with colour-coded severity badges.
9. Add a new rule via form — configuration JSON hint changes when rule type is switched.
10. Deactivate a rule — it disappears from the active rules list; subsequent validation run does not evaluate it.

---

## File Checklist

### Backend
- [ ] `validation/ValidationRule.java`
- [ ] `validation/ValidationRuleType.java` (enum)
- [ ] `validation/Severity.java` (enum)
- [ ] `validation/ValidationRuleRepository.java`
- [ ] `validation/InvoiceValidationEngine.java`
- [ ] `validation/ValidationReport.java`
- [ ] `validation/ValidationFailure.java`
- [ ] `validation/ValidationRuleController.java`
- [ ] `validation/dto/ValidationRuleRequest.java`
- [ ] `validation/dto/ValidationRuleResponse.java`

### Frontend
- [ ] `src/pages/masterdata/ValidationRulesPage.jsx`
- [ ] `src/pages/masterdata/components/ValidationRulesTable.jsx`
- [ ] `src/pages/masterdata/components/ValidationRuleForm.jsx`
- [ ] `src/api/validationRules.js`
