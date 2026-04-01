# Step 48 — Retroactive Price Adjustment — Apply Phase

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: RetroactivePriceAdjustmentService (apply phase)
- `Docs/structured_breakdown/04-api-layer.md` → Section: Retroactive Operations — POST /price-adjustments/{previewId}/apply
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 7: "Preview-Then-Apply Pattern", Rule 1: "Audit Trail" — PriceAdjustmentRun

---

## Goal
Implement the apply phase of `RetroactivePriceAdjustmentService`. A single atomic transaction applies all price changes from a non-expired preview, writes a `PriceAdjustmentRun` audit record, and writes an individual `BillingEventAuditLog` entry for every changed event. The preview expiry is validated before any changes are made. FUNCTION_ADMIN role required. The FE shows the audit record after success as confirmation.

---

## Backend

### 48.1 PriceAdjustmentRun Entity

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/PriceAdjustmentRun.java`

> **Requirement source:** `01-domain-model.md` — PriceAdjustmentRun audit entity
> **Requirement source:** `06-cross-cutting.md` — "Audit logs are append-only. Never updated or deleted."

```java
@Entity
@Table(name = "price_adjustment_runs")
public class PriceAdjustmentRun extends BaseAuditEntity {

    @Column(name = "run_id", nullable = false, unique = true)
    private String runId;               // UUID for external reference

    @Column(name = "applied_by", nullable = false, length = 100)
    private String appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "affected_count", nullable = false)
    private int affectedCount;

    @Column(name = "filter_criteria_json", columnDefinition = "TEXT")
    private String filterCriteriaJson;

    @Column(name = "adjustment_json", columnDefinition = "TEXT")
    private String adjustmentJson;

    @Column(name = "preview_id")
    private Long previewId;

    @Column(name = "total_price_delta", precision = 19, scale = 4)
    private BigDecimal totalPriceDelta; // sum of all (newPrice - oldPrice) across all events
}
```

This entity is append-only. No UPDATE or DELETE permissions granted to the application user for this table.

---

### 48.2 RetroactivePriceAdjustmentService — Apply Phase

**Extension to `RetroactivePriceAdjustmentService` (from step 47):**

```java
/**
 * Apply phase — commit all price changes from the preview in one transaction.
 * Validates: previewId exists, not expired, not already applied.
 * Writes PriceAdjustmentRun audit record + BillingEventAuditLog per changed event.
 */
