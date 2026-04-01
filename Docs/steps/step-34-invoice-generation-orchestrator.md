# Step 34 — Invoice Generation Orchestrator

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceGenerationService, InvoiceSimulationService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoices — POST /invoices/preview
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: "Simulation Mode Guard", Rule 4: "Status Gate"

---

## Goal
Implement `InvoiceGenerationService`, which orchestrates the full invoice production pipeline across 11 ordered steps from profile validation through FINVOICE XML generation and final save. A dedicated preview endpoint runs the entire pipeline in simulation mode, returning a `SimulationReport` without persisting anything or consuming invoice numbers. The FE "Generate Invoice" button triggers preview first, then a separate confirm call to commit.

---

## Backend

### 34.1 InvoiceGenerationService

**File:** `invoicing/src/main/java/com/example/invoicing/generation/InvoiceGenerationService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceGenerationService — the orchestrator

The 11-step pipeline (see service code outline):

```java
@Service
@Transactional
public class InvoiceGenerationService {

    /**
     * Generate an invoice for one customer's event group.
     * @param events       The BillingEvents to invoice (already filtered/grouped by the caller)
     * @param customerId   The target customer
     * @param simulationMode  When true: skip steps 8 (number assignment) and 10 (FINVOICE transmission)
     */
    public InvoiceGenerationResult generate(
            List<BillingEvent> events,
            Long customerId,
            boolean simulationMode) {

        // STEP 1 — Validate billing profile completeness (PD-298)
        BillingProfileValidationResult profileCheck =
            billingProfileValidationService.validate(customerId);
        if (!profileCheck.isValid()) {
            return InvoiceGenerationResult.profileError(customerId, profileCheck.getIssues());
        }

        // STEP 2 — Group events by bundling rules (PD-290)
        List<InvoiceLineItem> lineItems = invoiceBundlingService.bundle(events, customerId);

        // STEP 3 — Apply legal classification per line (PD-285)
        for (InvoiceLineItem line : lineItems) {
            LegalClassification classification =
                legalClassificationService.classify(line.getSourceEvent());
            line.setLegalClassification(classification);
        }

        // STEP 4 — Accounting allocation per line (PD-295)
        for (InvoiceLineItem line : lineItems) {
            accountingAllocationService.allocate(line);
        }

        // STEP 5 — Apply surcharges (PD-294)
        billingeSurchargeService.applySurcharge(lineItems, customerId);

        // STEP 6 — Apply minimum fee if needed (PD-286)
        minimumFeeService.applyIfNeeded(lineItems, customerId);

        // STEP 7 — Shared service distribution (PD-280) for shared-service events
        lineItems = applySharedServiceDistribution(lineItems, events);

        // STEP 8 — Assign invoice number (SKIP in simulation mode) (PD-309)
        String invoiceNumber = null;
        if (!simulationMode) {
            invoiceNumber = invoiceNumberingService.assignNext(customerId, lineItems);
        }

        // STEP 9 — Build FINVOICE XML (PD-310)
        String finvoiceXml = finvoiceBuilderService.build(lineItems, customerId, invoiceNumber);

        // STEP 10 — Run validation engine (PD-271)
        ValidationReport validationReport = invoiceValidationEngine.validate(lineItems, finvoiceXml);
        if (validationReport.hasBlockingFailures()) {
            return InvoiceGenerationResult.validationError(customerId, validationReport);
        }

        // STEP 11 — Save invoice (SKIP in simulation mode — return in-memory object)
        if (!simulationMode) {
            Invoice saved = buildAndSave(lineItems, customerId, invoiceNumber, finvoiceXml);
            return InvoiceGenerationResult.success(saved);
        } else {
            Invoice preview = buildInMemory(lineItems, customerId, finvoiceXml);
            return InvoiceGenerationResult.simulation(preview, validationReport);
        }
    }
}
```

**`InvoiceGenerationResult`** (value object — not an entity):
```java
public class InvoiceGenerationResult {
    private final boolean success;
    private final Invoice invoice;           // null on failure
    private final List<String> profileErrors;
    private final ValidationReport validationReport;
    private final boolean simulation;

    // Static factory methods:
    static InvoiceGenerationResult success(Invoice invoice) { ... }
    static InvoiceGenerationResult simulation(Invoice preview, ValidationReport report) { ... }
    static InvoiceGenerationResult profileError(Long customerId, List<String> issues) { ... }
    static InvoiceGenerationResult validationError(Long customerId, ValidationReport report) { ... }
}
```

---

### 34.2 SimulationReport

**File:** `invoicing/src/main/java/com/example/invoicing/generation/SimulationReport.java`

> **Requirement source:** `03-business-logic.md` — InvoiceSimulationService → SimulationReport

```java
public class SimulationReport {
    private int totalCustomers;
    private int totalInvoices;
    private BigDecimal totalNetAmount;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalVatAmount;
    private List<ValidationFailureEntry> validationFailures; // per invoice
    private List<InvoicePreviewEntry> sampleLineItems;       // first 5 invoices for review
    private boolean simulationMode = true;
}

public class ValidationFailureEntry {
    private Long customerId;
    private String customerName;
    private String ruleType;       // e.g. MANDATORY_FIELD
    private Severity severity;     // BLOCKING or WARNING
    private String description;
}

