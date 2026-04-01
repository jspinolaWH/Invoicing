# Step 38 — Invoice Run Entity

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: InvoiceRun
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoice Runs — POST, GET by ID, POST cancel, POST schedule-send, POST send
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 3: "Simulation Mode Guard", Rule 5: "Data Locking During Billing Runs"

---

## Goal
Define the `InvoiceRun` entity that is the container for a complete billing session. It stores the simulation flag, filter criteria, validation report, lock tracking metadata, and scheduled send date. The controller provides endpoints to create a run, poll its status, cancel it, schedule its send, and trigger manual send. The filter criteria are fully embedded so a run is self-contained and reproducible for auditing.

---

## Backend

### 38.1 RunFilterCriteria Embeddable

**File:** `invoicing/src/main/java/com/example/invoicing/run/RunFilterCriteria.java`

> **Requirement source:** `01-domain-model.md` — InvoiceRun filter configuration (PD-293)

```java
@Embeddable
public class RunFilterCriteria {

    @Column(name = "filter_municipality", length = 100)
    private String municipality;

    @Column(name = "filter_min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;           // exclude customers below this invoice total

    @Column(name = "filter_period_from")
    private LocalDate periodFrom;

    @Column(name = "filter_period_to")
    private LocalDate periodTo;

    @Column(name = "filter_customer_type", length = 50)
    private String customerType;            // e.g. MUNICIPALITY, BUSINESS, CONSUMER

    @Column(name = "filter_service_type", length = 100)
    private String serviceType;             // e.g. BIN_EMPTYING, SEPTIC

    @Column(name = "filter_location", length = 100)
    private String location;               // reception/unloading location

    @Column(name = "filter_service_responsibility", length = 50)
    private String serviceResponsibility;  // PUBLIC_LAW or PRIVATE_LAW
}
```

All fields are nullable — a null field means "no filter on this dimension."

---

### 38.2 ValidationReport Embeddable

**File:** `invoicing/src/main/java/com/example/invoicing/run/ValidationReport.java`

> **Requirement source:** `06-cross-cutting.md` — Rule 6: "Validation Report Pattern"

```java
@Embeddable
public class ValidationReport {

    @Column(name = "report_total_checked")
    private Integer totalChecked;

    @Column(name = "report_passed")
    private Integer passed;

    @Column(name = "report_blocking_count")
    private Integer blockingCount;

    @Column(name = "report_warning_count")
    private Integer warningCount;

    // JSON-serialised list of ValidationFailureEntry objects
    @Column(name = "report_failures_json", columnDefinition = "TEXT")
    private String failuresJson;
}
```

---

### 38.3 InvoiceRun Entity

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRun.java`

> **Requirement source:** `01-domain-model.md` — InvoiceRun entity

```java
@Entity
@Table(name = "invoice_runs")
public class InvoiceRun extends BaseAuditEntity {

    @Column(name = "simulation_mode", nullable = false)
    private boolean simulationMode;

    @Embedded
    private RunFilterCriteria filterCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceRunStatus status = InvoiceRunStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_invoices")
    private Integer totalInvoices;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Embedded
    private ValidationReport validationReport;

    // Lock tracking — which customers are locked for this run
    @Column(name = "locked_customer_count")
    private Integer lockedCustomerCount;

