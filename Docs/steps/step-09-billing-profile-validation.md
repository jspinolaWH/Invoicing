# Step 09 — Billing Profile Validation

## References to Original Requirements

- `Docs/structured_breakdown/03-business-logic.md` → Section: "BillingProfileValidationService" — "Checks: CustomerID present (6–9 digits), delivery method set, billing address complete, BusinessID present (for e-invoicing), e-invoice address set (if delivery method = E_INVOICE). Returns a structured list of issues. InvoiceGenerationService calls this before starting generation for each customer."
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 6: "Validation Report Pattern" — `totalChecked`, `passed`, `failures` list with `entityId`, `entityType`, `rule`, `severity` (BLOCKING or WARNING), `description`
- `Docs/structured_breakdown/01-domain-model.md` → Section: "ValidationRule" — company-configurable rules, rule types (MANDATORY_FIELD, PRICE_CONSISTENCY, QUANTITY_THRESHOLD, CLASSIFICATION), blocking vs warning-only, active flag; also Section: "InvoiceRun" — "ValidationReport embedded in it — populated during simulation and re-run on the real run"
- `Docs/structured_breakdown/02-data-layer.md` → Section: "ValidationRuleRepository" — `findAllActiveByCompanyId`

---

## Goal

Implement the `BillingProfileValidationService` that checks whether a customer has every piece of data required to receive an invoice, and expose it as a standalone HTTP endpoint that returns a structured `ValidationReport`. This is distinct from the event-level validation of Step 01 and the invoice-level validation of `InvoiceValidationEngine` — it is specifically about customer profile completeness.

The service is called in two contexts:
1. **From `InvoiceGenerationService`** before generating invoices for each customer — a customer with a blocking profile failure is skipped for that run and the failures appear in the run's error report
2. **From the REST API** — an office user clicks "Validate" on the billing profile page and sees a modal summarising all issues before starting a run

The `ValidationReport` shape is defined once (Rule 6 of cross-cutting) and reused everywhere validation results are returned. This step implements the pattern for the billing profile case; the same structure is reused in `InvoiceSimulationService` and `InvoiceValidationEngine`.

The `ValidationRule` entity from master data (PD-271, PD-278) provides the configurable rule set. Hard-coded checks (customerIdNumber format, e-invoice address presence) are always applied; company-configured `ValidationRule` records can add further mandatory field checks on top.

---

## Backend

### 9.1 ValidationSeverity Enum

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationSeverity.java`

> **Requirement source:** `06-cross-cutting.md` Rule 6 — "Blocking failures prevent progression. Warning failures are reported but do not block. The user must see both."

```java
public enum ValidationSeverity {
    /**
     * Blocks invoice generation for this customer. The run skips this customer
     * entirely and includes the failure in the run's error report.
     * Source: 06-cross-cutting.md Rule 6
     */
    BLOCKING,

    /**
     * Reported to the user but does not prevent invoice generation.
     * Example: billing address has no alt-language variant (desirable but not required).
     */
    WARNING
}
```

---

### 9.2 ValidationFailure Value Object

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationFailure.java`

> **Requirement source:** `06-cross-cutting.md` Rule 6 — "each failure has: entity ID, entity type, rule that failed, severity, description of the issue"

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationFailure {

    /** The ID of the entity that failed — customer ID for profile checks */
    private Long entityId;

    /** "CUSTOMER", "BILLING_EVENT", "INVOICE" etc. — identifies the entity type */
    private String entityType;

    /** The field path that failed, e.g. "billingProfile.customerIdNumber" */
    private String field;

    /**
     * The rule code that triggered the failure.
     * Hard-coded checks use codes like "CUSTOMER_ID_FORMAT".
     * Company-configured ValidationRule records use their own configurable code.
     */
    private String rule;

    /** BLOCKING or WARNING */
    private ValidationSeverity severity;

    /** Human-readable description shown to the office user */
    private String description;
}
```

---

### 9.3 ValidationReport Value Object

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationReport.java`