public class InvoicePreviewEntry {
    private Long customerId;
    private String customerName;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private int lineItemCount;
    private List<InvoiceLineItemResponse> lineItems;
}
```

---

### 34.3 InvoicePreviewController

**File:** `invoicing/src/main/java/com/example/invoicing/generation/InvoicePreviewController.java`

Base path: `/api/v1/invoices`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/preview` | Single-customer simulation — returns preview without saving | INVOICING_USER |

**POST /api/v1/invoices/preview request:**
```json
{
  "customerId": 1001,
  "billingPeriodFrom": "2024-01-01",
  "billingPeriodTo": "2024-01-31",
  "eventIds": [5001, 5002, 5003]
}
```

`eventIds` is optional; if omitted, all IN_PROGRESS events for the customer in the period are used.

**POST /api/v1/invoices/preview response (SimulationReport for a single customer):**
```json
{
  "simulationMode": true,
  "totalCustomers": 1,
  "totalInvoices": 1,
  "totalNetAmount": 120.00,
  "totalGrossAmount": 148.80,
  "totalVatAmount": 28.80,
  "validationFailures": [],
  "sampleLineItems": [
    {
      "customerId": 1001,
      "customerName": "Virtanen Oy",
      "netAmount": 120.00,
      "grossAmount": 148.80,
      "lineItemCount": 3,
      "lineItems": [
        {
          "description": "Waste container emptying",
          "quantity": 4,
          "unitPrice": 20.00,
          "vatRate": 24.00,
          "netAmount": 80.00,
          "legalClassification": "PUBLIC_LAW",
          "bundled": true
        }
      ]
    }
  ]
}
```

---

## Frontend

### 34.4 Generate Invoice Workflow

**File:** `invoicing-fe/src/pages/invoices/GenerateInvoicePage.jsx`

**Two-step flow:**

**Step 1 — Preview (simulation):**
- User selects customer, billing period, optionally specific events.
- Clicks "Preview Invoice" → calls `POST /api/v1/invoices/preview`.
- Page shows `SimulationPreviewPanel` with all line items, totals, VAT breakdown, and any validation warnings.
- If there are blocking validation failures, a red warning block is shown and the "Confirm & Generate" button is disabled.

**Step 2 — Confirm:**
- User reviews the preview and clicks "Confirm & Generate".
- Calls `POST /api/v1/invoice-runs` (step 38/41) with `simulationMode: false` scoped to the previewed customer.
- On success: navigates to the invoice detail page.

**Components:**
- **GenerateInvoiceForm** — customer selector, date range pickers, optional event multi-select.
- **SimulationPreviewPanel** — shows line items table, amount totals, VAT breakdown table, validation failure list.
- **ValidationFailuresList** — renders blocking failures in red, warnings in amber.
- **ConfirmGenerateButton** — disabled if blocking failures exist.

**API calls via `src/api/invoices.js` (additions):**
```js
export const previewInvoice = (data) => axios.post('/api/v1/invoices/preview', data)
```

---

## Verification Checklist

1. Call `POST /api/v1/invoices/preview` for a customer with 3 IN_PROGRESS events → response includes correct line items, net/gross totals, no invoice persisted, no invoice number assigned.
2. Verify step ordering: a customer with incomplete billing profile returns `profileErrors` in the result before any line items are generated.
3. Run with `simulationMode: true` → no `Invoice` record created in the database; `BillingEvent.status` unchanged.
4. Run with `simulationMode: false` → `Invoice` record saved with status `DRAFT`; invoice number assigned; `BillingEvent.status` → `SENT`.
5. Trigger a blocking validation failure (e.g. missing cost center) — `InvoiceGenerationResult.validationError` returned; no invoice saved.
6. Shared-service events: verify `distributeEvent()` is called and produces N lines (one per participant).
7. Minimum fee check: set a `MinimumFeeConfig` higher than the event total; verify an adjustment line item is added.
8. Surcharge: set `InvoicingSurchargeConfig` for PAPER delivery; create a customer with PAPER delivery → surcharge line appears.
9. Open `GenerateInvoicePage` in FE; preview returns and renders correctly; blocking failure disables Confirm button.
10. After confirming, navigate to invoice detail page and verify all line items match the preview.

---

## File Checklist

### Backend
- [ ] `generation/InvoiceGenerationService.java`
- [ ] `generation/InvoiceGenerationResult.java`
- [ ] `generation/SimulationReport.java`
- [ ] `generation/InvoicePreviewController.java`
- [ ] `generation/dto/InvoicePreviewRequest.java`
- [ ] `generation/dto/InvoicePreviewEntry.java`
- [ ] `generation/dto/ValidationFailureEntry.java`

### Frontend
- [ ] `src/pages/invoices/GenerateInvoicePage.jsx`
- [ ] `src/pages/invoices/components/GenerateInvoiceForm.jsx`
- [ ] `src/pages/invoices/components/SimulationPreviewPanel.jsx`
- [ ] `src/pages/invoices/components/ValidationFailuresList.jsx`
- [ ] `src/pages/invoices/components/ConfirmGenerateButton.jsx`
