# Step 53 — E-Invoice Operator Integration

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: EInvoiceIntegrationService
- `Docs/structured_breakdown/05-integration-layer.md` → Section 2: Invoice Operator — START/TERMINATE messages, two-identifier matching, manuallyLocked flag
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: SYSTEM for scheduled jobs

---

## Goal
Implement `EInvoiceIntegrationService` as a `@Scheduled` daily batch job. It fetches START and TERMINATE messages from the e-invoice operator (Ropo, Maventa, etc.) and updates customer delivery method and e-invoice addresses. Two-identifier matching is supported. The `manuallyLocked` flag prevents integration from overwriting manually set addresses. Unmatched messages are logged for manual review. FE: e-invoice operator log page showing daily batch results.

---

## Backend

### 53.1 EInvoiceOperatorMessage

**File:** `invoicing/src/main/java/com/example/invoicing/einvoice/EInvoiceOperatorMessage.java`

> **Requirement source:** `05-integration-layer.md` — "Each message contains: order type (START/TERMINATE), identifiers, e-invoice address, operator code"

```java
public class EInvoiceOperatorMessage {
    private EInvoiceOrderType orderType;       // START or TERMINATE
    private String identifier1;                // e.g. invoice reference number
    private String identifier2;                // e.g. customer number (optional)
    private String eInvoiceAddress;            // populated for START orders
    private String operatorCode;               // e.g. DABAFIHH for Ropo
    private Instant messageTimestamp;
}

public enum EInvoiceOrderType {
    START, TERMINATE
}
```

---

### 53.2 EInvoiceOperatorLog Entity

**File:** `invoicing/src/main/java/com/example/invoicing/einvoice/EInvoiceOperatorLog.java`

```java
@Entity
@Table(name = "einvoice_operator_logs")
public class EInvoiceOperatorLog extends BaseAuditEntity {

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private EInvoiceOrderType orderType;

    @Column(name = "identifier1", length = 100)
    private String identifier1;

    @Column(name = "identifier2", length = 100)
    private String identifier2;

    @Column(name = "matched_customer_id")
    private Long matchedCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private EInvoiceLogResult result;          // APPLIED, SKIPPED_LOCKED, MISMATCH, ERROR

    @Column(name = "note", length = 500)
    private String note;                       // reason for SKIPPED or MISMATCH
}

public enum EInvoiceLogResult {
    APPLIED,          // message processed and address updated
    SKIPPED_LOCKED,   // manuallyLocked = true — skipped
    MISMATCH,         // could not match to a customer
    ERROR             // unexpected exception
}
```

---

### 53.3 EInvoiceIntegrationService

**File:** `invoicing/src/main/java/com/example/invoicing/einvoice/EInvoiceIntegrationService.java`

> **Requirement source:** `03-business-logic.md` — EInvoiceIntegrationService
> **Requirement source:** `05-integration-layer.md` — matching, locking, daily batch