@Transactional
public PriceAdjustmentRunResponse apply(Long previewId, String appliedBy) {
    // 1. Load and validate the preview
    PriceAdjustmentPreview preview = previewRepository.findById(previewId)
        .orElseThrow(() -> new PreviewNotFoundException("Preview " + previewId + " not found"));

    if (preview.isApplied()) {
        throw new PreviewAlreadyAppliedException("Preview " + previewId + " has already been applied");
    }
    if (Instant.now().isAfter(preview.getExpiresAt())) {
        throw new PreviewExpiredException("Preview " + previewId + " has expired. Please create a new preview.");
    }

    // 2. Re-fetch affected events (re-validate they are still IN_PROGRESS)
    //    We do NOT rely on the stored JSON — we re-query to catch events that changed status
    PriceAdjustmentFilter filter = fromJson(preview.getFilterCriteriaJson(), PriceAdjustmentFilter.class);
    PriceAdjustmentSpec adjustment = fromJson(preview.getAdjustmentJson(), PriceAdjustmentSpec.class);

    List<BillingEvent> events = billingEventRepository.findByPriceAdjustmentFilter(
        filter.getDateFrom(), filter.getDateTo(),
        filter.getProductIds(), filter.getCustomerId(),
        BillingEventStatus.IN_PROGRESS);

    // 3. Apply changes and write audit logs
    BigDecimal totalDelta = BigDecimal.ZERO;
    for (BillingEvent event : events) {
        BigDecimal oldPrice = event.getUnitPrice();
        BigDecimal newPrice = computeNewPrice(event, adjustment);
        BigDecimal delta = newPrice.subtract(oldPrice);

        event.setUnitPrice(newPrice);
        // Recalculate net and gross amounts
        event.setNetAmount(newPrice.multiply(event.getQuantity()).setScale(4, RoundingMode.HALF_UP));
        BigDecimal vatFactor = BigDecimal.ONE.add(
            event.getVatRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
        event.setGrossAmount(event.getNetAmount().multiply(vatFactor).setScale(4, RoundingMode.HALF_UP));

        totalDelta = totalDelta.add(delta);

        // Per-event audit log
        auditLogService.logPriceChange(event.getId(), oldPrice, newPrice, appliedBy,
            "Retroactive price adjustment run " + previewId);
    }
    billingEventRepository.saveAll(events);

    // 4. Mark preview as applied
    preview.setApplied(true);
    previewRepository.save(preview);

    // 5. Write PriceAdjustmentRun audit record
    PriceAdjustmentRun run = new PriceAdjustmentRun();
    run.setRunId(UUID.randomUUID().toString());
    run.setAppliedBy(appliedBy);
    run.setAppliedAt(Instant.now());
    run.setAffectedCount(events.size());
    run.setFilterCriteriaJson(preview.getFilterCriteriaJson());
    run.setAdjustmentJson(preview.getAdjustmentJson());
    run.setPreviewId(previewId);
    run.setTotalPriceDelta(totalDelta);
    PriceAdjustmentRun saved = adjustmentRunRepository.save(run);

    return PriceAdjustmentRunResponse.from(saved);
}
```

---

### 48.3 PriceAdjustmentRunRepository

```java
public interface PriceAdjustmentRunRepository extends JpaRepository<PriceAdjustmentRun, Long> {

    // Audit queries — find by company and date range
    @Query("SELECT r FROM PriceAdjustmentRun r WHERE r.appliedAt BETWEEN :from AND :to ORDER BY r.appliedAt DESC")
    List<PriceAdjustmentRun> findByDateRange(
        @Param("from") Instant from,
        @Param("to") Instant to);

    // Review a specific user's retroactive actions
    List<PriceAdjustmentRun> findByAppliedByOrderByAppliedAtDesc(String appliedBy);
}
```

---

### 48.4 Exception Types

```java
public class PreviewNotFoundException extends RuntimeException { }      // 404
public class PreviewAlreadyAppliedException extends RuntimeException { } // 409
public class PreviewExpiredException extends RuntimeException { }        // 410 Gone
```

Map `PreviewExpiredException` to HTTP 410 Gone in `GlobalExceptionHandler`.

---

### 48.5 PriceAdjustmentController — Apply Endpoint

**Extension to `PriceAdjustmentController` (from step 47):**

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/price-adjustments/{previewId}/apply` | Apply a previewed price adjustment | FUNCTION_ADMIN |

**POST response (200 OK):**
```json
{
  "runId": "b4e1f9a2-3c5d-4e7b-8f9a-0b1c2d3e4f5a",
  "appliedBy": "user@example.com",
  "appliedAt": "2024-02-01T09:10:00Z",
  "affectedCount": 1250,
  "totalPriceDelta": 1350.00,
  "previewId": 77
}
```

**Error responses:**
- `404 Not Found` — previewId does not exist.
- `409 Conflict` — preview already applied.
- `410 Gone` — preview expired.

---

## Frontend

### 48.6 Price Adjustment Wizard — Step 3 (Apply)

**Extension to `PriceAdjustmentWizard.jsx`** (step 47):

**Step 3 — Confirm and Apply:**
- **ConfirmationSummary** — displays the exact same numbers as step 2: `affectedEventCount`, `totalCurrentAmount`, `totalNewAmount`, `totalDiff`.
- **ApplyButton** — labelled "Apply Price Changes — This cannot be undone." Disabled if `expiresAt` has passed.
- **ProgressSpinner** — shown while the apply call is in progress.

**Step 4 — Result (after successful apply):**
- **AuditRunCard** — shows the `PriceAdjustmentRun` audit record: Run ID (UUID), Applied By, Applied At, Affected Count, Total Price Delta.
- Success message: "Price adjustment applied successfully to X billing events."
- "Start New Adjustment" link returns to step 1.
- **Download Audit Report** button (optional) — calls a report endpoint or just displays the data.

**API calls (additions to `src/api/priceAdjustments.js`):**
```js
export const applyPriceAdjustment = (previewId) =>
  axios.post(`/api/v1/price-adjustments/${previewId}/apply`)
```

---

## Verification Checklist

1. `POST /api/v1/price-adjustments/{previewId}/apply` with valid non-expired preview → all `BillingEvent.unitPrice` values updated; `BillingEvent.netAmount` and `grossAmount` recalculated.
2. Verify recalculation: for a 3% increase, `newNetAmount = newUnitPrice × quantity` (scale 4, ROUND_HALF_UP).
3. `BillingEventAuditLog` records created for every changed event with old price, new price, user, and reason text including previewId.
4. `PriceAdjustmentRun` record persisted: `affectedCount` = number of events actually updated, `totalPriceDelta` = sum of all deltas.
5. Attempt apply twice on same previewId → second call returns HTTP 409.
6. Attempt apply on expired preview (manipulate `expiresAt` to past) → HTTP 410 Gone.
7. Attempt apply by INVOICING_USER → HTTP 403 Forbidden.
8. Events that changed to SENT between preview and apply are NOT modified — `findByPriceAdjustmentFilter` with `IN_PROGRESS` filter skips them.
9. `preview.applied = true` after successful apply — subsequent apply call returns 409 (not re-computes and re-applies).
10. Open wizard step 3 in FE — "Apply" button disabled after expiry; after successful apply, `AuditRunCard` shows run ID and affected count.

---

## File Checklist

### Backend
- [ ] `adjustment/PriceAdjustmentRun.java`
- [ ] `adjustment/PriceAdjustmentRunRepository.java`
- [ ] `adjustment/RetroactivePriceAdjustmentService.java` — add `apply()` method (extends step 47)
- [ ] `adjustment/PriceAdjustmentController.java` — add apply endpoint (extends step 47)
- [ ] `adjustment/PreviewNotFoundException.java`
- [ ] `adjustment/PreviewAlreadyAppliedException.java`
- [ ] `adjustment/PreviewExpiredException.java`
- [ ] `adjustment/dto/PriceAdjustmentRunResponse.java`
- [ ] `common/exception/GlobalExceptionHandler.java` — add 410 handler for PreviewExpiredException

### Frontend
- [ ] `src/pages/adjustments/PriceAdjustmentWizard.jsx` — add step 3 and step 4 (extends step 47)
- [ ] `src/pages/adjustments/components/AuditRunCard.jsx`
- [ ] `src/api/priceAdjustments.js` — add `applyPriceAdjustment()` (extends step 47)
