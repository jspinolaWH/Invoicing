# Step 10 — BillingEvent Entity + Repository

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: BillingEvent (all fields), Audit entities (BillingEventAuditLog)
- `Docs/structured_breakdown/02-data-layer.md` → Section: BillingEventRepository (all eight query groups)
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 4: "Status Gate — Immutability After SENT", Rule 1: Audit Trail base fields

---

## Goal

Define the `BillingEvent` JPA entity with every field required by the domain model, the `BillingEventStatus` enum that drives the state machine in Step 11, and the `BillingEventRepository` with all query methods needed by downstream services. This entity is the most central record in the entire invoicing system — every feature from Step 11 onward reads or writes it.

A secondary goal is to get the `BillingEventAuditLog` entity and repository in place now so that Steps 13 and 18 can build directly on them without retroactive schema changes.

---

## Backend

### 10.1 BillingEventStatus Enum

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventStatus.java`

> **Requirement source:** `01-domain-model.md` — "IN_PROGRESS: events that have been recorded, but for which an invoice has not yet been created. Sent: events for which an invoice and invoice data have been generated, but not yet sent to the invoicing system. Error: events that contain incorrect data or where errors occurred during the transfer. Completed: an invoice has been created from the event and the invoice data has been successfully sent to and confirmed by the invoicing system." (PD-297)

```java
public enum BillingEventStatus {
    IN_PROGRESS,
    SENT,
    COMPLETED,
    ERROR
}
```

---

### 10.2 LegalClassification Enum

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/LegalClassification.java`

> **Requirement source:** `01-domain-model.md` — "The event carries a legal classification field: PUBLIC_LAW or PRIVATE_LAW. This is set at creation time." (PD-285)

```java
public enum LegalClassification {
    PUBLIC_LAW,
    PRIVATE_LAW
}
```

---

### 10.3 BillingEvent Entity

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEvent.java`

> **Requirement source:** `01-domain-model.md` — BillingEvent section, listing all required fields

```java
@Entity
@Table(name = "billing_events")
@Getter
@Setter
@NoArgsConstructor
public class BillingEvent extends BaseAuditEntity {

    // ---- WHEN / WHAT ----

    @Column(nullable = false)
    private LocalDate eventDate;                        // "Date — when it happened" (PD-299)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;                            // "Product / service — what was done" (PD-299)

    // ---- PRICING (all BigDecimal — financial precision required) ----

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal wasteFeePrice;                   // Waste treatment fee component (PD-299)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal transportFeePrice;               // Transport fee component (PD-299)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal ecoFeePrice;                     // Eco-fee component (PD-299 PJH addition)

    // ---- QUANTITY / WEIGHT ----

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;                        // pcs or m³ (PD-299)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal weight;                          // tonnes or kg (PD-299)

    // ---- VAT ----

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate0;                        // 0% VAT amount applied to this event (PD-299)

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate24;                       // 24% VAT amount applied to this event (PD-299)
    // Note: VAT is resolved by eventDate, never by today's date — see 06-cross-cutting.md Rule 9

    // ---- VEHICLE / DRIVER ----

    @Column(length = 20)
    private String vehicleId;                           // Registration number (PD-228)

    @Column(length = 50)
    private String driverId;                            // Driver identifier (PD-228)

    // ---- ACCOUNTING ALLOCATION ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;                      // FK to CostCenter master data (PD-295)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id")
    private AccountingAccount accountingAccount;        // FK to AccountingAccount (PD-295)

    // ---- CUSTOMER / CONTRACTOR ----

    @Column(nullable = false, length = 9)
    private String customerNumber;                      // 6-9 digit customer identifier (PD-298)

    @Column(length = 200)
    private String contractor;                          // Who performed the service (PD-299)

    // ---- LOCATION / GEOGRAPHY ----

    @Column(length = 100)
    private String locationId;                          // Load/unloading location (PD-299)

