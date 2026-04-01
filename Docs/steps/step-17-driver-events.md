# Step 17 — Driver Events and Office Review Queue

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: DriverEventService
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingEvent — `officeReviewRequired` field (PD-228)
- `Docs/structured_breakdown/04-api-layer.md` → Driver Events section: `POST /driver/events`, `GET /billing-events/pending-review`, `POST /billing-events/{id}/approve`, `POST /billing-events/{id}/reject`
- `Docs/structured_breakdown/06-cross-cutting.md` → Roles: DRIVER, INVOICING_USER

---

## Goal

Implement the driver mobile submission flow and the corresponding office review queue. Drivers submit events from the field via `POST /driver/events`. The service determines whether the event requires human review before it can proceed to invoicing. If review is required, the event waits in the review queue until an office user approves or rejects it.

Two distinct roles are involved: `DRIVER` (submit only) and `INVOICING_USER` (approve/reject). A driver cannot approve their own submissions.

---

## Backend

### 17.1 EventTypeConfig Entity

**File:** `invoicing/src/main/java/com/example/invoicing/driver/EventTypeConfig.java`

> **Requirement source:** `03-business-logic.md` — "Looks up the EventTypeConfig for this event/additional-task type. Sets requiresOfficeReview based on the config."

This is a configuration entity that maps event/task types to their review requirements. Admin users configure it once at system setup.

```java
@Entity
@Table(name = "event_type_configs")
@Getter
@Setter
@NoArgsConstructor
public class EventTypeConfig extends BaseAuditEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String eventTypeCode;           // Matches the code the driver app sends

    @Column(nullable = false)
    private boolean requiresOfficeReview;   // Master flag for this event type

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualQuantityThreshold; // Review if quantity exceeds this

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualWeightThreshold;   // Review if weight exceeds this

    @Column(precision = 19, scale = 4)
    private BigDecimal unusualPriceThreshold;    // Review if any price exceeds this

    @Column(nullable = false)
    private boolean reviewIfUnknownLocation = true; // Review if locationId not in known list

    @Column(length = 500)
    private String description;
}
```

---

### 17.2 DriverEventRequest

**File:** `invoicing/src/main/java/com/example/invoicing/driver/dto/DriverEventRequest.java`

> **Requirement source:** `04-api-layer.md` — "POST /driver/events: authenticated with driver credentials. Request must include driverId and vehicleId."

```java
@Data
public class DriverEventRequest {

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long productId;

    @NotBlank
    private String eventTypeCode;           // Used to look up EventTypeConfig

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal wasteFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal transportFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal ecoFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal weight;

    @NotBlank
    private String vehicleId;               // Required for driver events (PD-228)

    @NotBlank
    @Pattern(regexp = "\\d{6,9}")
    private String customerNumber;

    private String locationId;
    private String municipalityId;
    private String comments;
}
```

The `driverId` is NOT in the request body — it is taken from the authenticated principal (JWT/session of the logged-in driver). This prevents a driver from submitting events under another driver's name.

---

### 17.3 DriverEventService

**File:** `invoicing/src/main/java/com/example/invoicing/driver/DriverEventService.java`

> **Requirement source:** `03-business-logic.md` — "Records driver ID and vehicle ID. Looks up EventTypeConfig. Sets requiresOfficeReview. If false, flows directly toward invoicing. If true, sits in review queue."

