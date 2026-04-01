# Step 50 — FINVOICE Transmission to External Invoicing System

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: External Invoicing System — transmission, status feedback, retry handling
- `Docs/structured_breakdown/04-api-layer.md` → Section: Invoice Runs — POST /invoice-runs/{id}/send
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate" (SENT → COMPLETED or ERROR)

---

## Goal
Implement `ExternalInvoicingClient`, an HTTP client that transmits FINVOICE XML to the external invoicing system (Ropo, Netvisor, etc.) per invoice after a run. On success: invoice status → SENT; BillingEvent status → SENT. On failure: invoice status → ERROR; event status unchanged (still SENT). Implements a retry strategy. The `POST /api/v1/invoice-runs/{id}/send` endpoint triggers batch transmission. FE shows transmission status per invoice with a Retry button for ERROR invoices.

---

## Backend

### 50.1 ExternalInvoicingClient

**File:** `invoicing/src/main/java/com/example/invoicing/integration/ExternalInvoicingClient.java`

> **Requirement source:** `05-integration-layer.md` — "WasteHero calls the external system's transmission API"

```java
@Component
public class ExternalInvoicingClient {

    private final RestTemplate restTemplate;
    private final ExternalInvoicingConfig config;

    /**
     * Transmit a single FINVOICE XML to the external invoicing system.
     * @return TransmissionResult — success or failure with error details
     */
    public TransmissionResult transmit(Invoice invoice) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Authorization", "Bearer " + config.getApiToken());

            HttpEntity<String> request = new HttpEntity<>(invoice.getFinvoiceXml(), headers);
            ResponseEntity<TransmissionAck> response = restTemplate.exchange(
                config.getTransmitUrl(),
                HttpMethod.POST,
                request,
                TransmissionAck.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return TransmissionResult.success(invoice.getId(),
                    response.getBody() != null ? response.getBody().getExternalId() : null);
            } else {
                return TransmissionResult.failure(invoice.getId(),
                    "External system returned status " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            return TransmissionResult.failure(invoice.getId(), ex.getMessage());
        }
    }

    /**
     * Recall a transmitted invoice from the external system (if supported).
     */
    public RecallResult recall(String invoiceNumber) {
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                config.getRecallUrl() + "/" + invoiceNumber,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return RecallResult.success(invoiceNumber);
            }
            return RecallResult.failure(invoiceNumber,
                "External system returned " + response.getStatusCode());
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            return RecallResult.unsupported(invoiceNumber);
        } catch (RestClientException ex) {
            return RecallResult.failure(invoiceNumber, ex.getMessage());
        }
    }
}
```

---

### 50.2 ExternalInvoicingConfig

**File:** `invoicing/src/main/java/com/example/invoicing/integration/ExternalInvoicingConfig.java`

```java
@ConfigurationProperties(prefix = "app.external-invoicing")
@Component
public class ExternalInvoicingConfig {
    private String transmitUrl;       // e.g. https://api.ropo.fi/v2/invoices
    private String recallUrl;         // e.g. https://api.ropo.fi/v2/invoices/recall
    private String apiToken;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
    private int maxRetries = 3;
    private long retryDelayMs = 2000;
}
```

---

### 50.3 TransmissionService

**File:** `invoicing/src/main/java/com/example/invoicing/integration/TransmissionService.java`

```java
@Service
@Transactional
public class TransmissionService {

    /**
     * Transmit all invoices in a run with retry logic.
     * On success per invoice: Invoice.status → SENT, BillingEvent.status → SENT.
     * On failure per invoice: Invoice.status → ERROR (BillingEvent stays at SENT for re-send).
     */
    public void transmitRun(Long runId) {
        InvoiceRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus(InvoiceRunStatus.SENDING);
        runRepository.save(run);

        List<Invoice> invoices = invoiceRepository.findByInvoiceRunId(runId)
            .stream()
            .filter(i -> i.getStatus() == InvoiceStatus.READY)
            .toList();

        int successCount = 0;
        int failCount = 0;

        for (Invoice invoice : invoices) {
            TransmissionResult result = transmitWithRetry(invoice);
            if (result.isSuccess()) {
                invoice.setStatus(InvoiceStatus.SENT);
                for (BillingEvent event : invoice.getBillingEvents()) {
                    billingEventStatusService.transition(event.getId(), BillingEventStatus.SENT);
                }
                successCount++;
            } else {
                invoice.setStatus(InvoiceStatus.ERROR);
                invoice.setTransmissionError(result.getErrorMessage());
                failCount++;
                log.warn("Transmission failed for invoice {}: {}", invoice.getId(), result.getErrorMessage());
            }
        }
        invoiceRepository.saveAll(invoices);

        run.setStatus(failCount == 0 ? InvoiceRunStatus.SENT : InvoiceRunStatus.COMPLETED_WITH_ERRORS);
        run.setSentAt(Instant.now());
        runRepository.save(run);
    }

    private TransmissionResult transmitWithRetry(Invoice invoice) {
        int attempts = 0;
        while (attempts < config.getMaxRetries()) {
            TransmissionResult result = externalClient.transmit(invoice);
            if (result.isSuccess()) return result;
            attempts++;
            if (attempts < config.getMaxRetries()) {
                sleep(config.getRetryDelayMs() * attempts); // exponential back-off
            }
        }
        return TransmissionResult.failure(invoice.getId(), "Max retries exceeded");
    }
}
```