    @Column(length = 100)
    private String municipalityId;                      // Municipality (PD-293)

    // ---- SHARED SERVICE ----

    @Column(precision = 5, scale = 2)
    private BigDecimal sharedServiceGroupPercentage;    // This participant's share (PD-280)

    @Column(length = 100)
    private String sharedServiceGroupId;                // PropertyGroup reference (PD-280)

    // ---- METADATA ----

    @Column(length = 2000)
    private String comments;                            // Free-text comment (PD-299)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingEventStatus status = BillingEventStatus.IN_PROGRESS; // Default per PD-297

    // ---- EXCLUSION (PD-318) ----

    @Column(nullable = false)
    private boolean excluded = false;                   // "Events marked as excluded remain in the system"

    @Column(length = 1000)
    private String exclusionReason;                     // Required when excluded = true

    @Column(length = 100)
    private String excludedBy;                          // User who excluded this event

    private Instant excludedAt;                         // Timestamp of exclusion

    // ---- BILLABILITY ----

    @Column(nullable = false)
    private boolean nonBillable = false;                // Contractor-only events (PD-318)

    // ---- CLASSIFICATION (PD-285) ----

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LegalClassification legalClassification;    // PUBLIC_LAW or PRIVATE_LAW

    // ---- OFFICE REVIEW (PD-228) ----

    @Column(nullable = false)
    private boolean officeReviewRequired = false;       // Set by DriverEventService

    @Column(length = 100)
    private String reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String rejectionReason;                     // Populated on rejection

    // ---- PROJECT (PD-287) ----

    @Column(length = 100)
    private String projectId;                           // Optional project/work-site reference

    // ---- ORIGIN ----

    @Column(length = 50)
    private String origin;                              // MANUAL, DRIVER, INTEGRATION, SEASONAL
}
```

**Why every price field is `BigDecimal`:** `06-cross-cutting.md` Rule 8 explicitly requires exact decimal arithmetic and forbids floating-point for all financial values in this system.

**Why `excluded` has its own `excludedBy`/`excludedAt`:** The audit trail for exclusion (PD-318) must record who excluded the event and when, in addition to the generic `lastModifiedBy` from `BaseAuditEntity`.

---

### 10.4 BillingEventRepository

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/BillingEventRepository.java`

> **Requirement source:** `02-data-layer.md` — BillingEventRepository section, all eight query groups. Note that EVERY billing query includes `AND e.excluded = false` — this is non-negotiable per `02-data-layer.md`: "Excluded events must never appear in billing queries. This filter must be in every relevant query definition."

