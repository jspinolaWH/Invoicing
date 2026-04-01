# Step 49 — Service Responsibility Change

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: ServiceResponsibilityChangeService
- `Docs/structured_breakdown/04-api-layer.md` → Section: Retroactive Operations — POST preview and apply for service responsibilities
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 7: "Preview-Then-Apply Pattern", Roles: FUNCTION_ADMIN required

---

## Goal
Implement `ServiceResponsibilityChangeService` using the same preview-then-apply pattern as steps 47/48. The user defines which events to change (date range, customer, product) and the new legal classification or cost center. Preview shows affected events; apply commits changes in a single transaction and writes a `ServiceResponsibilityChangeLog` (append-only audit record). FUNCTION_ADMIN role required.

---

## Backend

### 49.1 ServiceResponsibilityChangeLog Entity

**File:** `invoicing/src/main/java/com/example/invoicing/responsibility/ServiceResponsibilityChangeLog.java`

> **Requirement source:** `01-domain-model.md` — ServiceResponsibilityChangeLog (append-only)

```java
@Entity
@Table(name = "service_responsibility_change_logs")
public class ServiceResponsibilityChangeLog extends BaseAuditEntity {

    @Column(name = "change_run_id", nullable = false, unique = true)
    private String changeRunId;            // UUID

    @Column(name = "applied_by", nullable = false, length = 100)
    private String appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "affected_count", nullable = false)
    private int affectedCount;

    @Column(name = "filter_criteria_json", columnDefinition = "TEXT")
    private String filterCriteriaJson;

    @Column(name = "change_spec_json", columnDefinition = "TEXT")
    private String changeSpecJson;

    @Column(name = "preview_id")
    private Long previewId;
}
```

---

### 49.2 ServiceResponsibilityChangeSpec

**File:** `invoicing/src/main/java/com/example/invoicing/responsibility/ServiceResponsibilityChangeSpec.java`

```java
public class ServiceResponsibilityChangeSpec {
    // At least one of the following must be non-null
    private LegalClassification newLegalClassification;   // PUBLIC_LAW or PRIVATE_LAW
    private Long newCostCenterId;                         // change cost center assignment
    private String newServiceResponsibility;              // e.g. TSV, YVV (municipality-specific codes)
}
```

---

### 49.3 ServiceResponsibilityPreview Entity

**File:** `invoicing/src/main/java/com/example/invoicing/responsibility/ServiceResponsibilityPreview.java`

Same pattern as `PriceAdjustmentPreview` (step 47):

```java
@Entity
@Table(name = "service_responsibility_previews")
public class ServiceResponsibilityPreview extends BaseAuditEntity {

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "applied", nullable = false)
    private boolean applied = false;

    @Column(name = "filter_criteria_json", columnDefinition = "TEXT")
    private String filterCriteriaJson;

    @Column(name = "change_spec_json", columnDefinition = "TEXT")
    private String changeSpecJson;

    @Column(name = "affected_event_count")
    private int affectedEventCount;

    @Column(name = "affected_events_json", columnDefinition = "TEXT")
    private String affectedEventsJson;
}
```

TTL: 30 minutes (same as price adjustment preview).

---

### 49.4 ServiceResponsibilityChangeService

**File:** `invoicing/src/main/java/com/example/invoicing/responsibility/ServiceResponsibilityChangeService.java`

> **Requirement source:** `03-business-logic.md` — ServiceResponsibilityChangeService

