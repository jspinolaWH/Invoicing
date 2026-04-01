# Step 40 — Invoice Simulation Service

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceSimulationService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoice Runs — POST /invoice-runs/simulate
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: "Simulation Mode Guard" — explicit list of what runs/does-not-run in simulation

---

## Goal
Implement `InvoiceSimulationService`, which executes the complete invoice generation pipeline — bundling, classification, allocation, VAT calculation, surcharges, minimum fee, shared service splits, and validation — but deliberately skips invoice number assignment, status changes, external transmission, and audit logging for status changes. Returns a `SimulationReport` with totals, validation failures, and sample line items. The FE simulation results page shows the report before the user commits a real run.

---

## Backend

### 40.1 InvoiceSimulationService

**File:** `invoicing/src/main/java/com/example/invoicing/simulation/InvoiceSimulationService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceSimulationService
> **Requirement source:** `06-cross-cutting.md` — Rule 3: "Things that run ONLY in real mode (never in simulation): assigning invoice numbers, changing BillingEvent status, transmitting FINVOICE, writing to audit logs for status changes"

```java
@Service
public class InvoiceSimulationService {

    /**
     * Run the full invoice generation pipeline for all customers matching the filter.
     * No data is persisted. No invoice numbers are consumed. No statuses change.
     * Returns a complete SimulationReport.
     */
    @Transactional(readOnly = true)
    public SimulationReport simulate(InvoiceRunRequest request) {
        SimulationReport report = new SimulationReport();
        report.setSimulationMode(true);

        // 1. Fetch all IN_PROGRESS, non-excluded events matching the filter
        List<BillingEvent> allEvents = billingEventRepository
            .findByRunFilter(request.getFilterCriteria());

        // 2. Group events by customer
        Map<Long, List<BillingEvent>> byCustomer = allEvents.stream()
            .collect(Collectors.groupingBy(e -> e.getCustomer().getId()));

        report.setTotalCustomers(byCustomer.size());

        List<InvoicePreviewEntry> previews = new ArrayList<>();
        List<ValidationFailureEntry> allFailures = new ArrayList<>();

        for (Map.Entry<Long, List<BillingEvent>> entry : byCustomer.entrySet()) {
            Long customerId = entry.getKey();
            List<BillingEvent> events = entry.getValue();

            // 3. Run generation in simulation mode — no side effects
            InvoiceGenerationResult result = invoiceGenerationService
                .generate(events, customerId, true /* simulationMode */);

            if (result.isSuccess()) {
                Invoice preview = result.getInvoice();
                previews.add(InvoicePreviewEntry.from(preview));
                report.incrementTotalInvoices();
                report.addToTotalNet(preview.getNetAmount());
                report.addToTotalGross(preview.getGrossAmount());
                report.addToTotalVat(preview.getVatAmount());

                // Collect warnings from validation report
                allFailures.addAll(result.getValidationReport().getWarnings()
                    .stream().map(w -> ValidationFailureEntry.warning(customerId, w)).toList());
            } else {
                // Collect blocking failures
                allFailures.addAll(result.getValidationReport().getBlockingFailures()
                    .stream().map(f -> ValidationFailureEntry.blocking(customerId, f)).toList());
                report.incrementFailedCustomers();
            }
        }

        report.setValidationFailures(allFailures);
        // Return first 5 invoice previews as sample for UI display
        report.setSampleLineItems(previews.stream().limit(5).toList());

        return report;
    }