```java
@Service
@RequiredArgsConstructor
@Transactional
public class DriverEventService {

    private final BillingEventService        billingEventService;
    private final EventTypeConfigRepository  eventTypeConfigRepository;

    /**
     * Receives a driver-submitted event, creates a BillingEvent,
     * and determines whether office review is required.
     *
     * Role: DRIVER — the driverId comes from the security principal, not the request body.
     */
    public BillingEventResponse submitDriverEvent(DriverEventRequest request, String driverId) {
        EventTypeConfig config = eventTypeConfigRepository
            .findByEventTypeCode(request.getEventTypeCode())
            .orElseThrow(() -> new EntityNotFoundException(
                "Unknown event type code: " + request.getEventTypeCode()));

        // Build a standard create request — drivers cannot set accounts or VAT (same as manual)
        BillingEventManualCreateRequest createReq = mapToCreateRequest(request, driverId);
        BillingEventResponse created = billingEventService.createManual(createReq, driverId);

        // Apply office review flag from config rules
        boolean reviewRequired = evaluateReviewRequired(config, request);

        // Update the event with driver-specific fields
        BillingEvent event = billingEventRepository.findById(created.getId()).orElseThrow();
        event.setDriverId(driverId);
        event.setOfficeReviewRequired(reviewRequired);
        event.setOrigin("DRIVER");
        billingEventRepository.save(event);

        return billingEventService.toResponse(event);
    }

    /**
     * Evaluates whether this specific event submission needs human review.
     * A driver is never trusted to self-approve — review is a config-driven decision.
     */
    private boolean evaluateReviewRequired(EventTypeConfig config, DriverEventRequest request) {
        if (config.isRequiresOfficeReview()) return true;

        // Unusual quantities trigger review even if the event type doesn't always require it
        if (config.getUnusualQuantityThreshold() != null
                && request.getQuantity().compareTo(config.getUnusualQuantityThreshold()) > 0) {
            return true;
        }

        if (config.getUnusualWeightThreshold() != null
                && request.getWeight().compareTo(config.getUnusualWeightThreshold()) > 0) {
            return true;
        }

        // Unusual prices trigger review
        BigDecimal highestPrice = List.of(
                request.getWasteFeePrice(),
                request.getTransportFeePrice(),
                request.getEcoFeePrice())
            .stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        if (config.getUnusualPriceThreshold() != null
                && highestPrice.compareTo(config.getUnusualPriceThreshold()) > 0) {
            return true;
        }

        // Unknown location triggers review
        if (config.isReviewIfUnknownLocation()
                && (request.getLocationId() == null || request.getLocationId().isBlank())) {
            return true;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // APPROVE
    // Called by office user — sets reviewedBy, reviewedAt, clears the review flag
    // -----------------------------------------------------------------------
    @Transactional
    public BillingEventResponse approveReview(Long eventId, String reviewerUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (!event.isOfficeReviewRequired()) {
            throw new IllegalStateException("Event " + eventId + " does not require office review.");
        }
        if (event.getReviewedAt() != null) {
            throw new IllegalStateException("Event " + eventId + " has already been reviewed.");
        }

        event.setOfficeReviewRequired(false);
        event.setReviewedBy(reviewerUser);
        event.setReviewedAt(Instant.now());

        billingEventRepository.save(event);
        return billingEventService.toResponse(event);
    }

    // -----------------------------------------------------------------------
    // REJECT
    // Called by office user — flags the event with a rejection reason.
    // Rejected events are excluded from billing but remain in the system.
    // -----------------------------------------------------------------------
    @Transactional
    public BillingEventResponse rejectReview(Long eventId, String rejectionReason, String reviewerUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (!event.isOfficeReviewRequired()) {
            throw new IllegalStateException("Event " + eventId + " does not require office review.");
        }
        if (event.getReviewedAt() != null) {
            throw new IllegalStateException("Event " + eventId + " has already been reviewed.");
        }

        // Rejection = exclude from billing with the rejection reason
        event.setExcluded(true);
        event.setExclusionReason("REJECTED: " + rejectionReason);
        event.setExcludedBy(reviewerUser);
        event.setExcludedAt(Instant.now());
        event.setRejectionReason(rejectionReason);
        event.setReviewedBy(reviewerUser);
        event.setReviewedAt(Instant.now());

        billingEventRepository.save(event);
        return billingEventService.toResponse(event);
    }

    private BillingEventManualCreateRequest mapToCreateRequest(DriverEventRequest req, String driverId) {
        BillingEventManualCreateRequest r = new BillingEventManualCreateRequest();
        r.setEventDate(req.getEventDate());
        r.setProductId(req.getProductId());
        r.setWasteFeePrice(req.getWasteFeePrice());
        r.setTransportFeePrice(req.getTransportFeePrice());
        r.setEcoFeePrice(req.getEcoFeePrice());
        r.setQuantity(req.getQuantity());
        r.setWeight(req.getWeight());
        r.setCustomerNumber(req.getCustomerNumber());
        r.setVehicleId(req.getVehicleId());
        r.setDriverId(driverId);
        r.setLocationId(req.getLocationId());
        r.setMunicipalityId(req.getMunicipalityId());
        r.setComments(req.getComments());
        return r;
    }
}
```

---

### 17.4 DriverEventController

**File:** `invoicing/src/main/java/com/example/invoicing/driver/DriverEventController.java`

> **Requirement source:** `04-api-layer.md` — driver events section. Note the separate base path `/driver/events` for mobile app endpoints.

