# Step 13 — BillingEvent Edit + Audit Log

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: BillingEventService — "Editing events before invoicing"
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 1: "Audit Trail" — before-and-after records, mandatory reason field
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT"
- `Docs/structured_breakdown/01-domain-model.md` → Audit entities — BillingEventAuditLog fields

---

## Goal

Implement `PATCH /api/v1/billing-events/{id}` with field-level audit logging. Every edit must:

1. Pass through the status gate (immutability check from Step 11)
2. Accept a mandatory `reason` field — no silent changes
3. Write one `BillingEventAuditLog` record per changed field, capturing old value, new value, user, and reason

The frontend edit form is pre-populated with existing data and includes a mandatory reason textarea. A separate audit trail tab on the detail page shows the full change history.

---

## Backend

### 13.1 BillingEventUpdateRequest

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/dto/BillingEventUpdateRequest.java`

> **Requirement source:** `03-business-logic.md` — "Accepts a partial update (only the fields being changed). The reason field is mandatory on every edit." (PD-277)
> `04-api-layer.md` — "PATCH /billing-events/{id}: Requires a reason field in the request body."

All fields are optional — only fields that are present in the request are updated. The `reason` field is mandatory regardless.

```java
@Data
public class BillingEventUpdateRequest {

    // Mandatory on every edit — no silent changes (PD-277)
    @NotBlank(message = "A reason is required for every edit")
    private String reason;

    // All the correctable fields — null means "leave unchanged"
    private LocalDate eventDate;
    private Long productId;
    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;
    private String vehicleId;
    private String driverId;
    private String customerNumber;
    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private String comments;

    // Note: status is NOT in this request — it can only change via BillingEventStatusService.
    // Note: excluded is NOT in this request — it uses dedicated exclude/reinstate endpoints.
    // Note: accountingAccount/costCenter are NOT user-editable — they are re-resolved from product.
}
```

---

### 13.2 BillingEventService — update method

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventService.java` (add to existing class)

> **Requirement source:** `03-business-logic.md` — "The service accepts a partial update, checks the event's current status (editing is blocked at SENT or COMPLETED), applies the changes, and writes a BillingEventAuditLog record for every changed field with old value, new value, user, timestamp, and reason."

```java
@Transactional
public BillingEventResponse update(Long id, BillingEventUpdateRequest req, String currentUser) {
    BillingEvent event = billingEventRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));

    // Step 1: immutability gate — throws if SENT or COMPLETED
    billingEventStatusService.assertMutable(event);

    // Step 2: apply changes and collect audit entries in a single operation
    List<BillingEventAuditLog> auditEntries = new ArrayList<>();

    if (req.getEventDate() != null && !req.getEventDate().equals(event.getEventDate())) {
        auditEntries.add(buildAuditEntry(id, "eventDate",
            event.getEventDate(), req.getEventDate(), currentUser, req.getReason()));
        event.setEventDate(req.getEventDate());
        // Re-resolve VAT if event date changed (rate depends on the date the event occurred)
        resolveVatRates(event, req.getEventDate(), event.getProduct());
    }

    if (req.getProductId() != null && !req.getProductId().equals(event.getProduct().getId())) {
        Product newProduct = loadProduct(req.getProductId());
        auditEntries.add(buildAuditEntry(id, "product",
            event.getProduct().getId(), newProduct.getId(), currentUser, req.getReason()));
        event.setProduct(newProduct);
        // Re-resolve accounting defaults when product changes (PD-295)
        resolveAccountingDefaults(event, newProduct, event.getLocationId());
    }

    applyIfChanged(event, req.getWasteFeePrice(),   event.getWasteFeePrice(),
        "wasteFeePrice",   v -> event.setWasteFeePrice(v),   id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getTransportFeePrice(), event.getTransportFeePrice(),
        "transportFeePrice", v -> event.setTransportFeePrice(v), id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getEcoFeePrice(),     event.getEcoFeePrice(),
        "ecoFeePrice",     v -> event.setEcoFeePrice(v),     id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getQuantity(),        event.getQuantity(),
        "quantity",        v -> event.setQuantity(v),        id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getWeight(),          event.getWeight(),
        "weight",          v -> event.setWeight(v),          id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getVehicleId(),       event.getVehicleId(),
        "vehicleId",       v -> event.setVehicleId(v),       id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getDriverId(),        event.getDriverId(),
        "driverId",        v -> event.setDriverId(v),        id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getCustomerNumber(),  event.getCustomerNumber(),
        "customerNumber",  v -> event.setCustomerNumber(v),  id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getContractor(),      event.getContractor(),
        "contractor",      v -> event.setContractor(v),      id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getLocationId(),      event.getLocationId(),
        "locationId",      v -> event.setLocationId(v),      id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getMunicipalityId(),  event.getMunicipalityId(),
        "municipalityId",  v -> event.setMunicipalityId(v),  id, currentUser, req.getReason(), auditEntries);
    applyIfChanged(event, req.getComments(),        event.getComments(),
        "comments",        v -> event.setComments(v),        id, currentUser, req.getReason(), auditEntries);

    // Step 3: persist — event save + all audit entries in one transaction
    billingEventRepository.save(event);
    billingEventAuditLogRepository.saveAll(auditEntries);

    return toResponse(event);
}

// -----------------------------------------------------------------------
// Utility: build an audit log entry for a field change
// -----------------------------------------------------------------------
private <T> BillingEventAuditLog buildAuditEntry(
        Long eventId, String field, T oldValue, T newValue,
        String changedBy, String reason) {
    return BillingEventAuditLog.builder()
        .billingEventId(eventId)
        .field(field)
        .oldValue(oldValue != null ? oldValue.toString() : null)
        .newValue(newValue != null ? newValue.toString() : null)
        .changedBy(changedBy)
        .changedAt(Instant.now())
        .reason(reason)
        .build();
}

// Generic helper — only emits an audit entry and applies the setter when the new value differs
private <T> void applyIfChanged(BillingEvent event, T newVal, T oldVal, String fieldName,
        Consumer<T> setter, Long eventId, String user, String reason,
        List<BillingEventAuditLog> entries) {
    if (newVal == null || newVal.equals(oldVal)) return;
    entries.add(buildAuditEntry(eventId, fieldName, oldVal, newVal, user, reason));
    setter.accept(newVal);
}
```