```java
public interface BillingEventRepository extends JpaRepository<BillingEvent, Long> {

    // -----------------------------------------------------------------------
    // 1. UNBILLED EVENTS IN A DATE RANGE
    // Used by: RetroactivePriceAdjustmentService, ServiceResponsibilityChangeService
    // Source: PD-319 — "The system identifies all unbilled events for the specified time period"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.customerNumber = :customerNumber
          AND e.eventDate >= :from
          AND e.eventDate <= :to
          AND e.status = 'IN_PROGRESS'
          AND e.excluded = false
        """)
    List<BillingEvent> findUnbilledByCustomerAndDateRange(
        @Param("customerNumber") String customerNumber,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    // -----------------------------------------------------------------------
    // 2. EVENTS BY INVOICE RUN FILTER (dynamic — all params nullable)
    // Used by: InvoiceRunService
    // Source: PD-293 — "Selection criteria: municipality, invoicing period, customer type,
    //         service type, reception location, service responsibility"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.excluded = false
          AND (:municipalityId IS NULL OR e.municipalityId = :municipalityId)
          AND (:dateFrom     IS NULL OR e.eventDate >= :dateFrom)
          AND (:dateTo       IS NULL OR e.eventDate <= :dateTo)
          AND (:productId    IS NULL OR e.product.id = :productId)
          AND (:customerType IS NULL OR e.legalClassification = :customerType)
          AND (:locationId   IS NULL OR e.locationId = :locationId)
        """)
    List<BillingEvent> findByRunFilter(
        @Param("municipalityId")  String municipalityId,
        @Param("dateFrom")        LocalDate dateFrom,
        @Param("dateTo")          LocalDate dateTo,
        @Param("productId")       Long productId,
        @Param("customerType")    LegalClassification customerType,
        @Param("locationId")      String locationId
    );

    // -----------------------------------------------------------------------
    // 3. EVENTS PENDING OFFICE REVIEW
    // Used by: DriverEventService, review queue endpoint
    // Source: PD-228 — "Events must be able to be reviewed in bulk for error detection"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.officeReviewRequired = true
          AND e.reviewedAt IS NULL
          AND e.excluded = false
        ORDER BY e.eventDate ASC
        """)
    List<BillingEvent> findPendingOfficeReview();

    // -----------------------------------------------------------------------
    // 4. EVENTS BY SHARED SERVICE GROUP
    // Used by: SharedServiceInvoicingService (retroactive redistribution)
    // Source: PD-279 — "All events falling within the date range are redistributed"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.sharedServiceGroupId = :groupId
          AND e.status = 'IN_PROGRESS'
          AND e.eventDate >= :effectiveFrom
          AND e.excluded = false
        """)
    List<BillingEvent> findBySharedServiceGroup(
        @Param("groupId")       String groupId,
        @Param("effectiveFrom") LocalDate effectiveFrom
    );

    // -----------------------------------------------------------------------
    // 5. EVENTS BY PROJECT
    // Used by: InvoiceGenerationService (separate invoices per project)
    // Source: PD-287 — "Work-site-specific events must be generated on separate invoices"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.projectId = :projectId
          AND e.status = 'IN_PROGRESS'
          AND e.excluded = false
        """)
    List<BillingEvent> findByProject(@Param("projectId") String projectId);

    // -----------------------------------------------------------------------
    // 6. EVENTS BY BILLING CYCLE WINDOW
    // Used by: InvoiceRunService (cycle-based grouping)
    // Source: PD-291 — "Events are grouped according to their billing period"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.customerNumber = :customerNumber
          AND e.status = 'IN_PROGRESS'
          AND e.eventDate >= :cycleStart
          AND e.eventDate <= :cycleEnd
          AND e.excluded = false
        """)
    List<BillingEvent> findByCycleWindow(
        @Param("customerNumber") String customerNumber,
        @Param("cycleStart")     LocalDate cycleStart,
        @Param("cycleEnd")       LocalDate cycleEnd
    );

    // -----------------------------------------------------------------------
    // 7. FIND EXCLUDED EVENTS (for review and audit — excluded events remain visible)
    // Source: PD-318 — "Events marked as excluded remain in the system but are flagged"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.excluded = true
          AND (:customerNumber IS NULL OR e.customerNumber = :customerNumber)
          AND (:from IS NULL OR e.eventDate >= :from)
          AND (:to   IS NULL OR e.eventDate <= :to)
        ORDER BY e.excludedAt DESC
        """)
    List<BillingEvent> findExcluded(
        @Param("customerNumber") String customerNumber,
        @Param("from")           LocalDate from,
        @Param("to")             LocalDate to
    );

    // -----------------------------------------------------------------------
    // 8. EVENTS WITH MISSING MANDATORY DATA
    // Used by: BillingEventValidationService pre-flight check
    // Source: PD-278 — "An event must have mandatory data (accounts, cost centers, VAT)"
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE e.status = 'IN_PROGRESS'
          AND e.excluded = false
          AND (e.accountingAccount IS NULL
               OR e.costCenter IS NULL
               OR e.vatRate0 IS NULL
               OR e.vatRate24 IS NULL
               OR e.customerNumber IS NULL
               OR e.product IS NULL)
        """)
    List<BillingEvent> findWithMissingMandatoryData();

    // -----------------------------------------------------------------------
    // Standard filtered list — used by GET /api/v1/billing-events
    // -----------------------------------------------------------------------
    @Query("""
        SELECT e FROM BillingEvent e
        WHERE (:customerNumber IS NULL OR e.customerNumber = :customerNumber)
          AND (:status         IS NULL OR e.status = :status)
          AND (:municipalityId IS NULL OR e.municipalityId = :municipalityId)
          AND (:dateFrom       IS NULL OR e.eventDate >= :dateFrom)
          AND (:dateTo         IS NULL OR e.eventDate <= :dateTo)
          AND (:productId      IS NULL OR e.product.id = :productId)
          AND (:excluded       IS NULL OR e.excluded = :excluded)
          AND (:requiresReview IS NULL OR e.officeReviewRequired = :requiresReview)
        ORDER BY e.eventDate DESC
        """)
    Page<BillingEvent> findFiltered(
        @Param("customerNumber") String customerNumber,
        @Param("status")         BillingEventStatus status,
        @Param("municipalityId") String municipalityId,
        @Param("dateFrom")       LocalDate dateFrom,
        @Param("dateTo")         LocalDate dateTo,
        @Param("productId")      Long productId,
        @Param("excluded")       Boolean excluded,
        @Param("requiresReview") Boolean requiresReview,
        Pageable pageable
    );
}
```