    /**
     * Single-customer simulation — used by the preview endpoint (step 34).
     */
    @Transactional(readOnly = true)
    public SimulationReport simulateForCustomer(Long customerId, LocalDate periodFrom,
                                                 LocalDate periodTo, List<Long> eventIds) {
        List<BillingEvent> events = eventIds != null && !eventIds.isEmpty()
            ? billingEventRepository.findAllById(eventIds)
            : billingEventRepository.findByCustomerIdAndDateRangeAndStatus(
                customerId, periodFrom, periodTo, BillingEventStatus.IN_PROGRESS);

        InvoiceGenerationResult result =
            invoiceGenerationService.generate(events, customerId, true);

        SimulationReport report = new SimulationReport();
        report.setSimulationMode(true);
        report.setTotalCustomers(1);
        if (result.isSuccess()) {
            report.setTotalInvoices(1);
            report.setTotalNetAmount(result.getInvoice().getNetAmount());
            report.setTotalGrossAmount(result.getInvoice().getGrossAmount());
            report.setTotalVatAmount(result.getInvoice().getVatAmount());
            report.setSampleLineItems(List.of(InvoicePreviewEntry.from(result.getInvoice())));
        }
        report.setValidationFailures(mapFailures(customerId, result));
        return report;
    }
}
```

---

### 40.2 What Is Skipped in Simulation Mode

The simulation guard in `InvoiceGenerationService` (step 34) checks `simulationMode` before each of these operations:

| Operation | Real Run | Simulation |
|-----------|----------|------------|
| Event grouping and filtering | YES | YES |
| Bundling rules | YES | YES |
| Legal classification | YES | YES |
| Accounting allocation | YES | YES |
| VAT calculation | YES | YES |
| Minimum fee check | YES | YES |
| Surcharge application | YES | YES |
| Shared service splits | YES | YES |
| Validation rules | YES | YES |
| Invoice number assignment | YES | **SKIPPED** |
| BillingEvent status → SENT | YES | **SKIPPED** |
| Invoice status beyond DRAFT | YES | **SKIPPED** |
| FINVOICE transmission | YES | **SKIPPED** |
| Audit log for status changes | YES | **SKIPPED** |
| Persisting Invoice to DB | YES | **SKIPPED** |

---

### 40.3 SimulationController

**File:** `invoicing/src/main/java/com/example/invoicing/simulation/SimulationController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoice-runs/simulate` | Run full simulation, return SimulationReport | INVOICING_USER |

**POST /api/v1/invoice-runs/simulate request:**
```json
{
  "filterCriteria": {
    "municipality": "Tampere",
    "periodFrom": "2024-01-01",
    "periodTo": "2024-01-31",
    "minAmount": 5.00
  }
}
```

**POST /api/v1/invoice-runs/simulate response:**
```json
{
  "simulationMode": true,
  "totalCustomers": 312,
  "totalInvoices": 310,
  "failedCustomers": 2,
  "totalNetAmount": 38420.00,
  "totalGrossAmount": 47640.80,
  "totalVatAmount": 9220.80,
  "validationFailures": [
    {
      "customerId": 99,
      "customerName": "Korhonen Oy",
      "severity": "BLOCKING",
      "ruleType": "MANDATORY_FIELD",
      "description": "costCenter is null on event 5044"
    },
    {
      "customerId": 107,
      "customerName": "Mäkinen",
      "severity": "WARNING",
      "ruleType": "QUANTITY_THRESHOLD",
      "description": "Quantity 35 exceeds threshold of 30 on event 5099"
    }
  ],
  "sampleLineItems": [
    {
      "customerId": 1,
      "customerName": "Virtanen Oy",
      "netAmount": 120.00,
      "grossAmount": 148.80,
      "lineItemCount": 3,
      "lineItems": [...]
    }
  ]
}
```

---

## Frontend

### 40.4 Simulation Results Page

**File:** `invoicing-fe/src/pages/runs/SimulationResultsPage.jsx`

Components:
- **SimulationSummaryCard** — total customers, total invoices, failed customers, net total, VAT total, gross total. All amounts formatted in euros.
- **ValidationFailuresTable** — columns: Customer Name, Severity (RED for BLOCKING, AMBER for WARNING), Rule Type, Description. Sortable by severity. BLOCKING rows at the top.
- **SampleInvoicesAccordion** — expandable accordion showing up to 5 sample invoice previews with their line items table.
- **CommitRunButton** — enabled only if `validationFailures.filter(f => f.severity === 'BLOCKING').length === 0`. Clicking this submits a real `POST /api/v1/invoice-runs` (not simulate) with the same filter criteria.
- **BlockingFailureBanner** — shown above the CommitRunButton when blocking failures exist: "Cannot start real run: X blocking validation failures must be resolved first."

**Flow in UI:**
1. User fills in the Run Filter Form (from step 38).
2. Clicks "Simulate First" → calls `POST /api/v1/invoice-runs/simulate`.
3. SimulationResultsPage renders.
4. If no blocking failures: "Commit Run" button is active.
5. Clicking "Commit Run" calls `POST /api/v1/invoice-runs` (real run) and navigates to `InvoiceRunDetailPage`.

**API calls (additions to `src/api/invoiceRuns.js`):**
```js
export const simulate = (filterCriteria) =>
  axios.post('/api/v1/invoice-runs/simulate', { filterCriteria })
```

---

## Verification Checklist

1. `POST /api/v1/invoice-runs/simulate` — returns `SimulationReport` with correct totals; verify no `Invoice` entities are created in the database.
2. Verify no `BillingEvent.status` values change after a simulation run.
3. Verify no invoice numbers are consumed from `InvoiceNumberSeries` after simulation.
4. Simulation with a billing profile validation error — appears in `validationFailures` with `severity: BLOCKING`; that customer's events are excluded from totals.
5. Simulation with a quantity threshold warning — appears in `validationFailures` with `severity: WARNING`; the invoice for that customer is still included in totals.
6. `sampleLineItems` contains at most 5 entries regardless of how many customers were simulated.
7. `totalNetAmount` equals the sum of all successful preview invoice `netAmount` values (verify with manual addition in test).
8. `@Transactional(readOnly = true)` — verify simulation does not open any write transactions.
9. Open `SimulationResultsPage` in FE — blocking failures show in red at top of `ValidationFailuresTable`; `CommitRunButton` is disabled.
10. Remove the blocking failure (fix the event's cost center), re-simulate → `CommitRunButton` becomes active; clicking it starts a real run.

---

## File Checklist

### Backend
- [ ] `simulation/InvoiceSimulationService.java`
- [ ] `simulation/SimulationController.java`
- [ ] `generation/SimulationReport.java` (may already exist from step 34 — reuse)
- [ ] `generation/InvoicePreviewEntry.java` (may already exist from step 34)
- [ ] `generation/ValidationFailureEntry.java` (may already exist from step 34)

### Frontend
- [ ] `src/pages/runs/SimulationResultsPage.jsx`
- [ ] `src/pages/runs/components/SimulationSummaryCard.jsx`
- [ ] `src/pages/runs/components/ValidationFailuresTable.jsx`
- [ ] `src/pages/runs/components/SampleInvoicesAccordion.jsx`
- [ ] `src/pages/runs/components/CommitRunButton.jsx`
- [ ] `src/api/invoiceRuns.js` — add `simulate()`