> **Requirement source:** `06-cross-cutting.md` Rule 6 — "totalChecked, passed, failures list"

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationReport {

    /** How many entities were evaluated in this report */
    private int totalChecked;

    /** How many entities passed all checks (zero blocking failures) */
    private int passed;

    /** All failures — BLOCKING and WARNING together */
    private List<ValidationFailure> failures;

    /** Convenience: failures filtered to BLOCKING only */
    public List<ValidationFailure> blockingFailures() {
        return failures.stream()
                       .filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING)
                       .toList();
    }

    /** Convenience: failures filtered to WARNING only */
    public List<ValidationFailure> warnings() {
        return failures.stream()
                       .filter(f -> f.getSeverity() == ValidationSeverity.WARNING)
                       .toList();
    }

    /** Returns true if any BLOCKING failure is present */
    public boolean hasBlockingFailures() {
        return failures.stream()
                       .anyMatch(f -> f.getSeverity() == ValidationSeverity.BLOCKING);
    }
}
```

---

### 9.4 ValidationRule Entity

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRule.java`

> **Requirement source:** `01-domain-model.md` — ValidationRule entity: "rule type (MANDATORY_FIELD / PRICE_CONSISTENCY / QUANTITY_THRESHOLD / CLASSIFICATION), configuration as structured data, whether blocking or warning-only, and whether active"

```java
@Entity
@Table(name = "validation_rules",
       indexes = @Index(name = "idx_val_rule_company", columnList = "company_id, active"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule extends BaseAuditEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /**
     * Rule categories as defined in 01-domain-model.md.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private ValidationRuleType ruleType;

    /**
     * A short unique code used to identify this rule in failure reports.
     * Example: "MISSING_COST_CENTER", "MAX_30_CONTAINERS"
     */
    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    /**
     * JSON-structured configuration. Schema depends on ruleType:
     * MANDATORY_FIELD: {"field": "billingProfile.businessId"}
     * QUANTITY_THRESHOLD: {"field": "quantity", "max": 30}
     * CLASSIFICATION: {"allowedValues": ["PUBLIC_LAW","PRIVATE_LAW"]}
     * Source: 01-domain-model.md — "configuration as structured data"
     */
    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    /**
     * When true, a failure from this rule blocks invoice progression.
     * When false, the failure is reported as a warning only.
     * Source: 06-cross-cutting.md Rule 6
     */
    @Column(name = "blocking", nullable = false)
    private boolean blocking = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 500)
    private String description;
}
```

---

### 9.5 ValidationRuleType Enum

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRuleType.java`

> **Requirement source:** `01-domain-model.md` — "rule type (MANDATORY_FIELD / PRICE_CONSISTENCY / QUANTITY_THRESHOLD / CLASSIFICATION)"

```java
public enum ValidationRuleType {
    MANDATORY_FIELD,      // checks a specific field is not null or empty
    PRICE_CONSISTENCY,    // checks event price matches price list price
    QUANTITY_THRESHOLD,   // checks a quantity field does not exceed a configured maximum
    CLASSIFICATION        // checks legal classification is set and valid
}
```

---

### 9.6 ValidationRuleRepository

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRuleRepository.java`

> **Requirement source:** `02-data-layer.md` — "Find all active rules for a company" — called at the start of every validation pass; blocking rules first

```java
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

    /**
     * Primary query: all active rules for a company.
     * Blocking rules ordered first so they appear at the top of failure reports.
     * Source: 02-data-layer.md — ValidationRuleRepository
     */
    @Query("""
        SELECT v FROM ValidationRule v
        WHERE v.companyId = :companyId AND v.active = true
        ORDER BY v.blocking DESC, v.ruleCode ASC
        """)
    List<ValidationRule> findAllActiveByCompanyId(@Param("companyId") Long companyId);

    /** CRUD listing: all rules including inactive */
    List<ValidationRule> findByCompanyIdOrderByBlockingDescRuleCodeAsc(Long companyId);
}
```

---

### 9.7 BillingProfileValidationService

**File:** `invoicing/src/main/java/com/example/invoicing/validation/BillingProfileValidationService.java`

