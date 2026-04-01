# Step 41 — Invoice Run Orchestration

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceRunService, InvoiceRunLockService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoice Runs — POST /invoice-runs, GET /invoice-runs/{id}
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 5: "Data Locking During Billing Runs"

---

## Goal
Implement `InvoiceRunService`, the batch orchestrator that runs the complete invoicing pipeline for all customers matching the run filter. The sequence is: lock customers → group events by billing cycle → for each customer generate invoice → collect results → release locks. Handles partial failures gracefully (one customer failing does not abort the entire run). The controller provides creation and polling endpoints; the FE shows a progress indicator while the run executes.

---

## Backend

### 41.1 InvoiceRunService — Core Orchestration

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRunService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceRunService

```java
@Service
public class InvoiceRunService {

    @Async("invoiceRunExecutor")
    @Transactional
    public void executeRun(Long runId) {
        InvoiceRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus(InvoiceRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        List<String> lockedCustomers = new ArrayList<>();
        try {
            // 1. Fetch all matching IN_PROGRESS, non-excluded events
            List<BillingEvent> events =
                billingEventRepository.findByRunFilter(run.getFilterCriteria());

            // 2. Group by customer
            Map<Long, List<BillingEvent>> byCustomer = events.stream()
                .collect(Collectors.groupingBy(e -> e.getCustomer().getId()));

            // 3. Separate IMMEDIATE events (BillingRestriction config) from cycle-based events
            Map<Long, List<BillingEvent>> immediate = separateImmediateEvents(byCustomer);
            Map<Long, List<BillingEvent>> cycleBased = separateCycleBasedEvents(byCustomer);

            // 4. Collect all customer numbers and lock them
            lockedCustomers = Stream.concat(immediate.keySet().stream(),
                    cycleBased.keySet().stream())
                .distinct()
                .map(id -> customerService.getCustomerNumber(id))
                .toList();
            lockService.lockCustomers(runId, lockedCustomers);
            run.setLockedCustomerCount(lockedCustomers.size());
            runRepository.save(run);

            // 5. Process each customer group
            int totalInvoices = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<ValidationFailureEntry> allFailures = new ArrayList<>();

            for (Map.Entry<Long, List<BillingEvent>> entry :
                    mergeMaps(immediate, cycleBased).entrySet()) {
                Long customerId = entry.getKey();
                List<BillingEvent> customerEvents = entry.getValue();

                InvoiceGenerationResult result = invoiceGenerationService
                    .generate(customerEvents, customerId, false /* real run */);

                if (result.isSuccess()) {
                    totalInvoices++;
                    totalAmount = totalAmount.add(result.getInvoice().getGrossAmount());
                } else {
                    allFailures.addAll(mapToFailureEntries(customerId, result));
                }
            }

            // 6. Update run with results
            run.setTotalInvoices(totalInvoices);
            run.setTotalAmount(totalAmount);
            run.setValidationReport(buildValidationReport(allFailures, byCustomer.size()));
            run.setStatus(allFailures.stream().anyMatch(f -> f.getSeverity() == Severity.BLOCKING)
                ? InvoiceRunStatus.COMPLETED_WITH_ERRORS
                : InvoiceRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());

        } catch (Exception ex) {
            run.setStatus(InvoiceRunStatus.ERROR);
            run.setCompletedAt(Instant.now());
            log.error("Invoice run {} failed with exception", runId, ex);
        } finally {
            // 7. ALWAYS release locks — even on exception
            lockService.releaseLocksForRun(runId);
            run.setLockedCustomerCount(0);
            runRepository.save(run);
        }
    }
}
```

**Async execution:** `InvoiceRunService.executeRun()` is `@Async` so the `POST /api/v1/invoice-runs` endpoint returns immediately with the run ID. The caller polls `GET /api/v1/invoice-runs/{id}` for status.

**Thread pool configuration (`application.yml`):**
```yaml
app:
  async:
    invoice-run-executor:
      core-pool-size: 2
      max-pool-size: 4
      queue-capacity: 10
```

---

### 41.2 Billing Cycle Grouping

> **Requirement source:** `03-business-logic.md` — InvoiceRunService: separate IMMEDIATE and cycle-based events

```java
private Map<Long, List<BillingEvent>> separateImmediateEvents(
        Map<Long, List<BillingEvent>> byCustomer) {
    // Find products/services with BillingRestriction.IMMEDIATE
    Set<Long> immediateProductIds = billingRestrictionRepository
        .findImmediateProductIds();
    return byCustomer.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream()
                .filter(ev -> immediateProductIds.contains(ev.getProduct().getId()))
                .toList()
        ));
}

private Map<Long, List<BillingEvent>> separateCycleBasedEvents(
        Map<Long, List<BillingEvent>> byCustomer) {
    Set<Long> immediateProductIds = billingRestrictionRepository.findImmediateProductIds();
    return byCustomer.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream()
                .filter(ev -> !immediateProductIds.contains(ev.getProduct().getId()))
                .toList()
        ));
}
```

