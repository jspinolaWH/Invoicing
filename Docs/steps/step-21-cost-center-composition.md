# Step 21 — CostCenterCompositionService

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Area 6: Accounting and Financial Allocation — "CostCenterCompositionService: Takes product cost centre segment, reception point cost centre segment, service responsibility segment. Combines them into the final cost centre code per the company's configured format."
- `Docs/structured_breakdown/01-domain-model.md` → Section: AccountingAccount / CostCenter / VatRate — "CostCenter is a composite — its final value for an event is assembled from multiple components at runtime, not stored as a flat code. The entity defines the segments; the service composes them."
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingEvent — "Accounts and cost centres — mandatory for accounting allocation"

---

## Goal

In Step 03 the `CostCenter` entity was built with a static `compositeCode` field. This step replaces that static approach with dynamic assembly: the full cost centre code for a `BillingEvent` is computed at runtime by concatenating three segments drawn from the event's linked entities, separated by a configurable separator character.

The three segments are:
1. **Product segment** — the cost centre segment defined on the `BillingEvent`'s product
2. **Reception point segment** — the cost centre segment defined on the event's load/unload location
3. **Service responsibility segment** — derived from the event's `serviceResponsibility` field (PUBLIC_LAW vs PRIVATE_LAW encoding)

The company configures the separator (e.g. `-` or `.`) and the segment order during implementation. That configuration is stored in `CostCenterCompositionConfig`. The service reads this config and concatenates accordingly.

This is an internal service with no HTTP endpoints. The resolved cost centre code is surfaced to the user as part of the billing event detail response.

---

## Backend

### 21.1 CostCenterCompositionConfig Entity

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/costcenter/CostCenterCompositionConfig.java`

Holds the company-level configuration for how cost centre codes are assembled.

| Field | Java Type | JPA / Validation Notes |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue` |
| `separator` | `String` | `@Column(nullable = false, length = 5)` default `"-"` — the character(s) placed between segments |
| `segmentOrder` | `String` | `@Column(nullable = false, length = 50)` — comma-separated segment names in the desired order, e.g. `"PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY"` |
| `productSegmentEnabled` | `boolean` | `@Column(nullable = false)` default `true` |
| `receptionPointSegmentEnabled` | `boolean` | `@Column(nullable = false)` default `true` |
| `serviceResponsibilitySegmentEnabled` | `boolean` | `@Column(nullable = false)` default `true` |
| `publicLawCode` | `String` | `@Column(length = 10)` default `"PL"` — the string inserted for PUBLIC_LAW events |
| `privateLawCode` | `String` | `@Column(length = 10)` default `"PR"` — the string inserted for PRIVATE_LAW events |

```java
@Entity
@Table(name = "cost_center_composition_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterCompositionConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 5)
    private String separator = "-";

    @Column(nullable = false, length = 50)
    private String segmentOrder = "PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY";

    @Column(nullable = false)
    private boolean productSegmentEnabled = true;

    @Column(nullable = false)
    private boolean receptionPointSegmentEnabled = true;

    @Column(nullable = false)
    private boolean serviceResponsibilitySegmentEnabled = true;

    @Column(length = 10)
    private String publicLawCode = "PL";

    @Column(length = 10)
    private String privateLawCode = "PR";
}
```

---

### 21.2 CostCenter Entity Update

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/costcenter/CostCenter.java` (modification from Step 03)

Add two fields to the existing entity:

| Field | Java Type | Notes |
|---|---|---|
| `productSegment` | `String` | `@Column(length = 20)` — the segment code contributed by this cost centre when it belongs to a product |
| `receptionPointSegment` | `String` | `@Column(length = 20)` — the segment code contributed when this cost centre belongs to a reception point |

The `compositeCode` field from Step 03 becomes **deprecated** — it is no longer the primary key for cost centre lookup. It remains in the schema for backwards compatibility but `CostCenterCompositionService` does not use it.

---

### 21.3 CostCenterCompositionService

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/costcenter/CostCenterCompositionService.java`

> **Requirement source:** `03-business-logic.md` Area 6 — "Takes: product cost centre segment, reception point cost centre segment, service responsibility segment. Combines them into the final cost centre code per the company's configured format."

| Method Signature | Description |
|---|---|
| `String compose(BillingEvent event)` | Main method. Loads `CostCenterCompositionConfig`, resolves each enabled segment from the event's linked entities, concatenates with the configured separator in the configured order. Returns the fully assembled cost centre code string. |
| `String resolveProductSegment(BillingEvent event)` | Reads `event.getProduct().getCostCenter().getProductSegment()`. Returns empty string if the product has no cost centre, or if the product segment is disabled in config. |
| `String resolveReceptionPointSegment(BillingEvent event)` | Reads the cost centre segment of the event's load/unload location entity. Returns empty string if location has no cost centre or segment is disabled. |
| `String resolveServiceResponsibilitySegment(BillingEvent event)` | Maps `event.getLegalClassification()` to `config.getPublicLawCode()` or `config.getPrivateLawCode()`. Returns empty string if segment is disabled. |
| `CostCenterCompositionConfig getConfig()` | Loads the single config record. Throws if not configured — the admin must set this up before any invoicing. |

**Composition logic:**