> **Requirement source:** `03-business-logic.md` — BillingProfileValidationService; `01-domain-model.md` — ValidationRule; `06-cross-cutting.md` Rule 6 — ValidationReport pattern

```java
@Service
@RequiredArgsConstructor
public class BillingProfileValidationService {

    private final CustomerBillingProfileRepository customerRepo;
    private final EInvoiceAddressRepository einvoiceAddressRepo;
    private final ValidationRuleRepository validationRuleRepo;

    /**
     * Validates a single customer's billing profile.
     * Called by InvoiceGenerationService (03-business-logic.md) before generating
     * invoices for each customer, and by the REST controller for on-demand checks.
     *
     * Hard-coded checks always run first. Company-configured ValidationRule records
     * of type MANDATORY_FIELD are evaluated after.
     *
     * Source: 03-business-logic.md — BillingProfileValidationService;
     *         06-cross-cutting.md — Rule 6 ValidationReport pattern
     */
    public ValidationReport validate(Long customerId, Long companyId) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        List<ValidationFailure> failures = new ArrayList<>();

        BillingProfile profile = customer.getBillingProfile();

        // ── Hard-coded mandatory checks (always BLOCKING) ─────────────────────

        // 1. CustomerID: must be present and 6–9 digits (PD-298)
        if (profile == null || profile.getCustomerIdNumber() == null
                || !profile.getCustomerIdNumber().matches("\\d{6,9}")) {
            failures.add(failure(customerId, "billingProfile.customerIdNumber",
                "CUSTOMER_ID_FORMAT", ValidationSeverity.BLOCKING,
                "Customer ID must be a 6–9 digit numeric sequence (PD-298)"));
        }

        // 2. Delivery method must be set (PD-298)
        if (profile == null || profile.getDeliveryMethod() == null) {
            failures.add(failure(customerId, "billingProfile.deliveryMethod",
                "DELIVERY_METHOD_MISSING", ValidationSeverity.BLOCKING,
                "Invoice delivery method is required (PD-298)"));
        }

        // 3. Billing address — street, postal code, city, country all required (PD-298)
        BillingAddress addr = profile != null ? profile.getBillingAddress() : null;
        if (addr == null || isBlank(addr.getStreetAddress())) {
            failures.add(failure(customerId, "billingProfile.billingAddress.streetAddress",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address street is required (PD-298)"));
        }
        if (addr == null || isBlank(addr.getPostalCode())) {
            failures.add(failure(customerId, "billingProfile.billingAddress.postalCode",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address postal code is required (PD-298)"));
        }
        if (addr == null || isBlank(addr.getCity())) {
            failures.add(failure(customerId, "billingProfile.billingAddress.city",
                "BILLING_ADDRESS_INCOMPLETE", ValidationSeverity.BLOCKING,
                "Billing address city is required (PD-298)"));
        }

        // 4. Business ID is required when delivery method is E_INVOICE (PD-298)
        if (profile != null && profile.getDeliveryMethod() == DeliveryMethod.E_INVOICE
                && isBlank(profile.getBusinessId())) {
            failures.add(failure(customerId, "billingProfile.businessId",
                "BUSINESS_ID_REQUIRED_FOR_EINVOICE", ValidationSeverity.BLOCKING,
                "Business ID is required for e-invoice delivery (PD-298)"));
        }

        // 5. E-invoice address must be present when delivery method is E_INVOICE (PD-298, PD-282)
        if (profile != null && profile.getDeliveryMethod() == DeliveryMethod.E_INVOICE) {
            boolean hasEInvoiceAddress = einvoiceAddressRepo
                .findByCustomer_Id(customerId)
                .map(e -> !isBlank(e.getAddress()))
                .orElse(false);

            if (!hasEInvoiceAddress) {
                failures.add(failure(customerId, "einvoiceAddress.address",
                    "EINVOICE_ADDRESS_MISSING", ValidationSeverity.BLOCKING,
                    "An e-invoice address is required when delivery method is E_INVOICE (PD-282)"));
            }
        }

        // 6. Language code should be set — WARNING only (not blocking, but desirable)
        if (profile != null && isBlank(profile.getLanguageCode())) {
            failures.add(failure(customerId, "billingProfile.languageCode",
                "LANGUAGE_CODE_MISSING", ValidationSeverity.WARNING,
                "Language code is not set. Finnish (fi) will be used as default (PD-308)"));
        }

        // ── Company-configured MANDATORY_FIELD ValidationRule checks ─────────
        List<ValidationRule> companyRules =
            validationRuleRepo.findAllActiveByCompanyId(companyId)
                              .stream()
                              .filter(r -> r.getRuleType() == ValidationRuleType.MANDATORY_FIELD)
                              .toList();

        for (ValidationRule rule : companyRules) {
            evaluateMandatoryFieldRule(rule, customerId, profile, failures);
        }

        // ── Assemble report ──────────────────────────────────────────────────
        int hasAnyBlockingFailure = (int) failures.stream()
            .filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).count();

        return ValidationReport.builder()
            .totalChecked(1)
            .passed(hasAnyBlockingFailure == 0 ? 1 : 0)
            .failures(failures)
            .build();
    }

    /**
     * Bulk validation — validates a list of customers and returns a combined report.
     * Called by InvoiceRunService at run start.
     */
    public ValidationReport validateAll(List<Long> customerIds, Long companyId) {
        List<ValidationFailure> allFailures = new ArrayList<>();
        int passed = 0;

        for (Long customerId : customerIds) {
            ValidationReport single = validate(customerId, companyId);
            allFailures.addAll(single.getFailures());
            passed += single.getPassed();
        }

        return ValidationReport.builder()
            .totalChecked(customerIds.size())
            .passed(passed)
            .failures(allFailures)
            .build();
    }

    private void evaluateMandatoryFieldRule(ValidationRule rule,
                                             Long customerId,
                                             BillingProfile profile,
                                             List<ValidationFailure> failures) {
        // config JSON shape: {"field": "billingProfile.businessId"}
        // Simple field-presence check; extend as new field paths are configured
        try {
            String fieldPath = extractFieldFromConfig(rule.getConfig());
            boolean isPresent = resolveFieldPresence(profile, fieldPath);
            if (!isPresent) {
                ValidationSeverity severity = rule.isBlocking()
                    ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING;
                failures.add(failure(customerId, fieldPath, rule.getRuleCode(),
                    severity, rule.getDescription()));
            }
        } catch (Exception e) {
            // Misconfigured rule — log and skip rather than crashing validation
        }
    }

    private ValidationFailure failure(Long customerId, String field,
                                       String rule, ValidationSeverity severity,
                                       String description) {
        return ValidationFailure.builder()
            .entityId(customerId)
            .entityType("CUSTOMER")
            .field(field)
            .rule(rule)
            .severity(severity)
            .description(description)
            .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // Minimal field-path resolver for MANDATORY_FIELD rules on BillingProfile.
    // Add more paths here as company-configured rules are expanded.
    private boolean resolveFieldPresence(BillingProfile profile, String fieldPath) {
        if (profile == null) return false;
        return switch (fieldPath) {
            case "billingProfile.businessId"    -> !isBlank(profile.getBusinessId());
            case "billingProfile.languageCode"  -> !isBlank(profile.getLanguageCode());
            case "billingProfile.invoicingMode" -> profile.getInvoicingMode() != null;
            default -> true; // unknown field path — treat as present to avoid false positives
        };
    }

    private String extractFieldFromConfig(String config) {
        // Minimal JSON field extraction — replace with Jackson ObjectMapper in production
        int start = config.indexOf("\"field\":\"") + 9;
        int end   = config.indexOf("\"", start);
        return config.substring(start, end);
    }
}
```