**Preview method:**
```java
@Transactional(readOnly = true)
public ServiceResponsibilityPreviewResponse preview(ServiceResponsibilityChangeRequest request) {
    List<BillingEvent> affected = billingEventRepository.findByPriceAdjustmentFilter(
        request.getFilter().getDateFrom(),
        request.getFilter().getDateTo(),
        request.getFilter().getProductIds(),
        request.getFilter().getCustomerId(),
        BillingEventStatus.IN_PROGRESS);

    List<AffectedResponsibilityEvent> previews = affected.stream()
        .map(event -> new AffectedResponsibilityEvent(
            event.getId(),
            event.getCustomer().getId(),
            event.getEventDate(),
            event.getLegalClassification(),        // current
            request.getSpec().getNewLegalClassification(),  // new (if changing classification)
            event.getCostCenter() != null ? event.getCostCenter().getCode() : null,
            request.getSpec().getNewCostCenterId()
        ))
        .toList();

    ServiceResponsibilityPreview preview = new ServiceResponsibilityPreview();
    preview.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
    preview.setAffectedEventCount(previews.size());
    preview.setFilterCriteriaJson(toJson(request.getFilter()));
    preview.setChangeSpecJson(toJson(request.getSpec()));
    preview.setAffectedEventsJson(toJson(previews));

    ServiceResponsibilityPreview saved = previewRepository.save(preview);
    return new ServiceResponsibilityPreviewResponse(saved.getId(), saved.getExpiresAt(), previews);
}
```

**Apply method:**
```java
@Transactional
public ServiceResponsibilityChangeLog apply(Long previewId, String appliedBy) {
    ServiceResponsibilityPreview preview = previewRepository.findById(previewId).orElseThrow(
        () -> new PreviewNotFoundException("Preview " + previewId + " not found"));
    if (preview.isApplied()) throw new PreviewAlreadyAppliedException("Already applied");
    if (Instant.now().isAfter(preview.getExpiresAt())) throw new PreviewExpiredException("Preview expired");

    ServiceResponsibilityChangeFilter filter =
        fromJson(preview.getFilterCriteriaJson(), ServiceResponsibilityChangeFilter.class);
    ServiceResponsibilityChangeSpec spec =
        fromJson(preview.getChangeSpecJson(), ServiceResponsibilityChangeSpec.class);

    List<BillingEvent> events = billingEventRepository.findByPriceAdjustmentFilter(
        filter.getDateFrom(), filter.getDateTo(),
        filter.getProductIds(), filter.getCustomerId(),
        BillingEventStatus.IN_PROGRESS);

    for (BillingEvent event : events) {
        String oldClassification = event.getLegalClassification() != null
            ? event.getLegalClassification().name() : null;

        if (spec.getNewLegalClassification() != null) {
            event.setLegalClassification(spec.getNewLegalClassification());
        }
        if (spec.getNewCostCenterId() != null) {
            event.setCostCenter(costCenterRepository.getReferenceById(spec.getNewCostCenterId()));
        }

        auditLogService.logResponsibilityChange(event.getId(), oldClassification,
            spec.getNewLegalClassification() != null ? spec.getNewLegalClassification().name() : null,
            appliedBy, "Service responsibility change run " + previewId);
    }
    billingEventRepository.saveAll(events);

    preview.setApplied(true);
    previewRepository.save(preview);

    ServiceResponsibilityChangeLog log = new ServiceResponsibilityChangeLog();
    log.setChangeRunId(UUID.randomUUID().toString());
    log.setAppliedBy(appliedBy);
    log.setAppliedAt(Instant.now());
    log.setAffectedCount(events.size());
    log.setFilterCriteriaJson(preview.getFilterCriteriaJson());
    log.setChangeSpecJson(preview.getChangeSpecJson());
    log.setPreviewId(previewId);
    return changeLogRepository.save(log);
}
```

---

### 49.5 Controller