    @Column(name = "scheduled_send_at")
    private Instant scheduledSendAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @OneToMany(mappedBy = "invoiceRun")
    private List<Invoice> invoices = new ArrayList<>();

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
```

**`InvoiceRunStatus` enum:** `PENDING`, `RUNNING`, `COMPLETED`, `COMPLETED_WITH_ERRORS`, `CANCELLED`, `SENDING`, `SENT`, `ERROR`

---

### 38.4 InvoiceRunRepository

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRunRepository.java`

```java
public interface InvoiceRunRepository extends JpaRepository<InvoiceRun, Long> {

    // Find runs due for scheduled transmission
    @Query("SELECT r FROM InvoiceRun r WHERE r.status = 'COMPLETED' " +
           "AND r.scheduledSendAt IS NOT NULL AND r.scheduledSendAt <= :now")
    List<InvoiceRun> findScheduledForSend(@Param("now") Instant now);

    // Find active runs (for lock check)
    @Query("SELECT r FROM InvoiceRun r WHERE r.status IN ('PENDING', 'RUNNING', 'SENDING')")
    List<InvoiceRun> findActiveRuns();
}
```

---

### 38.5 InvoiceRunService

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRunService.java`

Method signatures:
- `create(InvoiceRunRequest request)` → `InvoiceRunResponse` (creates and starts or queues the run)
- `findById(Long id)` → `InvoiceRunResponse`
- `cancel(Long id, String reason)` → `InvoiceRunResponse`
- `scheduleSend(Long id, Instant sendAt)` → `InvoiceRunResponse`
- `triggerSend(Long id)` → `InvoiceRunResponse`
- `updateStatus(Long id, InvoiceRunStatus status)` → void (internal)

---

### 38.6 InvoiceRunController

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRunController.java`

Base path: `/api/v1/invoice-runs`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoice-runs` | Create and start a real invoice run | INVOICING_USER |
| GET | `/api/v1/invoice-runs/{id}` | Poll run status and results | INVOICING_USER |
| POST | `/api/v1/invoice-runs/{id}/cancel` | Cancel a run | BILLING_MANAGER |
| POST | `/api/v1/invoice-runs/{id}/schedule-send` | Schedule transmission at a future time | INVOICING_USER |
| POST | `/api/v1/invoice-runs/{id}/send` | Manually trigger transmission | INVOICING_USER |

**POST /api/v1/invoice-runs request:**
```json
{
  "simulationMode": false,
  "filterCriteria": {
    "municipality": "Tampere",
    "minAmount": 5.00,
    "periodFrom": "2024-01-01",
    "periodTo": "2024-01-31",
    "customerType": null,
    "serviceType": null,
    "location": null,
    "serviceResponsibility": null
  }
}
```

**GET /api/v1/invoice-runs/{id} response:**
```json
{
  "id": 42,
  "simulationMode": false,
  "status": "COMPLETED",
  "startedAt": "2024-02-01T08:00:00Z",
  "completedAt": "2024-02-01T08:03:12Z",
  "totalInvoices": 248,
  "totalAmount": 31540.00,
  "filterCriteria": {
    "municipality": "Tampere",
    "periodFrom": "2024-01-01",
    "periodTo": "2024-01-31"
  },
  "validationReport": {
    "totalChecked": 248,
    "passed": 245,
    "blockingCount": 2,
    "warningCount": 1
  },
  "scheduledSendAt": null,
  "sentAt": null
}
```

**POST /api/v1/invoice-runs/{id}/cancel request:**
```json
{
  "reason": "Wrong period selected — run will be restarted."
}
```

**POST /api/v1/invoice-runs/{id}/schedule-send request:**
```json
{
  "sendAt": "2024-02-05T09:00:00Z"
}
```

---

## Frontend

### 38.7 Invoice Run Creation Form

**File:** `invoicing-fe/src/pages/runs/InvoiceRunPage.jsx`

Components:
- **RunFilterForm** — form fields: municipality (text), minimum amount (number), period from/to (date pickers), customer type (select), service type (text), location (text), service responsibility (select: PUBLIC_LAW / PRIVATE_LAW / all). All fields optional.
- **RunModeToggle** — "Real Run" vs "Simulation" radio buttons.
- **CreateRunButton** — submits the form; on success navigates to the run detail/progress page.

**File:** `invoicing-fe/src/pages/runs/InvoiceRunDetailPage.jsx`

Components:
- **RunStatusBadge** — colour-coded: PENDING (grey), RUNNING (blue/spinner), COMPLETED (green), COMPLETED_WITH_ERRORS (amber), CANCELLED (red), SENT (teal).
- **RunSummaryCard** — total invoices, total amount, duration (completedAt - startedAt).
- **ValidationReportSummary** — passed count, blocking failures count, warning count. Link to full failures list.
- **RunActionBar** — shows contextually: "Cancel" (BILLING_MANAGER, only if not yet SENT), "Schedule Send" (date/time picker), "Send Now" button.
- **RunInvoicesList** — paginated list of invoices in the run with individual status badges.

**Polling:** the FE polls `GET /api/v1/invoice-runs/{id}` every 3 seconds while `status` is `PENDING` or `RUNNING`, stopping when a terminal status is reached.

**API calls via `src/api/invoiceRuns.js`:**
```js
export const createRun = (data) => axios.post('/api/v1/invoice-runs', data)
export const getRun = (id) => axios.get(`/api/v1/invoice-runs/${id}`)
export const cancelRun = (id, reason) => axios.post(`/api/v1/invoice-runs/${id}/cancel`, { reason })
export const scheduleSend = (id, sendAt) => axios.post(`/api/v1/invoice-runs/${id}/schedule-send`, { sendAt })
export const triggerSend = (id) => axios.post(`/api/v1/invoice-runs/${id}/send`)
```

---

## Verification Checklist

1. Hibernate creates `invoice_runs` table with all columns including embedded `filter_criteria_*` and `report_*` columns.
2. `POST /api/v1/invoice-runs` with `simulationMode: false` → returns run ID and `status: PENDING`.
3. `GET /api/v1/invoice-runs/{id}` reflects real-time status while run is in progress.
4. `POST /api/v1/invoice-runs/{id}/cancel` by BILLING_MANAGER role → status changes to CANCELLED; by INVOICING_USER → HTTP 403.
5. `POST /api/v1/invoice-runs/{id}/schedule-send` → `scheduledSendAt` persisted; `findScheduledForSend()` returns this run when `now >= scheduledSendAt`.
6. `POST /api/v1/invoice-runs/{id}/send` when status = COMPLETED → triggers transmission (step 50).
7. `validationReport` is populated after a run completes — blocking failures count matches actual blocking validation failures.
8. `findActiveRuns()` returns only runs in PENDING/RUNNING/SENDING status — not COMPLETED or CANCELLED.
9. Open `InvoiceRunPage` in FE — filter form submits; navigates to detail page; spinner shows while RUNNING.
10. FE polling stops automatically when run reaches COMPLETED or CANCELLED status.

---

## File Checklist

### Backend
- [ ] `run/InvoiceRun.java`
- [ ] `run/InvoiceRunStatus.java` (enum)
- [ ] `run/RunFilterCriteria.java` (Embeddable)
- [ ] `run/ValidationReport.java` (Embeddable)
- [ ] `run/InvoiceRunRepository.java`
- [ ] `run/InvoiceRunService.java`
- [ ] `run/InvoiceRunController.java`
- [ ] `run/dto/InvoiceRunRequest.java`
- [ ] `run/dto/InvoiceRunResponse.java`
- [ ] `run/dto/ScheduleSendRequest.java`
- [ ] `run/dto/CancelRunRequest.java`

### Frontend
- [ ] `src/pages/runs/InvoiceRunPage.jsx`
- [ ] `src/pages/runs/InvoiceRunDetailPage.jsx`
- [ ] `src/pages/runs/components/RunFilterForm.jsx`
- [ ] `src/pages/runs/components/RunStatusBadge.jsx`
- [ ] `src/pages/runs/components/RunSummaryCard.jsx`
- [ ] `src/pages/runs/components/ValidationReportSummary.jsx`
- [ ] `src/pages/runs/components/RunActionBar.jsx`
- [ ] `src/pages/runs/components/RunInvoicesList.jsx`
- [ ] `src/api/invoiceRuns.js`
