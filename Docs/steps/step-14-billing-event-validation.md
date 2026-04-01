# Step 14 — BillingEvent Validation

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingEventValidationService
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 6: "Validation Report Pattern"
- `Docs/structured_breakdown/04-api-layer.md` → `POST /billing-events/validate`

---

## Goal

Implement `BillingEventValidationService` and the `POST /api/v1/billing-events/validate` endpoint. This service runs pre-flight checks on a set of billing events before they enter an invoice run, returning a `ValidationReport` that categorises failures by severity (BLOCKING vs WARNING). The same service is also called internally by `InvoiceRunService`, so it must be designed as a reusable component.

---

## Backend

### 14.1 ValidationSeverity Enum

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationSeverity.java`

> **Requirement source:** `06-cross-cutting.md` — "Blocking failures prevent progression. Warning failures are reported but do not block."

```java
public enum ValidationSeverity {
    BLOCKING,   // Invoice cannot proceed — must be corrected
    WARNING     // Reported but does not block invoicing
}
```

---

### 14.2 ValidationFailure DTO

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationFailure.java`

> **Requirement source:** `06-cross-cutting.md` Rule 6 — "Each failure has: entity ID, entity type, rule that failed, severity, description of the issue."

```java
@Data
@Builder
public class ValidationFailure {
    private Long entityId;
    private String entityType;           // "BILLING_EVENT"
    private String ruleCode;             // e.g. "MISSING_PRODUCT", "NEGATIVE_PRICE"
    private ValidationSeverity severity;
    private String description;          // Human-readable, suitable for display in UI
}
```

---

### 14.3 ValidationReport DTO

**File:** `invoicing/src/main/java/com/example/invoicing/validation/ValidationReport.java`

> **Requirement source:** `06-cross-cutting.md` Rule 6 — "ValidationReport contains: totalChecked, passed, failures (list)."

```java
@Data
@Builder
public class ValidationReport {
    private int totalChecked;
    private int passed;
    private int blockingFailureCount;
    private int warningCount;
    private List<ValidationFailure> failures;

    public boolean hasBlockingFailures() {
        return failures.stream()
            .anyMatch(f -> f.getSeverity() == ValidationSeverity.BLOCKING);
    }
}
```

---

### 14.4 BillingEventValidationService

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/validation/BillingEventValidationService.java`

> **Requirement source:** `03-business-logic.md` — "Pre-flight validation at the event level — before events enter a run. Returns a list of events with missing mandatory fields." (PD-278)

The service runs four categories of checks, each matching one of the `ValidationRule` types from the domain model:

1. **MANDATORY_FIELD** — product, date, customer number, prices must all be present
2. **PRICE_CONSISTENCY** — no negative prices unless the event is a credit (nonBillable = true)
3. **QUANTITY_THRESHOLD** — quantity and weight must not exceed configured maximums
4. **CLASSIFICATION** — legal classification must be set and valid

```java
@Service
@RequiredArgsConstructor
public class BillingEventValidationService {

    private final BillingEventRepository billingEventRepository;
    private final ValidationRuleRepository validationRuleRepository;

    /**
     * Validates a list of event IDs.
     * Called by: POST /api/v1/billing-events/validate (user-triggered)
     *            InvoiceRunService (automatic pre-run check)
     */
    public ValidationReport validate(List<Long> eventIds) {
        List<BillingEvent> events = billingEventRepository.findAllById(eventIds);
        List<ValidationRule> activeRules = validationRuleRepository.findAllActiveForCompany();

        List<ValidationFailure> failures = new ArrayList<>();
        int passed = 0;

        for (BillingEvent event : events) {
            List<ValidationFailure> eventFailures = validateEvent(event, activeRules);
            if (eventFailures.isEmpty()) {
                passed++;
            } else {
                failures.addAll(eventFailures);
            }
        }

        return ValidationReport.builder()
            .totalChecked(events.size())
            .passed(passed)
            .blockingFailureCount((int) failures.stream()
                .filter(f -> f.getSeverity() == ValidationSeverity.BLOCKING).count())
            .warningCount((int) failures.stream()
                .filter(f -> f.getSeverity() == ValidationSeverity.WARNING).count())
            .failures(failures)
            .build();
    }

    /**
     * Validates a single event — used by InvoiceGenerationService per-event.
     */
    public List<ValidationFailure> validateEvent(BillingEvent event, List<ValidationRule> rules) {
        List<ValidationFailure> failures = new ArrayList<>();

        for (ValidationRule rule : rules) {
            switch (rule.getRuleType()) {
                case MANDATORY_FIELD  -> checkMandatoryField(event, rule, failures);
                case PRICE_CONSISTENCY -> checkPriceConsistency(event, rule, failures);
                case QUANTITY_THRESHOLD -> checkQuantityThreshold(event, rule, failures);
                case CLASSIFICATION    -> checkClassification(event, rule, failures);
            }
        }

        return failures;
    }