---

### 13.3 PATCH endpoint in BillingEventController

Add to `BillingEventController.java`:

```java
/**
 * PATCH /api/v1/billing-events/{id}
 * Partial edit with mandatory reason. Blocked by status gate if SENT/COMPLETED.
 * Role: INVOICING_USER
 */
@PatchMapping("/{id}")
public BillingEventResponse update(
    @PathVariable Long id,
    @Valid @RequestBody BillingEventUpdateRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return billingEventService.update(id, request, currentUser);
}
```

**Request/Response example — successful edit:**

`PATCH /api/v1/billing-events/1001`
```json
{
  "quantity": 2.0,
  "weight": 0.480,
  "reason": "Driver reported wrong quantity initially. Corrected after physical check."
}
```

Response `200 OK`:
```json
{
  "id": 1001,
  "quantity": 2.0,
  "weight": 0.480,
  "status": "IN_PROGRESS",
  "lastModifiedAt": "2024-01-16T11:00:00Z",
  "lastModifiedBy": "anna.virtanen"
}
```

Two `BillingEventAuditLog` rows are written for this request — one for `quantity` (old: 1.0, new: 2.0) and one for `weight` (old: 0.240, new: 0.480). Both carry the same `reason` and `changedBy`.

**Request/Response example — rejected edit (event is SENT):**

`PATCH /api/v1/billing-events/1001`
```json
{
  "quantity": 3.0,
  "reason": "Attempting to edit a sent event"
}
```

Response `409 Conflict`:
```json
{
  "error": "STATUS_TRANSITION_ERROR",
  "message": "BillingEvent 1001 is SENT and cannot be modified. Use the credit-and-re-invoice flow for corrections."
}
```

**Request/Response example — missing reason:**

`PATCH /api/v1/billing-events/1001`
```json
{
  "quantity": 2.0
}
```

Response `400 Bad Request`:
```json
{
  "error": "VALIDATION_ERROR",
  "fieldErrors": [
    { "field": "reason", "message": "A reason is required for every edit" }
  ]
}
```

---

## Frontend

### 13.4 Edit BillingEvent Form

**File:** `invoicing-fe/src/pages/billing/EditBillingEventPage.jsx`

This page loads the existing event data via `GET /api/v1/billing-events/{id}`, pre-populates all fields, and submits via `PATCH /api/v1/billing-events/{id}`.