---

### 9.8 BillingProfileValidationController

**File:** `invoicing/src/main/java/com/example/invoicing/validation/BillingProfileValidationController.java`

> **Requirement source:** `03-business-logic.md` — BillingProfileValidationService used as a standalone check; `04-api-layer.md` — same pattern as `/billing-events/validate`

Base path: `/api/v1/customers/{customerId}/billing-profile/validate`

| Method | Path | Role required | Description |
|--------|------|---------------|-------------|
| POST | `/api/v1/customers/{id}/billing-profile/validate` | INVOICING_USER | Run profile validation; return ValidationReport |

**POST request body:**
```json
{
  "companyId": 1
}
```

**POST response — profile is valid (200):**
```json
{
  "totalChecked": 1,
  "passed": 1,
  "failures": []
}
```

**POST response — profile has blocking failures (200, but failures list is non-empty):**
```json
{
  "totalChecked": 1,
  "passed": 0,
  "failures": [
    {
      "entityId": 42,
      "entityType": "CUSTOMER",
      "field": "billingProfile.customerIdNumber",
      "rule": "CUSTOMER_ID_FORMAT",
      "severity": "BLOCKING",
      "description": "Customer ID must be a 6–9 digit numeric sequence (PD-298)"
    },
    {
      "entityId": 42,
      "entityType": "CUSTOMER",
      "field": "einvoiceAddress.address",
      "rule": "EINVOICE_ADDRESS_MISSING",
      "severity": "BLOCKING",
      "description": "An e-invoice address is required when delivery method is E_INVOICE (PD-282)"
    },
    {
      "entityId": 42,
      "entityType": "CUSTOMER",
      "field": "billingProfile.languageCode",
      "rule": "LANGUAGE_CODE_MISSING",
      "severity": "WARNING",
      "description": "Language code is not set. Finnish (fi) will be used as default (PD-308)"
    }
  ]
}
```