---

### 41.3 Scheduled Send

> **Requirement source:** `03-business-logic.md` — InvoiceCancellationService (deferred send)

```java
@Scheduled(fixedDelay = 60_000)  // check every minute
public void triggerScheduledSends() {
    List<InvoiceRun> due = runRepository.findScheduledForSend(Instant.now());
    for (InvoiceRun run : due) {
        transmissionService.transmitRun(run.getId());
    }
}
```

---

### 41.4 InvoiceRunController (extended from step 38)

The controller from step 38 is already defined. This step implements the service it delegates to.

`POST /api/v1/invoice-runs` now calls `executeRun()` asynchronously:
```java
@PostMapping
public ResponseEntity<InvoiceRunResponse> createRun(@RequestBody InvoiceRunRequest request) {
    InvoiceRun run = invoiceRunService.create(request);     // persist run in PENDING state
    invoiceRunService.executeRun(run.getId());               // fire async
    return ResponseEntity.accepted().body(InvoiceRunResponse.from(run));
}
```

Returns HTTP 202 Accepted (not 201 Created) to indicate the run has started but is not yet complete.

---

## Frontend

### 41.5 Invoice Run Creation and Progress Tracking

**File:** `invoicing-fe/src/pages/runs/InvoiceRunPage.jsx` (from step 38 — extend with progress)

**Run creation flow:**
1. User fills `RunFilterForm` and clicks "Start Run".
2. `POST /api/v1/invoice-runs` returns run ID with status `PENDING`.
3. FE navigates to `InvoiceRunDetailPage` (step 38) and begins polling.

**Progress indicator:**
- While status is `PENDING` or `RUNNING`: show a loading spinner and "Processing invoices..." message.
- Show running total if the backend reports it (optional intermediate status updates).
- When status reaches a terminal state (COMPLETED, COMPLETED_WITH_ERRORS, ERROR, CANCELLED): stop polling and render results.

**`RunInvoicesList` component** — lists all invoices in the completed run, paginated. Each row shows:
- Invoice number, customer name, net amount, gross amount, status badge.
- Clicking a row navigates to `InvoiceDetailPage`.

**API calls (existing `src/api/invoiceRuns.js` — no new functions needed for this step).**

**Polling logic example:**
```js
useEffect(() => {
  if (!runId) return
  const terminalStatuses = ['COMPLETED', 'COMPLETED_WITH_ERRORS', 'ERROR', 'CANCELLED', 'SENT']
  const interval = setInterval(async () => {
    const res = await getRun(runId)
    setRun(res.data)
    if (terminalStatuses.includes(res.data.status)) {
      clearInterval(interval)
    }
  }, 3000)
  return () => clearInterval(interval)
}, [runId])
```

---

## Verification Checklist

1. `POST /api/v1/invoice-runs` returns HTTP 202 and a run ID immediately (does not block until completion).
2. `GET /api/v1/invoice-runs/{id}` reflects real-time status: PENDING → RUNNING → COMPLETED.
3. After a completed run, all in-scope `BillingEvent` records have status = SENT.
4. All in-scope `Invoice` records are created with status = DRAFT (ready for the send step).
5. Locks are acquired at run start: attempt to update a locked customer's billing address → HTTP 423.
6. Locks are released after run completion: same customer's billing address update succeeds.
7. Locks are released even when the run throws an exception mid-processing (verify `finally` block).
8. IMMEDIATE events produce invoices regardless of whether cycle-based events are also in the run.
9. One customer with a blocking validation failure → that customer's invoice is skipped; other customers succeed; run status = COMPLETED_WITH_ERRORS.
10. FE polling: spinner visible while RUNNING; results rendered when COMPLETED; stops polling on terminal status.

---

## File Checklist

### Backend
- [ ] `run/InvoiceRunService.java` — full orchestration implementation
- [ ] `run/InvoiceRunController.java` — update to return 202 and call async (extends step 38)
- [ ] `common/config/AsyncConfig.java` — thread pool configuration
- [ ] `run/InvoiceRunRepository.java` — `findScheduledForSend` and `findActiveRuns` (extends step 38)

### Frontend
- [ ] `src/pages/runs/InvoiceRunDetailPage.jsx` — add polling logic and progress indicator (extends step 38)
- [ ] `src/pages/runs/components/RunInvoicesList.jsx` (extends step 38)