    // -----------------------------------------------------------------------
    // MANDATORY FIELD CHECK
    // "An event must have mandatory data, such as accounts and cost centres,
    // in order for it to be transferred to billing." (PD-278)
    // -----------------------------------------------------------------------
    private void checkMandatoryField(BillingEvent event, ValidationRule rule,
            List<ValidationFailure> failures) {
        String fieldName = (String) rule.getConfigValue("field");

        boolean missing = switch (fieldName) {
            case "product"           -> event.getProduct() == null;
            case "eventDate"         -> event.getEventDate() == null;
            case "customerNumber"    -> event.getCustomerNumber() == null
                                        || event.getCustomerNumber().isBlank();
            case "wasteFeePrice"     -> event.getWasteFeePrice() == null;
            case "transportFeePrice" -> event.getTransportFeePrice() == null;
            case "ecoFeePrice"       -> event.getEcoFeePrice() == null;
            case "accountingAccount" -> event.getAccountingAccount() == null;
            case "costCenter"        -> event.getCostCenter() == null;
            case "legalClassification" -> event.getLegalClassification() == null;
            default -> false;
        };

        if (missing) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("MISSING_" + fieldName.toUpperCase())
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Field '" + fieldName + "' is required but missing on event " + event.getId())
                .build());
        }
    }

    // -----------------------------------------------------------------------
    // PRICE CONSISTENCY CHECK
    // "No negative prices unless credit" — negative prices on a standard event
    // indicate a data entry error.
    // -----------------------------------------------------------------------
    private void checkPriceConsistency(BillingEvent event, ValidationRule rule,
            List<ValidationFailure> failures) {
        if (event.isNonBillable()) return;   // credits may have negative values

        List<String> negativeFields = new ArrayList<>();
        if (event.getWasteFeePrice() != null && event.getWasteFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("wasteFeePrice");
        if (event.getTransportFeePrice() != null && event.getTransportFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("transportFeePrice");
        if (event.getEcoFeePrice() != null && event.getEcoFeePrice().compareTo(BigDecimal.ZERO) < 0)
            negativeFields.add("ecoFeePrice");

        for (String field : negativeFields) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("NEGATIVE_PRICE")
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Price field '" + field + "' is negative on event " + event.getId()
                    + ". Negative prices are only valid for credit events (nonBillable = true).")
                .build());
        }
    }

    // -----------------------------------------------------------------------
    // QUANTITY THRESHOLD CHECK
    // "An employee builds an automation where an alert is triggered for
    //  billing events with more than 30 container collections." (PD-278 use case)
    // -----------------------------------------------------------------------
    private void checkQuantityThreshold(BillingEvent event, ValidationRule rule,
            List<ValidationFailure> failures) {
        String field = (String) rule.getConfigValue("field");
        BigDecimal threshold = new BigDecimal(rule.getConfigValue("threshold").toString());

        BigDecimal value = "weight".equals(field) ? event.getWeight() : event.getQuantity();

        if (value != null && value.compareTo(threshold) > 0) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("QUANTITY_THRESHOLD_EXCEEDED")
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description(field + " value " + value + " exceeds configured threshold of "
                    + threshold + " on event " + event.getId())
                .build());
        }
    }

    // -----------------------------------------------------------------------
    // CLASSIFICATION CHECK
    // "Mandatory reporting information — legal classification — is automatically
    //  assigned." (PD-289) — here we verify it was actually assigned.
    // -----------------------------------------------------------------------
    private void checkClassification(BillingEvent event, ValidationRule rule,
            List<ValidationFailure> failures) {
        if (event.getLegalClassification() == null) {
            failures.add(ValidationFailure.builder()
                .entityId(event.getId())
                .entityType("BILLING_EVENT")
                .ruleCode("MISSING_CLASSIFICATION")
                .severity(rule.isBlocking() ? ValidationSeverity.BLOCKING : ValidationSeverity.WARNING)
                .description("Legal classification (PUBLIC_LAW/PRIVATE_LAW) is not set on event "
                    + event.getId())
                .build());
        }
    }
}
```

---

### 14.5 Validate Endpoint in BillingEventController

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventController.java` (add to existing)

```java
/**
 * POST /api/v1/billing-events/validate
 * Pre-billing validation for a list of event IDs.
 * Returns a ValidationReport — does NOT modify any events.
 * Role: INVOICING_USER
 */
@PostMapping("/validate")
public ValidationReport validate(@RequestBody @Valid ValidateEventsRequest request) {
    return billingEventValidationService.validate(request.getEventIds());
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/ValidateEventsRequest.java`