```java
public String compose(BillingEvent event) {
    CostCenterCompositionConfig config = getConfig();
    String[] order = config.getSegmentOrder().split(",");

    List<String> segments = new ArrayList<>();
    for (String segmentName : order) {
        String value = switch (segmentName.trim()) {
            case "PRODUCT"              -> resolveProductSegment(event);
            case "RECEPTION_POINT"      -> resolveReceptionPointSegment(event);
            case "SERVICE_RESPONSIBILITY" -> resolveServiceResponsibilitySegment(event);
            default -> "";
        };
        if (value != null && !value.isBlank()) {
            segments.add(value);
        }
    }
    return String.join(config.getSeparator(), segments);
}
```

**Example outputs:**
- Product segment `"WC"`, reception point segment `"RP42"`, service responsibility `"PL"`, separator `"-"` → `"WC-RP42-PL"`
- Reception point segment disabled → `"WC-PL"`
- All segments disabled → `""` (empty string, treated as "no cost centre")

---

### 21.4 BillingEvent Detail Response Update

**File:** `invoicing/src/main/java/com/example/invoicing/billing/dto/BillingEventDetailResponse.java` (modification)

Add one field to the existing detail DTO:

```java
private String resolvedCostCenterCode;  // computed by CostCenterCompositionService, not stored
```

**File:** `invoicing/src/main/java/com/example/invoicing/billing/BillingEventService.java` (modification)

In the `getById(Long id)` method, after fetching the event, call `costCenterCompositionService.compose(event)` and set the result on the response DTO:

```java
BillingEventDetailResponse response = mapper.toDetailResponse(event);
response.setResolvedCostCenterCode(costCenterCompositionService.compose(event));
return response;
```

The resolved cost centre code is **not stored on the BillingEvent** — it is computed on demand each time the detail is fetched. This means a change to `CostCenterCompositionConfig` is immediately reflected in all unbilled event detail views.

---

### 21.5 CostCenterCompositionConfig Controller

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/costcenter/CostCenterCompositionConfigController.java`

Base path: `/api/v1/cost-center-composition-config`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/cost-center-composition-config` | Returns the current composition config |
| PUT | `/api/v1/cost-center-composition-config` | Updates the config (replaces the single record) |

**GET response / PUT request body:**
```json
{
  "separator": "-",
  "segmentOrder": "PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY",
  "productSegmentEnabled": true,
  "receptionPointSegmentEnabled": true,
  "serviceResponsibilitySegmentEnabled": true,
  "publicLawCode": "PL",
  "privateLawCode": "PR"
}
```

---

## Frontend

### 21.6 Resolved Cost Centre on Billing Event Detail Page

**File:** `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` (modification)

Add a read-only "Resolved Cost Centre Code" field to the event detail view. This field is populated from `billingEvent.resolvedCostCenterCode` in the API response.

Display:
```
Cost Centre Code: WC-RP42-PL  (computed dynamically)
```

Show a tooltip or info icon explaining: "This code is assembled dynamically from the product, reception point, and service responsibility segments."

**No new API file is needed** — the value is part of the existing `GET /api/v1/billing-events/{id}` response that the detail page already calls.

---

## Verification Checklist

1. Set up `CostCenterCompositionConfig` via `PUT /api/v1/cost-center-composition-config` with separator `"-"` and all three segments enabled.
2. Create a product with `costCenter.productSegment = "WC"`. Create a reception point with `costCenter.receptionPointSegment = "RP42"`. Create a `BillingEvent` linked to both, with `legalClassification = PUBLIC_LAW`.
3. `GET /api/v1/billing-events/{id}` — response includes `resolvedCostCenterCode: "WC-RP42-PL"`.
4. Update `CostCenterCompositionConfig` to disable `receptionPointSegmentEnabled`. Call `GET /api/v1/billing-events/{id}` again — `resolvedCostCenterCode` is now `"WC-PL"` (reception point segment absent).
5. Update separator to `"."`. Call `GET /api/v1/billing-events/{id}` — `resolvedCostCenterCode` is now `"WC.PL"`.
6. Create a `BillingEvent` with `legalClassification = PRIVATE_LAW` — `resolvedCostCenterCode` ends in `"PR"` instead of `"PL"`.
7. Create a `BillingEvent` where the product has no linked `CostCenter` — `resolvedCostCenterCode` contains only the reception point and service responsibility segments, e.g. `"RP42-PL"`.
8. Open the Billing Event detail page in the FE — confirm the "Resolved Cost Centre Code" field displays the computed value, not a stored field.
9. Change `CostCenterCompositionConfig` separator, reload the FE detail page — confirm the new separator is reflected without any data migration.
10. Call `compose()` for an event where no config record exists — confirm a clear error is thrown, not a NullPointerException.

---

## File Checklist

### Backend
- [ ] `accounting/costcenter/CostCenterCompositionConfig.java`
- [ ] `accounting/costcenter/CostCenterCompositionService.java`
- [ ] `accounting/costcenter/CostCenterCompositionConfigController.java`
- [ ] `accounting/costcenter/dto/CostCenterCompositionConfigRequest.java`
- [ ] `accounting/costcenter/dto/CostCenterCompositionConfigResponse.java`
- [ ] `masterdata/costcenter/CostCenter.java` (modification — add `productSegment`, `receptionPointSegment` fields)
- [ ] `billing/dto/BillingEventDetailResponse.java` (modification — add `resolvedCostCenterCode`)
- [ ] `billing/BillingEventService.java` (modification — call `CostCenterCompositionService.compose()` in `getById()`)

### Frontend
- [ ] `src/pages/billing/BillingEventDetailPage.jsx` (modification — add resolved cost centre code field)