---

### 10.5 BillingEventAuditLog Entity

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/audit/BillingEventAuditLog.java`

> **Requirement source:** `01-domain-model.md` — Audit entities: "BillingEventAuditLog — field-level change record for any edit to a BillingEvent. These are append-only. Nothing in the system deletes from them." (PD-277)

```java
@Entity
@Table(name = "billing_event_audit_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long billingEventId;      // FK (not a JPA relation — audit logs survive event deletes)

    @Column(nullable = false, length = 100)
    private String field;             // e.g. "quantity", "wasteFeePrice"

    @Column(length = 2000)
    private String oldValue;          // String representation of the old value

    @Column(length = 2000)
    private String newValue;          // String representation of the new value

    @Column(nullable = false, length = 100)
    private String changedBy;         // User principal

    @Column(nullable = false)
    private Instant changedAt;        // UTC timestamp

    @Column(nullable = false, length = 2000)
    private String reason;            // Mandatory — no silent changes (PD-277)
}
```

**Why no `BaseAuditEntity`:** The audit log is itself an audit record. Adding `createdBy`/`lastModifiedBy` from `BaseAuditEntity` would be redundant. The log carries `changedBy` and `changedAt` directly.

**Why `billingEventId` is a plain `Long` rather than a `@ManyToOne`:** Audit logs must survive even if the parent event is deleted (which should not happen, but if it does the audit trail must not be lost). A plain column avoids a cascading delete risk.

---

### 10.6 BillingEventAuditLogRepository

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/audit/BillingEventAuditLogRepository.java`

> **Requirement source:** `02-data-layer.md` — BillingEventAuditLogRepository section

```java
public interface BillingEventAuditLogRepository extends JpaRepository<BillingEventAuditLog, Long> {

    // All changes for a single event — for the audit trail tab (PD-277)
    List<BillingEventAuditLog> findByBillingEventIdOrderByChangedAtDesc(Long billingEventId);

    // All changes by a user in a time window — for internal user activity audit (PD-277)
    @Query("""
        SELECT a FROM BillingEventAuditLog a
        WHERE a.changedBy = :user
          AND a.changedAt >= :from
          AND a.changedAt <= :to
        ORDER BY a.changedAt DESC
        """)
    List<BillingEventAuditLog> findByUserAndTimeRange(
        @Param("user") String user,
        @Param("from") Instant from,
        @Param("to")   Instant to
    );

    // All changes to a specific field type across events — for field-type audit
    List<BillingEventAuditLog> findByFieldOrderByChangedAtDesc(String field);
}
```