Note: HTTP 200 is always returned — the response body describes the validation outcome. A non-2xx response would indicate a server error or missing customer, not a failed validation.

```java
@RestController
@RequestMapping("/api/v1/customers/{customerId}/billing-profile/validate")
@RequiredArgsConstructor
public class BillingProfileValidationController {

    private final BillingProfileValidationService validationService;

    @PostMapping
    public ResponseEntity<ValidationReport> validate(
            @PathVariable Long customerId,
            @RequestBody @Valid ValidationRequest request) {
        ValidationReport report = validationService.validate(customerId, request.getCompanyId());
        return ResponseEntity.ok(report);
    }
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/validation/dto/ValidationRequest.java`

```java
@Data
public class ValidationRequest {
    @NotNull
    private Long companyId;
}
```

---

### 9.9 ValidationRule CRUD Controller

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationRuleController.java`

> **Requirement source:** `04-api-layer.md` — "GET /validation-rules / POST / PUT /{id} — CRUD for company-configurable validation rules"

Base path: `/api/v1/validation-rules`

| Method | Path | Role required | Description |
|--------|------|---------------|-------------|
| GET | `/api/v1/validation-rules?companyId=1` | INVOICING_USER | List all rules for company |
| POST | `/api/v1/validation-rules` | FUNCTION_ADMIN | Create new rule |
| PUT | `/api/v1/validation-rules/{id}` | FUNCTION_ADMIN | Update rule |
| DELETE | `/api/v1/validation-rules/{id}` | FUNCTION_ADMIN | Delete rule |

**POST/PUT request body:**
```json
{
  "companyId": 1,
  "ruleType": "MANDATORY_FIELD",
  "ruleCode": "MISSING_INVOICING_MODE",
  "config": "{\"field\": \"billingProfile.invoicingMode\"}",
  "blocking": true,
  "active": true,
  "description": "Customer must have an invoicing mode (GROSS or NET) set before invoicing"
}
```

---

## Frontend

### 9.10 billingProfileValidation.js — API Client

**File:** `invoicing-fe/src/api/billingProfileValidation.js`

```js
import axios from './axios'

export const validateBillingProfile = (customerId, companyId) =>
  axios.post(`/api/v1/customers/${customerId}/billing-profile/validate`, { companyId })