```java
@Data
public class ValidateEventsRequest {
    @NotEmpty
    private List<Long> eventIds;
}
```

**Request/Response example:**

`POST /api/v1/billing-events/validate`
```json
{
  "eventIds": [1001, 1002, 1003]
}
```

Response `200 OK` — all pass:
```json
{
  "totalChecked": 3,
  "passed": 3,
  "blockingFailureCount": 0,
  "warningCount": 0,
  "failures": []
}
```

Response `200 OK` — mixed results:
```json
{
  "totalChecked": 3,
  "passed": 1,
  "blockingFailureCount": 1,
  "warningCount": 1,
  "failures": [
    {
      "entityId": 1002,
      "entityType": "BILLING_EVENT",
      "ruleCode": "MISSING_accountingAccount",
      "severity": "BLOCKING",
      "description": "Field 'accountingAccount' is required but missing on event 1002"
    },
    {
      "entityId": 1003,
      "entityType": "BILLING_EVENT",
      "ruleCode": "QUANTITY_THRESHOLD_EXCEEDED",
      "severity": "WARNING",
      "description": "quantity value 45.0 exceeds configured threshold of 30 on event 1003"
    }
  ]
}
```

---

## Frontend

### 14.6 Validate Button on BillingEvents List

On `BillingEventsPage.jsx` (Step 10), add:
- Row-level checkboxes to multi-select events
- A "Validate Selected" button in the toolbar, enabled only when at least one event is selected

**File:** `invoicing-fe/src/pages/billing/BillingEventsPage.jsx` (extend existing)

```jsx
// In the toolbar area:
<button
  disabled={selectedIds.length === 0}
  onClick={() => setValidateModalOpen(true)}
>
  Validate Selected ({selectedIds.length})
</button>
```

---

### 14.7 ValidationReport Modal

**File:** `invoicing-fe/src/components/billing/ValidationReportModal.jsx`

Triggered when the "Validate Selected" button is clicked. Calls the validate endpoint and shows the results.

**Loading state:** "Running validation checks…"

**Results display:**
- Summary bar: "3 checked — 1 passed, 1 blocking issue, 1 warning"
- **Blocking issues table** (shown first, red header):
  - Event ID, Rule, Description
- **Warnings table** (yellow header, collapsible):
  - Event ID, Rule, Description
- "All Clear" state (green banner): shown when `blockingFailureCount === 0 && warningCount === 0`

**Actions at bottom of modal:**
- "Close" — dismisses modal
- "Fix Issues" — closes modal and scrolls to the first failing event in the list

---

### 14.8 API helper

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const validateBillingEvents = (eventIds) =>
  axios.post('/api/v1/billing-events/validate', { eventIds })
```

---

## Verification Checklist

1. `POST /api/v1/billing-events/validate` with an event that has `accountingAccount = null` — verify BLOCKING failure with `ruleCode: "MISSING_accountingAccount"`.
2. `POST /api/v1/billing-events/validate` with an event that has `wasteFeePrice = -5.00` and `nonBillable = false` — verify BLOCKING failure with `ruleCode: "NEGATIVE_PRICE"`.
3. `POST /api/v1/billing-events/validate` with an event where `quantity = 50` and a QUANTITY_THRESHOLD rule configured at 30 (WARNING) — verify WARNING failure, NOT blocking.
4. `POST /api/v1/billing-events/validate` with an event that has `legalClassification = null` — verify BLOCKING failure with `ruleCode: "MISSING_CLASSIFICATION"`.
5. `POST /api/v1/billing-events/validate` with a fully valid event — verify `passed: 1`, `failures: []`.
6. `POST /api/v1/billing-events/validate` with an empty `eventIds` array — verify 400 validation error.
7. Validate a mix of 3 events (1 clean, 1 blocking, 1 warning) — verify `totalChecked: 3`, `passed: 1`, `blockingFailureCount: 1`, `warningCount: 1`.
8. The validation service does NOT modify any event in the database (re-fetch events after validation, all fields unchanged).
9. Frontend: select 2 events on the list page, click "Validate Selected" — modal opens showing results.
10. Frontend: "All Clear" banner displays when all selected events are valid.

---

## File Checklist

### Backend
- [ ] `validation/ValidationSeverity.java`
- [ ] `validation/ValidationFailure.java`
- [ ] `validation/ValidationReport.java`
- [ ] `billingevent/validation/BillingEventValidationService.java`
- [ ] `billingevent/dto/ValidateEventsRequest.java`
- [ ] `billingevent/BillingEventController.java` — add `POST /validate` endpoint

### Frontend
- [ ] `src/components/billing/ValidationReportModal.jsx`
- [ ] `src/pages/billing/BillingEventsPage.jsx` — add checkboxes, validate button, modal integration
- [ ] `src/api/billingEvents.js` — add `validateBillingEvents`