```java
@Service
@Transactional
public class EInvoiceIntegrationService {

    /**
     * Daily batch — fetches and processes all operator messages.
     * Runs once per day, typically at night (configure via @Scheduled cron).
     */
    @Scheduled(cron = "0 0 2 * * *")  // 2:00 AM daily
    public void processDailyBatch() {
        LocalDate today = LocalDate.now();
        log.info("Starting e-invoice operator batch for {}", today);

        List<EInvoiceOperatorMessage> messages = operatorClient.fetchMessages(today);
        int applied = 0, skipped = 0, mismatched = 0;

        for (EInvoiceOperatorMessage msg : messages) {
            EInvoiceLogResult result = processMessage(msg, today);
            switch (result) {
                case APPLIED -> applied++;
                case SKIPPED_LOCKED -> skipped++;
                case MISMATCH -> mismatched++;
                default -> {}
            }
        }
        log.info("E-invoice batch complete: {} applied, {} skipped (locked), {} mismatched",
            applied, skipped, mismatched);
    }

    private EInvoiceLogResult processMessage(EInvoiceOperatorMessage msg, LocalDate batchDate) {
        EInvoiceOperatorLog logEntry = new EInvoiceOperatorLog();
        logEntry.setBatchDate(batchDate);
        logEntry.setOrderType(msg.getOrderType());
        logEntry.setIdentifier1(msg.getIdentifier1());
        logEntry.setIdentifier2(msg.getIdentifier2());

        // 1. Match message to customer (two-identifier matching)
        Optional<Customer> customerOpt = matchCustomer(msg.getIdentifier1(), msg.getIdentifier2());
        if (customerOpt.isEmpty()) {
            logEntry.setResult(EInvoiceLogResult.MISMATCH);
            logEntry.setNote("Could not match identifiers to any customer");
            logRepository.save(logEntry);
            return EInvoiceLogResult.MISMATCH;
        }

        Customer customer = customerOpt.get();
        logEntry.setMatchedCustomerId(customer.getId());
        BillingProfile profile = customer.getBillingProfile();
        EInvoiceAddress eInvoiceAddr = profile.getEInvoiceAddress();

        // 2. Check manuallyLocked flag (PD-282)
        if (eInvoiceAddr != null && eInvoiceAddr.isManuallyLocked()) {
            logEntry.setResult(EInvoiceLogResult.SKIPPED_LOCKED);
            logEntry.setNote("E-invoice address is manually locked — integration update skipped");
            logRepository.save(logEntry);
            return EInvoiceLogResult.SKIPPED_LOCKED;
        }

        // 3. Apply the message
        if (msg.getOrderType() == EInvoiceOrderType.START) {
            profile.setDeliveryMethod(DeliveryMethod.E_INVOICE);
            if (eInvoiceAddr == null) {
                eInvoiceAddr = new EInvoiceAddress();
                profile.setEInvoiceAddress(eInvoiceAddr);
            }
            eInvoiceAddr.setAddress(msg.getEInvoiceAddress());
            eInvoiceAddr.setOperatorCode(msg.getOperatorCode());
            eInvoiceAddr.setManuallyLocked(false); // operator sets it — not manual
        } else { // TERMINATE
            profile.setDeliveryMethod(DeliveryMethod.PAPER);
            if (eInvoiceAddr != null) {
                eInvoiceAddr.setAddress(null);
                eInvoiceAddr.setOperatorCode(null);
            }
        }
        customerRepository.save(customer);

        logEntry.setResult(EInvoiceLogResult.APPLIED);
        logRepository.save(logEntry);
        return EInvoiceLogResult.APPLIED;
    }

    private Optional<Customer> matchCustomer(String id1, String id2) {
        // Two-identifier matching per PD-107: try invoiceReference + customerNumber
        return customerRepository.findByEInvoiceIdentifiers(id1, id2);
    }
}
```

---

### 53.4 EInvoiceOperatorClient

**File:** `invoicing/src/main/java/com/example/invoicing/einvoice/EInvoiceOperatorClient.java`

HTTP client that fetches the daily message batch from the operator:

```java
@Component
public class EInvoiceOperatorClient {

    public List<EInvoiceOperatorMessage> fetchMessages(LocalDate date) {
        ResponseEntity<List<EInvoiceOperatorMessage>> response = restTemplate.exchange(
            config.getOperatorUrl() + "?date=" + date,
            HttpMethod.GET,
            buildAuthHeaders(),
            new ParameterizedTypeReference<>() {});
        return response.getBody() != null ? response.getBody() : List.of();
    }
}
```

---

### 53.5 EInvoiceLogController

**File:** `invoicing/src/main/java/com/example/invoicing/einvoice/EInvoiceLogController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/einvoice-operator-logs` | List batch log entries, filterable by date and result | INVOICING_USER |