**Pre-population:** All editable fields are populated from the fetched event on page load. Non-editable fields (status, accounting account, cost center, legal classification) are shown as read-only badges.

**Form sections:** Same layout as the Create form (Step 12) but all fields are pre-filled.

**Mandatory reason field:**
- Textarea at the bottom of the form, always visible
- Marked with a red asterisk
- Cannot submit if empty — both client-side and server-side validation

**Submit behavior:** PATCH request. On 409 Conflict (event is SENT), show a clear error banner: "This event has already been sent and cannot be edited. Use the credit-and-re-invoice flow."

---

### 13.5 Audit Trail Tab

**File:** `invoicing-fe/src/components/billing/AuditTrailTab.jsx`

Shown as a tab on the BillingEvent detail page alongside "Details" and "Status". Loads from `GET /api/v1/billing-events/{id}/audit-log`.

**Table columns:**
| Column | Source |
|---|---|
| Timestamp | `changedAt` (formatted as `DD.MM.YYYY HH:mm`) |
| User | `changedBy` |
| Field | `field` (human-readable label) |
| Previous value | `oldValue` |
| New value | `newValue` |
| Reason | `reason` |

**Empty state:** "No changes recorded. This event has not been edited since creation."

**API call:**
```js
export const getAuditLog = (eventId) =>
  axios.get(`/api/v1/billing-events/${eventId}/audit-log`)
```

---

### 13.6 BillingEvent Detail Page layout

**File:** `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx`

Tab structure:
- **Details** — all fields, read-only
- **Audit Trail** — `AuditTrailTab` component (see above)
- **Status** — `StatusTransitionPanel` (Step 11)

Header section:
- Event ID, date, customer number
- `StatusBadge` (Step 11)
- "Edit" button (disabled if `status === 'SENT' || status === 'COMPLETED'`)

---

### 13.7 API additions

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const updateBillingEvent = (id, data) =>
  axios.patch(`/api/v1/billing-events/${id}`, data)

export const getAuditLog = (id) =>
  axios.get(`/api/v1/billing-events/${id}/audit-log`)
```

---

## Verification Checklist

1. `PATCH /api/v1/billing-events/{id}` without a `reason` field — verify 400 with "A reason is required for every edit".
2. `PATCH /api/v1/billing-events/{id}` with only `quantity` changed — verify exactly ONE audit log entry is written for the `quantity` field.
3. `PATCH /api/v1/billing-events/{id}` with `quantity` and `weight` both changed — verify TWO audit log entries are written, both with the same `reason`.
4. `PATCH /api/v1/billing-events/{id}` with `quantity` set to the same value as existing — verify ZERO audit entries are written.
5. `PATCH /api/v1/billing-events/{id}` on an event with `status = SENT` — verify 409 Conflict.
6. `PATCH /api/v1/billing-events/{id}` on an event with `status = COMPLETED` — verify 409 Conflict.
7. `PATCH /api/v1/billing-events/{id}` changing `productId` — verify the accounting account and cost center are re-resolved from the new product's allocation rule.
8. `PATCH /api/v1/billing-events/{id}` changing `eventDate` — verify VAT rates are re-resolved for the new event date (not today's date).
9. `GET /api/v1/billing-events/{id}/audit-log` — returns all audit entries for that event, ordered by `changedAt` DESC.
10. Frontend: open an IN_PROGRESS event detail page — "Edit" button is active; open a SENT event — "Edit" button is disabled with a tooltip.
11. Frontend: submit the edit form without filling the reason field — form validation error appears, request is not sent.
12. Frontend: after a successful edit, the Audit Trail tab immediately shows the new entry.

---

## File Checklist

### Backend
- [ ] `billingevent/dto/BillingEventUpdateRequest.java`
- [ ] `billingevent/BillingEventService.java` — add `update()` method
- [ ] `billingevent/BillingEventController.java` — add `PATCH /{id}` endpoint
- [ ] `billingevent/BillingEventController.java` — add `GET /{id}/audit-log` endpoint

### Frontend
- [ ] `src/pages/billing/EditBillingEventPage.jsx`
- [ ] `src/pages/billing/BillingEventDetailPage.jsx`
- [ ] `src/components/billing/AuditTrailTab.jsx`
- [ ] `src/api/billingEvents.js` — add `updateBillingEvent`, `getAuditLog`
- [ ] `src/App.jsx` — add routes for `/billing-events/:id` and `/billing-events/:id/edit`