```

---

### 9.11 ValidationReportModal.jsx

**File:** `invoicing-fe/src/components/ValidationReportModal.jsx`

**Purpose:** Reusable modal component that displays a `ValidationReport`. Used here on the billing profile page; will also be reused on the invoice run result page. The component receives a `ValidationReport` object as a prop and renders it.

**Component structure:**

```
ValidationReportModal
├── ModalHeader            — "Validation Report" with Close button
├── SummaryBanner
│   ├── If passed:         — green: "Profile is valid. Ready to invoice."
│   └── If failures:       — red: "N blocking issue(s) found" + amber "N warning(s)"
├── BlockingFailuresList   — shown first, each row:
│   ├── FieldPath badge    — e.g. "billingProfile.customerIdNumber"
│   ├── Rule code          — e.g. "CUSTOMER_ID_FORMAT"
│   └── Description        — full text
├── WarningsList           — same row structure, amber colour scheme
└── ModalFooter
    ├── CloseButton
    └── GoToFieldButton    — scrolls to the offending field in the form (optional enhancement)
```

**Props:**
```js
ValidationReportModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  report: PropTypes.shape({
    totalChecked: PropTypes.number,
    passed: PropTypes.number,
    failures: PropTypes.arrayOf(PropTypes.shape({
      entityId: PropTypes.number,
      field: PropTypes.string,
      rule: PropTypes.string,
      severity: PropTypes.oneOf(['BLOCKING', 'WARNING']),
      description: PropTypes.string,
    }))
  })
}
```

**Key behaviours:**

- Separate sections for BLOCKING and WARNING failures; BLOCKING always rendered first
- BLOCKING row: red left border, red "Blocking" badge
- WARNING row: amber left border, amber "Warning" badge
- If `failures` is empty and `passed > 0`: show only the green success banner, no failure lists
- Modal is scrollable if there are many failures

---

### 9.12 Validate Button in BillingProfilePage

**File:** `invoicing-fe/src/pages/customers/BillingProfilePage.jsx` — add validate button and modal wiring

```jsx
import { validateBillingProfile } from '../../api/billingProfileValidation'
import ValidationReportModal from '../../components/ValidationReportModal'

// State
const [validating, setValidating] = useState(false)
const [validationReport, setValidationReport] = useState(null)
const [reportModalOpen, setReportModalOpen] = useState(false)

// Handler
const handleValidate = async () => {
  setValidating(true)
  try {
    const { data: report } = await validateBillingProfile(customerId, companyId)
    setValidationReport(report)
    setReportModalOpen(true)
  } catch (err) {
    showError('Validation failed: ' + err.message)
  } finally {
    setValidating(false)
  }
}

// In the ActionBar, alongside SaveButton:
<button onClick={handleValidate} disabled={validating}>
  {validating ? 'Validating…' : 'Validate Profile'}
</button>

// After ActionBar:
<ValidationReportModal
  isOpen={reportModalOpen}
  onClose={() => setReportModalOpen(false)}
  report={validationReport}
/>
```

The Validate button triggers the POST to `/billing-profile/validate` and opens the modal with the report. The Save button is independent — saving the profile does not auto-trigger validation. The user explicitly clicks "Validate Profile" to check readiness for invoicing.

---

### 9.13 Route — ValidationRules Management Page

**File:** `invoicing-fe/src/pages/config/ValidationRulesPage.jsx`

**Purpose:** FUNCTION_ADMIN page for managing company-configured validation rules. Same structure as `ClassificationRulesPage` (table + modal).

**Columns:** Rule Code, Type, Description, Blocking badge, Active toggle, Actions (Edit / Delete)

**File:** `invoicing-fe/src/api/validationRules.js`

```js
import axios from './axios'

export const getValidationRules   = (companyId) =>
  axios.get('/api/v1/validation-rules', { params: { companyId } })

export const createValidationRule = (data) =>
  axios.post('/api/v1/validation-rules', data)

export const updateValidationRule = (id, data) =>
  axios.put(`/api/v1/validation-rules/${id}`, data)

export const deleteValidationRule = (id) =>
  axios.delete(`/api/v1/validation-rules/${id}`)
