# Step 47 — Retroactive Price Adjustment — Preview Phase

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: RetroactivePriceAdjustmentService (preview phase)
- `Docs/structured_breakdown/04-api-layer.md` → Section: Retroactive Operations — POST /price-adjustments/preview
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 7: "The Preview-Then-Apply Pattern", Roles: FUNCTION_ADMIN required

---

## Goal
Implement the preview phase of `RetroactivePriceAdjustmentService`. The user defines filter criteria (date range + product selection) and an adjustment (percentage or fixed euro amount). The service computes what would change — current price, new price, difference — and returns the full list without making any changes. The preview has a TTL (30 minutes) and is stored in cache or DB. FUNCTION_ADMIN role required. No changes are made during preview.

---

## Backend

### 47.1 PriceAdjustmentPreview Entity

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/PriceAdjustmentPreview.java`

> **Requirement source:** `06-cross-cutting.md` — Rule 7: preview is held temporarily, expires if not applied

```java
@Entity
@Table(name = "price_adjustment_previews")
public class PriceAdjustmentPreview extends BaseAuditEntity {

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;             // now + 30 minutes

    @Column(name = "applied", nullable = false)
    private boolean applied = false;       // set to true when apply is called (step 48)

    @Column(name = "filter_criteria_json", columnDefinition = "TEXT")
    private String filterCriteriaJson;     // serialised PriceAdjustmentFilter

    @Column(name = "adjustment_json", columnDefinition = "TEXT")
    private String adjustmentJson;         // serialised PriceAdjustmentSpec

    @Column(name = "affected_event_count")
    private int affectedEventCount;

    @Column(name = "total_current_amount", precision = 19, scale = 4)
    private BigDecimal totalCurrentAmount;

    @Column(name = "total_new_amount", precision = 19, scale = 4)
    private BigDecimal totalNewAmount;

    // JSON list of affected event previews (stored for apply phase lookup)
    @Column(name = "affected_events_json", columnDefinition = "TEXT")
    private String affectedEventsJson;
}
```

---

### 47.2 PriceAdjustmentFilter and PriceAdjustmentSpec

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/PriceAdjustmentFilter.java`

```java
public class PriceAdjustmentFilter {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private List<Long> productIds;    // null = all products
    private Long customerId;          // null = all customers
    private String municipality;      // null = all municipalities
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/PriceAdjustmentSpec.java`

```java
public class PriceAdjustmentSpec {
    private AdjustmentType type;       // PERCENTAGE or FIXED_AMOUNT
    private BigDecimal value;          // e.g. 3.00 for 3% or €3.00 fixed
}

public enum AdjustmentType {
    PERCENTAGE,    // multiply unit price by (1 + value/100)
    FIXED_AMOUNT   // add value to unit price (can be negative)
}
```

---