**Immutability enforcement:** `06-cross-cutting.md` Rule 1 states the DB application user must have INSERT permission but NOT UPDATE or DELETE on this table. Add a Flyway migration that explicitly REVOKES these:
```sql
REVOKE UPDATE, DELETE ON billing_event_audit_log FROM invoicing_app_user;
```

---

### 10.7 Flyway Migration

**File:** `invoicing/src/main/resources/db/migration/V10__billing_event.sql`

```sql
CREATE TABLE billing_events (
    id                            BIGSERIAL PRIMARY KEY,
    event_date                    DATE           NOT NULL,
    product_id                    BIGINT         NOT NULL REFERENCES products(id),
    waste_fee_price               NUMERIC(19,4)  NOT NULL,
    transport_fee_price           NUMERIC(19,4)  NOT NULL,
    eco_fee_price                 NUMERIC(19,4)  NOT NULL,
    quantity                      NUMERIC(19,4)  NOT NULL,
    weight                        NUMERIC(19,4)  NOT NULL,
    vat_rate_0                    NUMERIC(5,2)   NOT NULL,
    vat_rate_24                   NUMERIC(5,2)   NOT NULL,
    vehicle_id                    VARCHAR(20),
    driver_id                     VARCHAR(50),
    cost_center_id                BIGINT         REFERENCES cost_centers(id),
    accounting_account_id         BIGINT         REFERENCES accounting_accounts(id),
    customer_number               VARCHAR(9)     NOT NULL,
    contractor                    VARCHAR(200),
    location_id                   VARCHAR(100),
    municipality_id               VARCHAR(100),
    shared_service_group_id       VARCHAR(100),
    shared_service_group_percentage NUMERIC(5,2),
    comments                      VARCHAR(2000),
    status                        VARCHAR(20)    NOT NULL DEFAULT 'IN_PROGRESS',
    excluded                      BOOLEAN        NOT NULL DEFAULT FALSE,
    exclusion_reason              VARCHAR(1000),
    excluded_by                   VARCHAR(100),
    excluded_at                   TIMESTAMPTZ,
    non_billable                  BOOLEAN        NOT NULL DEFAULT FALSE,
    legal_classification          VARCHAR(20),
    office_review_required        BOOLEAN        NOT NULL DEFAULT FALSE,
    reviewed_by                   VARCHAR(100),
    reviewed_at                   TIMESTAMPTZ,
    rejection_reason              VARCHAR(1000),
    project_id                    VARCHAR(100),
    origin                        VARCHAR(50),
    created_by                    VARCHAR(100),
    created_at                    TIMESTAMPTZ,
    last_modified_by              VARCHAR(100),
    last_modified_at              TIMESTAMPTZ
);

CREATE INDEX idx_billing_events_customer_date   ON billing_events(customer_number, event_date);
CREATE INDEX idx_billing_events_status          ON billing_events(status);
CREATE INDEX idx_billing_events_excluded        ON billing_events(excluded);
CREATE INDEX idx_billing_events_review          ON billing_events(office_review_required, reviewed_at);
CREATE INDEX idx_billing_events_shared_group    ON billing_events(shared_service_group_id);
CREATE INDEX idx_billing_events_project         ON billing_events(project_id);

CREATE TABLE billing_event_audit_log (
    id                BIGSERIAL    PRIMARY KEY,
    billing_event_id  BIGINT       NOT NULL,
    field             VARCHAR(100) NOT NULL,
    old_value         VARCHAR(2000),
    new_value         VARCHAR(2000),
    changed_by        VARCHAR(100) NOT NULL,
    changed_at        TIMESTAMPTZ  NOT NULL,
    reason            VARCHAR(2000) NOT NULL
);

CREATE INDEX idx_audit_log_event_id  ON billing_event_audit_log(billing_event_id);
CREATE INDEX idx_audit_log_changed_by ON billing_event_audit_log(changed_by);

-- Revoke mutation permissions from the application user
-- Replace 'invoicing_app_user' with your actual DB user
REVOKE UPDATE, DELETE ON billing_event_audit_log FROM invoicing_app_user;
```