**File:** `invoicing/src/main/java/com/example/invoicing/responsibility/ServiceResponsibilityController.java`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/v1/service-responsibilities/retroactive-change/preview` | Preview the change | FUNCTION_ADMIN |
| POST | `/api/v1/service-responsibilities/retroactive-change/{previewId}/apply` | Apply the change | FUNCTION_ADMIN |

**POST preview request:**
```json
{
  "filter": {
    "dateFrom": "2024-01-01",
    "dateTo": "2024-01-31",
    "customerId": 99,
    "productIds": null
  },
  "spec": {
    "newLegalClassification": "PRIVATE_LAW",
    "newCostCenterId": null
  }
}
```

**POST preview response:**
```json
{
  "previewId": 88,
  "expiresAt": "2024-02-01T10:30:00Z",
  "affectedEventCount": 42,
  "affectedEvents": [
    {
      "eventId": 5001,
      "customerId": 99,
      "eventDate": "2024-01-10",
      "currentClassification": "PUBLIC_LAW",
      "newClassification": "PRIVATE_LAW"
    }
  ]
}
```

**POST apply response:**
```json
{
  "changeRunId": "c5f2a3b4-...",
  "appliedBy": "admin@company.fi",
  "appliedAt": "2024-02-01T10:15:00Z",
  "affectedCount": 42,
  "previewId": 88
}
```

---

## Frontend

### 49.6 Service Responsibility Change Wizard

**File:** `invoicing-fe/src/pages/adjustments/ServiceResponsibilityWizard.jsx`

Same three-step structure as `PriceAdjustmentWizard` (step 47/48):

**Step 1 — Define Criteria:**
- **FilterForm** — date range, customer search, product filter.
- **ChangeSpecForm** — classification selector (PUBLIC_LAW / PRIVATE_LAW / no change), cost center selector (searchable dropdown / no change).

**Step 2 — Review Affected Events:**
- **AffectedResponsibilityTable** — columns: Event ID, Customer, Date, Current Classification, New Classification, Current Cost Center, New Cost Center.

**Step 3 — Confirm and Apply:**
- Same confirmation + `AuditRunCard` pattern as step 48.

**API calls via `src/api/serviceResponsibilities.js`:**
```js
export const previewResponsibilityChange = (data) =>
  axios.post('/api/v1/service-responsibilities/retroactive-change/preview', data)

export const applyResponsibilityChange = (previewId) =>
  axios.post(`/api/v1/service-responsibilities/retroactive-change/${previewId}/apply`)
```

---

## Verification Checklist

1. `POST preview` with customer 99 over January 2024 → returns 42 affected events with current and new classification.
2. Events with status != IN_PROGRESS excluded from preview.
3. `POST apply` with valid preview → all 42 events updated: `legalClassification` changed to PRIVATE_LAW.
4. `BillingEventAuditLog` records created for each event with old/new classification and `appliedBy`.
5. `ServiceResponsibilityChangeLog` record persisted with `affectedCount = 42`.
6. Apply expired preview → HTTP 410 Gone.
7. Apply already-applied preview → HTTP 409 Conflict.
8. Apply by INVOICING_USER → HTTP 403 Forbidden.
9. Change only cost center (classification unchanged): verify `newLegalClassification` field on spec is null; only `costCenter` updated on events.
10. Open wizard in FE — step 2 table shows current and new values; step 3 commit button triggers apply; `AuditRunCard` shown after success.

---

## File Checklist

### Backend
- [ ] `responsibility/ServiceResponsibilityChangeLog.java`
- [ ] `responsibility/ServiceResponsibilityChangeLogRepository.java`
- [ ] `responsibility/ServiceResponsibilityPreview.java`
- [ ] `responsibility/ServiceResponsibilityPreviewRepository.java`
- [ ] `responsibility/ServiceResponsibilityChangeService.java`
- [ ] `responsibility/ServiceResponsibilityController.java`
- [ ] `responsibility/ServiceResponsibilityChangeFilter.java`
- [ ] `responsibility/ServiceResponsibilityChangeSpec.java`
- [ ] `responsibility/AffectedResponsibilityEvent.java`
- [ ] `responsibility/dto/ServiceResponsibilityChangeRequest.java`
- [ ] `responsibility/dto/ServiceResponsibilityPreviewResponse.java`

### Frontend
- [ ] `src/pages/adjustments/ServiceResponsibilityWizard.jsx`
- [ ] `src/pages/adjustments/components/ChangeSpecForm.jsx`
- [ ] `src/pages/adjustments/components/AffectedResponsibilityTable.jsx`
- [ ] `src/api/serviceResponsibilities.js`