---

### 50.4 Invoice Entity Extensions

Add to `Invoice.java`:
```java
@Column(name = "transmission_error", length = 1000)
private String transmissionError;

@Column(name = "external_invoice_id", length = 100)
private String externalInvoiceId;   // ID assigned by the external system on success
```

---

### 50.5 Retry Endpoint for ERROR Invoices

**Extension to `InvoiceController`** (step 29):

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/invoices/{id}/retry-transmission` | Re-attempt transmission for an ERROR invoice | INVOICING_USER |

```java
@PostMapping("/{id}/retry-transmission")
public ResponseEntity<InvoiceResponse> retryTransmission(@PathVariable Long id) {
    Invoice invoice = invoiceRepository.findById(id).orElseThrow();
    if (invoice.getStatus() != InvoiceStatus.ERROR) {
        throw new InvalidInvoiceStateException("Only ERROR invoices can be retried");
    }
    TransmissionResult result = transmissionService.transmitSingle(invoice);
    if (result.isSuccess()) {
        invoice.setStatus(InvoiceStatus.SENT);
    }
    return ResponseEntity.ok(InvoiceResponse.from(invoiceRepository.save(invoice)));
}
```

---

## Frontend

### 50.6 Transmission Status in Run Results

**Extension to `RunInvoicesList.jsx`** (step 38/41):

Each invoice row in the run results shows:
- Status badge: READY (grey), SENDING (blue spinner), SENT (green), ERROR (red).
- For ERROR invoices: a "Retry" button and a tooltip showing `transmissionError` message.

**`RetryTransmissionButton` component:**
```jsx
const RetryTransmissionButton = ({ invoiceId, onRetry }) => (
  <button onClick={() => retryTransmission(invoiceId).then(onRetry)}>
    Retry
  </button>
)
```

**API calls (additions to `src/api/invoices.js`):**
```js
export const retryTransmission = (invoiceId) =>
  axios.post(`/api/v1/invoices/${invoiceId}/retry-transmission`)
```

---

## Verification Checklist

1. `POST /api/v1/invoice-runs/{id}/send` transmits all READY invoices; successful invoices → SENT; failed → ERROR with `transmissionError` populated.
2. Mock external endpoint to return 200 → invoice status = SENT; BillingEvent status = SENT.
3. Mock external endpoint to return 503 → first attempt fails; retry fires 3 times (verify log output); after max retries, invoice status = ERROR.
4. Retry delay: confirm exponential back-off (2s, 4s, 6s or 2s, 4s, 8s depending on implementation).
5. `POST /api/v1/invoices/{id}/retry-transmission` on ERROR invoice → re-attempts transmission; on success, status = SENT.
6. `POST retry-transmission` on SENT invoice → HTTP 400 "Only ERROR invoices can be retried."
7. `run.status = SENT` when all invoices transmitted; `COMPLETED_WITH_ERRORS` when at least one failed.
8. `run.sentAt` is populated after transmission completes.
9. Open `RunInvoicesList` in FE — ERROR invoices show red badge and Retry button with tooltip showing error message.
10. Click Retry in FE → `retryTransmission()` called; on success, badge updates to SENT; Retry button disappears.

---

## File Checklist

### Backend
- [ ] `integration/ExternalInvoicingClient.java`
- [ ] `integration/ExternalInvoicingConfig.java`
- [ ] `integration/TransmissionService.java`
- [ ] `integration/TransmissionResult.java`
- [ ] `integration/RecallResult.java`
- [ ] `invoice/Invoice.java` — add `transmissionError` and `externalInvoiceId` fields (extends step 29)
- [ ] `invoice/InvoiceController.java` — add retry-transmission endpoint (extends step 29)

### Frontend
- [ ] `src/pages/runs/components/RunInvoicesList.jsx` — add status badge and Retry button (extends step 38/41)
- [ ] `src/api/invoices.js` — add `retryTransmission()` (extends step 29)