---

## Frontend

### 10.8 BillingEvent List Page

**File:** `invoicing-fe/src/pages/billing/BillingEventsPage.jsx`

This is the primary entry point for billing staff. It shows a paginated, filterable table of billing events.

**Filter bar fields:**
- Date range (From / To) — `eventDate`
- Status dropdown: All / IN_PROGRESS / SENT / COMPLETED / ERROR
- Customer number (text input)
- Product (dropdown populated from `/api/v1/products`)
- Excluded toggle (three options: All / Active only / Excluded only)

**Table columns:**
| Column | Source field |
|---|---|
| Date | `eventDate` |
| Customer # | `customerNumber` |
| Product | `product.name` |
| Waste Fee | `wasteFeePrice` |
| Transport Fee | `transportFeePrice` |
| Eco Fee | `ecoFeePrice` |
| Qty | `quantity` |
| Weight | `weight` |
| Status | `status` (badge — see Step 11) |
| Excluded | icon if `excluded = true` |
| Actions | View / Edit |

**API module:**

**File:** `invoicing-fe/src/api/billingEvents.js`

```js
import axios from './axios'

export const getBillingEvents = (params) =>
  axios.get('/api/v1/billing-events', { params })

export const getBillingEvent = (id) =>
  axios.get(`/api/v1/billing-events/${id}`)

export const createBillingEvent = (data) =>
  axios.post('/api/v1/billing-events', data)

export const createManualBillingEvent = (data) =>
  axios.post('/api/v1/billing-events/manual', data)

export const updateBillingEvent = (id, data) =>
  axios.patch(`/api/v1/billing-events/${id}`, data)
```

---

## Verification Checklist

1. Run `mvn spring-boot:run` — Hibernate creates `billing_events` and `billing_event_audit_log` tables with all columns.
2. Verify all indexes exist: `\d billing_events` in psql shows `idx_billing_events_customer_date`, `idx_billing_events_status`, `idx_billing_events_excluded`.
3. Verify the REVOKE statement was applied: attempt an UPDATE on `billing_event_audit_log` with the app user — it should be denied.
4. `POST /api/v1/billing-events` with a full payload including all required fields — returns 201 with the created event including `id`, `status: "IN_PROGRESS"`, `excluded: false`.
5. `GET /api/v1/billing-events?status=IN_PROGRESS` — returns the created event.
6. `GET /api/v1/billing-events?excluded=false` — event appears; `?excluded=true` returns empty list.
7. Manually set `excluded = true` directly in the DB, then call `findByRunFilter` — the excluded event must NOT appear in the result.
8. Manually set `status = SENT` directly in the DB, then call `findByRunFilter` — the sent event must NOT appear (status filter).
9. `GET /api/v1/billing-events?dateFrom=2024-01-01&dateTo=2024-01-31` — returns only events in that window.
10. Frontend: navigate to `/billing-events` — filter bar renders, table loads from API, filters update results on apply.

---

## File Checklist

### Backend
- [ ] `billingevent/BillingEventStatus.java`
- [ ] `billingevent/LegalClassification.java`
- [ ] `billingevent/BillingEvent.java`
- [ ] `billingevent/BillingEventRepository.java`
- [ ] `billingevent/audit/BillingEventAuditLog.java`
- [ ] `billingevent/audit/BillingEventAuditLogRepository.java`
- [ ] `db/migration/V10__billing_event.sql`

### Frontend
- [ ] `src/api/billingEvents.js`
- [ ] `src/pages/billing/BillingEventsPage.jsx`