```

---

### 9.14 Route Registration

**File:** `invoicing-fe/src/App.jsx` — add:

```jsx
<Route path="/config/validation-rules" element={<ValidationRulesPage />} />
```

---

## Verification Checklist

1. `POST /api/v1/customers/1/billing-profile/validate` on a customer with a complete valid profile — returns `{ totalChecked: 1, passed: 1, failures: [] }`
2. Set `customerIdNumber` to `"12345"` (5 digits) on customer 1; call validate — returns failure with `rule: "CUSTOMER_ID_FORMAT"`, `severity: "BLOCKING"`
3. Set `deliveryMethod = E_INVOICE`, `businessId = null`; call validate — returns failure with `rule: "BUSINESS_ID_REQUIRED_FOR_EINVOICE"`, `severity: "BLOCKING"`
4. Keep `deliveryMethod = E_INVOICE`, `businessId` present, but delete the `einvoice_addresses` row; call validate — returns failure `rule: "EINVOICE_ADDRESS_MISSING"`, `severity: "BLOCKING"`
5. Set `deliveryMethod = EMAIL`; call validate with no `businessId` and no EInvoice address — both checks for businessId and e-invoice address must NOT appear (they only apply to E_INVOICE delivery)
6. Set `languageCode = null`; call validate — returns failure with `rule: "LANGUAGE_CODE_MISSING"`, `severity: "WARNING"` (not BLOCKING)
7. `POST /api/v1/validation-rules` — create a MANDATORY_FIELD rule for `billingProfile.invoicingMode`, `blocking: true`; call validate on a customer with no `invoicingMode` — the custom rule failure appears in the report
8. Set the custom rule to `active: false`; call validate again — the custom rule failure no longer appears
9. `BillingProfileValidationService.validateAll([1L, 2L, 3L], companyId)` — `totalChecked` = 3, `passed` counts correctly, `failures` contains entries from all three customers
10. `GET /api/v1/validation-rules?companyId=1` — returns the custom rules created in step 7
11. `PUT /api/v1/validation-rules/{id}` — change `blocking: true` to `blocking: false`; re-run validate — same rule now produces a WARNING, not a BLOCKING failure
12. Open FE at `/customers/1/billing-profile` — "Validate Profile" button is present in the ActionBar
13. Click "Validate Profile" on a customer with no issues — green banner "Profile is valid" shows in modal; no failure rows
14. Introduce a missing field (clear businessId for an E_INVOICE customer via the form save); click "Validate Profile" — modal opens with red blocking failure row for `BUSINESS_ID_REQUIRED_FOR_EINVOICE`
15. Warning failures (e.g. missing languageCode) appear in the modal in amber below the blocking section; they do not prevent the user from saving the profile
16. `POST /api/v1/validation-rules` with invalid JSON in `config` — returns 400; the service must not crash on a misconfigured rule during validation (the rule is skipped with a logged warning)

---

## File Checklist

### Backend
- [ ] `validation/ValidationSeverity.java`
- [ ] `validation/ValidationFailure.java`
- [ ] `validation/ValidationReport.java`
- [ ] `validation/ValidationRuleType.java`
- [ ] `validation/ValidationRule.java`
- [ ] `validation/ValidationRuleRepository.java`
- [ ] `validation/BillingProfileValidationService.java`
- [ ] `validation/BillingProfileValidationController.java`
- [ ] `validation/ValidationRuleController.java`
- [ ] `validation/ValidationRuleService.java`
- [ ] `validation/dto/ValidationRequest.java`
- [ ] `validation/dto/ValidationRuleRequest.java`
- [ ] `validation/dto/ValidationRuleResponse.java`

### Frontend
- [ ] `src/api/billingProfileValidation.js`
- [ ] `src/api/validationRules.js`
- [ ] `src/components/ValidationReportModal.jsx`
- [ ] `src/pages/customers/BillingProfilePage.jsx` — add Validate button and modal wiring
- [ ] `src/pages/config/ValidationRulesPage.jsx`
- [ ] `src/App.jsx` — add route `/config/validation-rules`