```java
@RestController
@RequestMapping("/driver/events")
@RequiredArgsConstructor
public class DriverEventController {

    private final DriverEventService driverEventService;

    /**
     * POST /driver/events
     * Driver submits an event from mobile app.
     * Role: DRIVER
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse submit(
        @Valid @RequestBody DriverEventRequest request,
        @AuthenticationPrincipal String driverId   // comes from JWT principal
    ) {
        return driverEventService.submitDriverEvent(request, driverId);
    }
}
```

---

### 17.5 Review Endpoints in BillingEventController

Add to `BillingEventController.java`:

```java
/**
 * GET /api/v1/billing-events/pending-review
 * Returns events waiting for office review.
 * Role: INVOICING_USER
 */
@GetMapping("/pending-review")
public List<BillingEventResponse> getPendingReview() {
    return billingEventService.findPendingReview();
}

/**
 * POST /api/v1/billing-events/{id}/approve
 * Approve a driver-submitted event for invoicing.
 * Role: INVOICING_USER
 */
@PostMapping("/{id}/approve")
public BillingEventResponse approve(
    @PathVariable Long id,
    @AuthenticationPrincipal String currentUser
) {
    return driverEventService.approveReview(id, currentUser);
}

/**
 * POST /api/v1/billing-events/{id}/reject
 * Reject a driver-submitted event with a reason.
 * Role: INVOICING_USER
 */
@PostMapping("/{id}/reject")
public BillingEventResponse reject(
    @PathVariable Long id,
    @Valid @RequestBody RejectEventRequest request,
    @AuthenticationPrincipal String currentUser
) {
    return driverEventService.rejectReview(id, request.getRejectionReason(), currentUser);
}
```

**File:** `invoicing/src/main/java/com/example/invoicing/driver/dto/RejectEventRequest.java`

```java
@Data
public class RejectEventRequest {
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
```

**Request/Response example — driver submission:**

`POST /driver/events` (authenticated as driver "driver-007")
```json
{
  "eventDate": "2024-01-16",
  "productId": 7,
  "eventTypeCode": "BIO_WASTE_EMPTYING",
  "wasteFeePrice": 12.50,
  "transportFeePrice": 4.80,
  "ecoFeePrice": 0.50,
  "quantity": 1.0,
  "weight": 0.240,
  "vehicleId": "ABC-123",
  "customerNumber": "123456",
  "locationId": "LOC-042",
  "comments": "Gate was open, no issues"
}
```

Response `201 Created`:
```json
{
  "id": 1008,
  "eventDate": "2024-01-16",
  "driverId": "driver-007",
  "vehicleId": "ABC-123",
  "officeReviewRequired": false,
  "status": "IN_PROGRESS",
  "origin": "DRIVER"
}
```

Response when review is triggered (unusual quantity):
```json
{
  "id": 1009,
  "officeReviewRequired": true,
  "status": "IN_PROGRESS",
  "origin": "DRIVER"
}
```

**Request/Response example — approve:**

`POST /api/v1/billing-events/1009/approve`

Response `200 OK`:
```json
{
  "id": 1009,
  "officeReviewRequired": false,
  "reviewedBy": "anna.virtanen",
  "reviewedAt": "2024-01-16T10:00:00Z"
}
```

**Request/Response example — reject:**

`POST /api/v1/billing-events/1009/reject`
```json
{
  "rejectionReason": "Location does not match driver's route plan. Sending back for clarification."
}
```

Response `200 OK`:
```json
{
  "id": 1009,
  "excluded": true,
  "exclusionReason": "REJECTED: Location does not match driver's route plan. Sending back for clarification.",
  "rejectionReason": "Location does not match driver's route plan. Sending back for clarification.",
  "reviewedBy": "anna.virtanen",
  "reviewedAt": "2024-01-16T10:05:00Z"
}
```

---

## Frontend

### 17.6 Office Review Queue Page

**File:** `invoicing-fe/src/pages/billing/OfficeReviewQueuePage.jsx`

A dedicated page reachable from the sidebar. Shows all events with `officeReviewRequired = true` and `reviewedAt = null`.

**Columns:**
| Column | Source |
|---|---|
| Date | `eventDate` |
| Driver | `driverId` |
| Vehicle | `vehicleId` |
| Customer # | `customerNumber` |
| Product | `product.name` |
| Waste Fee | `wasteFeePrice` |
| Qty | `quantity` |
| Weight | `weight` |
| Location | `locationId` |
| Comments | `comments` |
| Actions | Approve / Reject buttons |

**Empty state:** "No events waiting for review."

**Polling or manual refresh:** Include a "Refresh" button and an auto-refresh option (every 60 seconds) since drivers submit events continuously.

---

### 17.7 Approve/Reject Inline Actions