**GET response (paginated):**
```json
{
  "content": [
    {
      "id": 1,
      "batchDate": "2024-02-01",
      "orderType": "START",
      "identifier1": "INV-2024-000042",
      "identifier2": "123456",
      "matchedCustomerId": 1001,
      "result": "APPLIED",
      "note": null
    },
    {
      "id": 2,
      "batchDate": "2024-02-01",
      "orderType": "START",
      "identifier1": "UNKNOWN-REF",
      "identifier2": null,
      "matchedCustomerId": null,
      "result": "MISMATCH",
      "note": "Could not match identifiers to any customer"
    }
  ],
  "totalElements": 42
}
```

---

## Frontend

### 53.6 E-Invoice Operator Log Page

**File:** `invoicing-fe/src/pages/einvoice/EInvoiceOperatorLogPage.jsx`

Components:
- **DateFilter** — date picker to filter log entries by batch date (defaults to today).
- **ResultFilter** — filter by result: All / APPLIED / SKIPPED_LOCKED / MISMATCH / ERROR.
- **OperatorLogTable** — columns: Batch Date, Order Type, Identifier 1, Identifier 2, Matched Customer, Result (colour-coded badge), Note. Clicking a row expands details.
- **MismatchReviewPanel** — shows all MISMATCH entries with a "Resolve manually" action that navigates to the matching customer's billing profile for manual address entry.
- **DailySummaryCard** — at the top: total messages today, applied, skipped, mismatched count.

**Result badge colours:**
- APPLIED: green
- SKIPPED_LOCKED: amber
- MISMATCH: red
- ERROR: dark red

**API calls via `src/api/einvoiceLogs.js`:**
```js
export const getEInvoiceLogs = (params) =>
  axios.get('/api/v1/einvoice-operator-logs', { params })
```

---

## Verification Checklist

1. Mock operator API to return 2 START messages and 1 TERMINATE message for today. Run `processDailyBatch()` — verify 2 APPLIED and 1 APPLIED (terminate) in the log.
2. START message: customer's `deliveryMethod` = E_INVOICE; `eInvoiceAddress` set to the operator-provided address.
3. TERMINATE message: customer's `deliveryMethod` = PAPER; `eInvoiceAddress` cleared.
4. Message for a customer with `manuallyLocked = true` → `EInvoiceLogResult.SKIPPED_LOCKED`; no address change.
5. Message with unrecognisable identifiers → `EInvoiceLogResult.MISMATCH`; logged with both identifiers for manual review.
6. Two-identifier matching: customer found when both `identifier1` (invoice reference) and `identifier2` (customer number) are provided.
7. Single-identifier matching: customer found when only `identifier1` is provided and `identifier2` is null.
8. Unmatched MISMATCH entries are never silently discarded — they are persisted in the log for manual review.
9. `GET /api/v1/einvoice-operator-logs?batchDate=2024-02-01&result=MISMATCH` returns only MISMATCH entries for that date.
10. Open `EInvoiceOperatorLogPage` in FE — MISMATCH entries show in red; clicking a mismatch row links to the customer search page.

---

## File Checklist

### Backend
- [ ] `einvoice/EInvoiceIntegrationService.java`
- [ ] `einvoice/EInvoiceOperatorClient.java`
- [ ] `einvoice/EInvoiceOperatorMessage.java`
- [ ] `einvoice/EInvoiceOrderType.java` (enum)
- [ ] `einvoice/EInvoiceOperatorLog.java`
- [ ] `einvoice/EInvoiceLogResult.java` (enum)
- [ ] `einvoice/EInvoiceOperatorLogRepository.java`
- [ ] `einvoice/EInvoiceLogController.java`
- [ ] `einvoice/EInvoiceAddress.java` (embedded value object on BillingProfile)
- [ ] `einvoice/dto/EInvoiceOperatorLogResponse.java`

### Frontend
- [ ] `src/pages/einvoice/EInvoiceOperatorLogPage.jsx`
- [ ] `src/pages/einvoice/components/OperatorLogTable.jsx`
- [ ] `src/pages/einvoice/components/MismatchReviewPanel.jsx`
- [ ] `src/pages/einvoice/components/DailySummaryCard.jsx`
- [ ] `src/api/einvoiceLogs.js`