### 47.3 RetroactivePriceAdjustmentService — Preview

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/RetroactivePriceAdjustmentService.java`

```java
@Service
@Transactional
public class RetroactivePriceAdjustmentService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    /**
     * Preview phase — compute affected events and new prices.
     * NO changes are made. Returns a previewId for the apply phase.
     */
    public PriceAdjustmentPreviewResponse preview(PriceAdjustmentPreviewRequest request) {
        // 1. Find all IN_PROGRESS events matching the filter
        //    Events with status != IN_PROGRESS are NEVER touched (PD-319)
        List<BillingEvent> affected = billingEventRepository
            .findByPriceAdjustmentFilter(
                request.getFilter().getDateFrom(),
                request.getFilter().getDateTo(),
                request.getFilter().getProductIds(),
                request.getFilter().getCustomerId(),
                BillingEventStatus.IN_PROGRESS);

        // 2. Compute new prices for each event
        List<AffectedEventPreview> previews = affected.stream()
            .map(event -> computeNewPrice(event, request.getAdjustment()))
            .toList();

        // 3. Persist preview with TTL
        PriceAdjustmentPreview preview = new PriceAdjustmentPreview();
        preview.setExpiresAt(Instant.now().plus(PREVIEW_TTL));
        preview.setApplied(false);
        preview.setFilterCriteriaJson(toJson(request.getFilter()));
        preview.setAdjustmentJson(toJson(request.getAdjustment()));
        preview.setAffectedEventCount(previews.size());
        preview.setTotalCurrentAmount(previews.stream()
            .map(AffectedEventPreview::getCurrentUnitPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        preview.setTotalNewAmount(previews.stream()
            .map(AffectedEventPreview::getNewUnitPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        preview.setAffectedEventsJson(toJson(previews));
        PriceAdjustmentPreview saved = previewRepository.save(preview);

        return new PriceAdjustmentPreviewResponse(
            saved.getId(),
            saved.getExpiresAt(),
            previews);
    }

    private AffectedEventPreview computeNewPrice(BillingEvent event, PriceAdjustmentSpec spec) {
        BigDecimal current = event.getUnitPrice();
        BigDecimal newPrice = switch (spec.getType()) {
            case PERCENTAGE ->
                current.multiply(BigDecimal.ONE.add(
                    spec.getValue().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
                    .setScale(4, RoundingMode.HALF_UP);
            case FIXED_AMOUNT ->
                current.add(spec.getValue()).setScale(4, RoundingMode.HALF_UP);
        };
        return new AffectedEventPreview(
            event.getId(),
            event.getCustomer().getId(),
            event.getProduct().getCode(),
            event.getEventDate(),
            current,
            newPrice,
            newPrice.subtract(current));
    }
}
```

---

### 47.4 BillingEventRepository — Price Adjustment Query

**Extension to `BillingEventRepository`:**

```java
@Query("SELECT e FROM BillingEvent e WHERE e.status = :status " +
       "AND e.eventDate >= :dateFrom AND e.eventDate <= :dateTo " +
       "AND (:productIds IS NULL OR e.product.id IN :productIds) " +
       "AND (:customerId IS NULL OR e.customer.id = :customerId) " +
       "AND e.excluded = false")
List<BillingEvent> findByPriceAdjustmentFilter(
    @Param("dateFrom") LocalDate dateFrom,
    @Param("dateTo") LocalDate dateTo,
    @Param("productIds") List<Long> productIds,
    @Param("customerId") Long customerId,
    @Param("status") BillingEventStatus status);
```

---

### 47.5 PriceAdjustmentController — Preview Endpoint

**File:** `invoicing/src/main/java/com/example/invoicing/adjustment/PriceAdjustmentController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/price-adjustments/preview` | Preview retroactive price adjustment | FUNCTION_ADMIN |

**POST request:**
```json
{
  "filter": {
    "dateFrom": "2024-01-01",
    "dateTo": "2024-01-31",
    "productIds": [101, 102],
    "customerId": null,
    "municipality": null
  },
  "adjustment": {
    "type": "PERCENTAGE",
    "value": 3.00
  }
}
```

**POST response:**
```json
{
  "previewId": 77,
  "expiresAt": "2024-02-01T09:30:00Z",
  "affectedEventCount": 1250,
  "totalCurrentAmount": 45000.00,
  "totalNewAmount": 46350.00,
  "totalDiff": 1350.00,
  "affectedEvents": [
    {
      "eventId": 5001,
      "customerId": 101,
      "productCode": "BIN-240L",
      "eventDate": "2024-01-15",
      "currentUnitPrice": 20.00,
      "newUnitPrice": 20.60,
      "diff": 0.60
    }
  ]
}
```

Note: `affectedEvents` may be paginated server-side if > 1000 events to avoid excessively large responses.

---

## Frontend

### 47.6 Price Adjustment Wizard — Step 1 and Step 2

**File:** `invoicing-fe/src/pages/adjustments/PriceAdjustmentWizard.jsx`

A multi-step wizard component:

**Step 1 — Define Criteria:**
- **FilterForm** — date range pickers (from/to), product multi-select (optional), customer search (optional), municipality (optional).
- **AdjustmentTypeSelector** — radio: "Percentage adjustment" / "Fixed amount adjustment".
- **AdjustmentValueInput** — number input. Shows `%` suffix for PERCENTAGE, `€` for FIXED_AMOUNT.
- **PreviewButton** — calls `POST /api/v1/price-adjustments/preview`. Loading spinner while waiting.

**Step 2 — Review Affected Events:**
- **PreviewSummaryCard** — `affectedEventCount`, `totalCurrentAmount`, `totalNewAmount`, `totalDiff` (formatted with + prefix).
- **AffectedEventsTable** — paginated table: Event ID, Customer, Product Code, Date, Current Price, New Price, Diff (colour-coded: positive = green, negative = red).
- **ExpiryCountdown** — "Preview expires in X minutes" countdown. When expired, "Preview Expired — Start Over" message replaces the Apply button.
- **ApplyButton** — calls `POST /api/v1/price-adjustments/{previewId}/apply` (step 48).

**API calls via `src/api/priceAdjustments.js`:**
```js
export const previewPriceAdjustment = (data) =>
  axios.post('/api/v1/price-adjustments/preview', data)
```

---

## Verification Checklist

1. `POST /api/v1/price-adjustments/preview` with 3% PERCENTAGE adjustment over January 2024 → returns preview with affected events; `newUnitPrice = currentUnitPrice * 1.03` (scale 4, ROUND_HALF_UP).
2. Events with status != IN_PROGRESS are NOT included in the preview — verify by creating a SENT event in the date range.
3. Excluded events (`excluded = true`) are NOT included in the preview.
4. FIXED_AMOUNT adjustment: `newUnitPrice = currentUnitPrice + value` (can produce negative if value is negative).
5. Preview persisted in DB with `expiresAt = now + 30 minutes`; `applied = false`.
6. `previewId` returned in response.
7. `POST preview` by INVOICING_USER → HTTP 403 Forbidden (FUNCTION_ADMIN required).
8. Filter by productIds: only events for the specified product IDs appear in the preview.
9. BigDecimal precision: percentage calculation uses scale 10 internally; stored at scale 4.
10. Open `PriceAdjustmentWizard` in FE — step 1 form submits; step 2 shows summary card and affected events table; expiry countdown visible.

---

## File Checklist

### Backend
- [ ] `adjustment/PriceAdjustmentPreview.java`
- [ ] `adjustment/PriceAdjustmentPreviewRepository.java`
- [ ] `adjustment/RetroactivePriceAdjustmentService.java`
- [ ] `adjustment/PriceAdjustmentController.java`
- [ ] `adjustment/PriceAdjustmentFilter.java`
- [ ] `adjustment/PriceAdjustmentSpec.java`
- [ ] `adjustment/AdjustmentType.java` (enum)
- [ ] `adjustment/AffectedEventPreview.java`
- [ ] `adjustment/dto/PriceAdjustmentPreviewRequest.java`
- [ ] `adjustment/dto/PriceAdjustmentPreviewResponse.java`
- [ ] `event/BillingEventRepository.java` — add `findByPriceAdjustmentFilter()` (extends step 10)

### Frontend
- [ ] `src/pages/adjustments/PriceAdjustmentWizard.jsx`
- [ ] `src/pages/adjustments/components/FilterForm.jsx`
- [ ] `src/pages/adjustments/components/AdjustmentTypeSelector.jsx`
- [ ] `src/pages/adjustments/components/PreviewSummaryCard.jsx`
- [ ] `src/pages/adjustments/components/AffectedEventsTable.jsx`
- [ ] `src/pages/adjustments/components/ExpiryCountdown.jsx`
- [ ] `src/api/priceAdjustments.js`