On each row in the review queue, two action buttons:

- **Approve** (green) — calls `POST /api/v1/billing-events/{id}/approve` directly, no modal needed
- **Reject** (red) — opens a modal requiring a rejection reason before confirming

**File:** `invoicing-fe/src/components/billing/RejectReasonModal.jsx`

```jsx
export default function RejectReasonModal({ eventId, onSuccess, onClose }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async () => {
    if (!reason.trim()) return
    setLoading(true)
    try {
      await rejectBillingEvent(eventId, reason)
      onSuccess()
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="Reject Event" onClose={onClose}>
      <p className="text-sm text-gray-600 mb-3">
        Rejecting this event will exclude it from billing. Provide a clear reason.
      </p>
      <textarea
        value={reason}
        onChange={e => setReason(e.target.value)}
        rows={4}
        className="input-field w-full"
        placeholder="Required rejection reason"
      />
      <div className="flex justify-end gap-2 mt-4">
        <button onClick={onClose}>Cancel</button>
        <button
          onClick={handleSubmit}
          disabled={!reason.trim() || loading}
          className="btn-danger"
        >
          {loading ? 'Rejecting…' : 'Reject Event'}
        </button>
      </div>
    </Modal>
  )
}
```

---

### 17.8 API additions

Add to `invoicing-fe/src/api/billingEvents.js`:

```js
export const getPendingReview = () =>
  axios.get('/api/v1/billing-events/pending-review')

export const approveBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/approve`)

export const rejectBillingEvent = (id, rejectionReason) =>
  axios.post(`/api/v1/billing-events/${id}/reject`, { rejectionReason })
```

Add to `invoicing-fe/src/api/driverEvents.js` (new file — driver-app-facing):

```js
import axios from './axios'

export const submitDriverEvent = (data) =>
  axios.post('/driver/events', data)
```

---

## Verification Checklist

1. `POST /driver/events` authenticated as a DRIVER role user — verify 201, event created with `driverId` from the JWT principal (not from request body).
2. `POST /driver/events` with a quantity exceeding `unusualQuantityThreshold` in EventTypeConfig — verify `officeReviewRequired: true` in the response.
3. `POST /driver/events` with a quantity below threshold and `requiresOfficeReview: false` in EventTypeConfig — verify `officeReviewRequired: false`.
4. `POST /driver/events` without a `locationId` when `reviewIfUnknownLocation: true` — verify `officeReviewRequired: true`.
5. `GET /api/v1/billing-events/pending-review` — returns only events where `officeReviewRequired = true` AND `reviewedAt IS NULL`.
6. `POST /api/v1/billing-events/{id}/approve` — verify 200, `officeReviewRequired: false`, `reviewedBy` and `reviewedAt` are populated.
7. After approval, event appears in `GET /api/v1/billing-events?status=IN_PROGRESS` but NOT in `GET /api/v1/billing-events/pending-review`.
8. `POST /api/v1/billing-events/{id}/reject` with a reason — verify 200, `excluded: true`, `rejectionReason` is set.
9. After rejection, event does NOT appear in `GET /api/v1/billing-events/pending-review`.
10. `POST /api/v1/billing-events/{id}/approve` by a user with DRIVER role — verify 403 Forbidden.
11. `POST /api/v1/billing-events/{id}/approve` on an event that doesn't require review — verify 409.
12. Frontend: Office Review Queue page loads and shows only pending-review events.
13. Frontend: clicking Approve removes the row from the queue immediately.
14. Frontend: clicking Reject opens the modal; submitting without a reason shows a validation error.

---

## File Checklist

### Backend
- [ ] `driver/EventTypeConfig.java`
- [ ] `driver/EventTypeConfigRepository.java`
- [ ] `driver/dto/DriverEventRequest.java`
- [ ] `driver/dto/RejectEventRequest.java`
- [ ] `driver/DriverEventService.java`
- [ ] `driver/DriverEventController.java` — `POST /driver/events`
- [ ] `billingevent/BillingEventController.java` — add `GET /pending-review`, `POST /{id}/approve`, `POST /{id}/reject`
- [ ] `db/migration/V17__event_type_config.sql`

### Frontend
- [ ] `src/pages/billing/OfficeReviewQueuePage.jsx`
- [ ] `src/components/billing/RejectReasonModal.jsx`
- [ ] `src/api/billingEvents.js` — add `getPendingReview`, `approveBillingEvent`, `rejectBillingEvent`
- [ ] `src/api/driverEvents.js`
- [ ] `src/App.jsx` — add route for `/billing-events/review-queue`
