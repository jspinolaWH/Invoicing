# Step 18 — Audit Logs

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Audit entities: BillingEventAuditLog (PD-277)
- `Docs/structured_breakdown/02-data-layer.md` → Section: BillingEventAuditLogRepository — all three query types
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 1: "Audit Trail" — immutability, no UPDATE/DELETE on audit tables

---

## Goal

Build out the full audit log capability on top of the `BillingEventAuditLog` entity and repository that were created in Step 10. This step adds the read-side — the `GET /api/v1/billing-events/{id}/audit-log` endpoint, additional repository queries (user activity audit, field-type audit), and the frontend audit log panel on the billing event detail page.

The key guarantee in this step is immutability: audit log entries are never updated or deleted. This is enforced at two levels — application code (no update/delete methods on the repository) and database permissions (REVOKE UPDATE, DELETE from Step 10's migration).

---

## Backend

### 18.1 AuditLogQueryService

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/audit/AuditLogQueryService.java`

> **Requirement source:** `02-data-layer.md` — BillingEventAuditLogRepository: "change history for a billing event", "user activity audit", "changes by field type"

This service handles all read operations on the audit log. It does not expose any write methods — writes happen only in `BillingEventService` during edits.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final BillingEventAuditLogRepository auditLogRepository;

    // -----------------------------------------------------------------------
    // 1. CHANGE HISTORY FOR A BILLING EVENT
    // Used by: GET /api/v1/billing-events/{id}/audit-log
    // Source: PD-277 — "The system generates before-and-after records, ensuring
    //          transparency and providing references for customer inquiries."
    // -----------------------------------------------------------------------
    public List<AuditLogEntryResponse> getChangeHistoryForEvent(Long billingEventId) {
        return auditLogRepository
            .findByBillingEventIdOrderByChangedAtDesc(billingEventId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // -----------------------------------------------------------------------
    // 2. USER ACTIVITY AUDIT — all changes made by a user in a time range
    // Used by: internal audit review, compliance reporting
    // Source: PD-277 — "The system logs all retroactive changes and stores
    //          details such as the user who made the change."
    // -----------------------------------------------------------------------
    public List<AuditLogEntryResponse> getUserActivity(
            String userId, Instant from, Instant to) {
        return auditLogRepository
            .findByUserAndTimeRange(userId, from, to)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // -----------------------------------------------------------------------
    // 3. CHANGES BY FIELD TYPE
    // Used by: compliance review, price change investigation
    // Source: PD-319 — "Price adjustments are logged, including the reason
    //          for the change, the user who performed the update."
    // -----------------------------------------------------------------------
    public List<AuditLogEntryResponse> getChangesByField(String fieldName) {
        return auditLogRepository
            .findByFieldOrderByChangedAtDesc(fieldName)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // -----------------------------------------------------------------------
    private AuditLogEntryResponse toResponse(BillingEventAuditLog log) {
        return AuditLogEntryResponse.builder()
            .id(log.getId())
            .billingEventId(log.getBillingEventId())
            .field(log.getField())
            .oldValue(log.getOldValue())
            .newValue(log.getNewValue())
            .changedBy(log.getChangedBy())
            .changedAt(log.getChangedAt())
            .reason(log.getReason())
            .build();
    }
}
```

---

### 18.2 AuditLogEntryResponse DTO

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/audit/dto/AuditLogEntryResponse.java`

```java
@Data
@Builder
public class AuditLogEntryResponse {
    private Long id;
    private Long billingEventId;
    private String field;            // e.g. "quantity", "wasteFeePrice", "excluded"
    private String oldValue;
    private String newValue;
    private String changedBy;
    private Instant changedAt;
    private String reason;
}
```

---

### 18.3 Audit Log Endpoint in BillingEventController

Add to `BillingEventController.java`:

```java
/**
 * GET /api/v1/billing-events/{id}/audit-log
 * Returns the full change history for a billing event.
 * Role: INVOICING_USER
 */
@GetMapping("/{id}/audit-log")
public List<AuditLogEntryResponse> getAuditLog(@PathVariable Long id) {
    return auditLogQueryService.getChangeHistoryForEvent(id);
}
```

**Request/Response example — full audit trail:**

`GET /api/v1/billing-events/1001/audit-log`

Response `200 OK`:
```json
[
  {
    "id": 305,
    "billingEventId": 1001,
    "field": "excluded",
    "oldValue": "false",
    "newValue": "true",
    "changedBy": "anna.virtanen",
    "changedAt": "2024-01-16T14:00:00Z",
    "reason": "Customer dispute — do not bill until resolved"
  },
  {
    "id": 302,
    "billingEventId": 1001,
    "field": "weight",
    "oldValue": "0.240",
    "newValue": "0.480",
    "changedBy": "anna.virtanen",
    "changedAt": "2024-01-16T11:00:00Z",
    "reason": "Driver reported wrong quantity. Corrected after physical check."
  },
  {
    "id": 301,
    "billingEventId": 1001,
    "field": "quantity",
    "oldValue": "1.0",
    "newValue": "2.0",
    "changedBy": "anna.virtanen",
    "changedAt": "2024-01-16T11:00:00Z",
    "reason": "Driver reported wrong quantity. Corrected after physical check."
  }
]
```

**Response — event with no changes:**

`GET /api/v1/billing-events/1002/audit-log`

Response `200 OK`:
```json
[]
```

---

### 18.4 Admin Audit Query Endpoints

These endpoints are used by system administrators for compliance and internal audit purposes. They are not exposed to standard `INVOICING_USER` accounts.

Add to a new `AuditLogController.java`:

**File:** `invoicing/src/main/java/com/example/invoicing/billingevent/audit/AuditLogController.java`

```java
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    /**
     * GET /api/v1/audit/user-activity
     * Returns all changes made by a specific user in a time range.
     * Role: FUNCTION_ADMIN
     *
     * Params: userId, from (ISO instant), to (ISO instant)
     */
    @GetMapping("/user-activity")
    public List<AuditLogEntryResponse> getUserActivity(
        @RequestParam String userId,
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        return auditLogQueryService.getUserActivity(userId, from, to);
    }

    /**
     * GET /api/v1/audit/by-field
     * Returns all changes to a specific field across all events.
     * Role: FUNCTION_ADMIN
     *
     * Param: fieldName (e.g. "wasteFeePrice", "quantity")
     */
    @GetMapping("/by-field")
    public List<AuditLogEntryResponse> getByField(@RequestParam String fieldName) {
        return auditLogQueryService.getChangesByField(fieldName);
    }
}
```

**Request/Response example — user activity audit:**

`GET /api/v1/audit/user-activity?userId=anna.virtanen&from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z`

Response `200 OK`:
```json
[
  {
    "id": 305,
    "billingEventId": 1001,
    "field": "excluded",
    "oldValue": "false",
    "newValue": "true",
    "changedBy": "anna.virtanen",
    "changedAt": "2024-01-16T14:00:00Z",
    "reason": "Customer dispute"
  },
  {
    "id": 302,
    "billingEventId": 1001,
    "field": "weight",
    "oldValue": "0.240",
    "newValue": "0.480",
    "changedBy": "anna.virtanen",
    "changedAt": "2024-01-16T11:00:00Z",
    "reason": "Driver reported wrong quantity."
  }
]
```

**Request/Response example — changes by field:**

`GET /api/v1/audit/by-field?fieldName=wasteFeePrice`

Response `200 OK`:
```json
[
  {
    "id": 410,
    "billingEventId": 1015,
    "field": "wasteFeePrice",
    "oldValue": "10.00",
    "newValue": "12.50",
    "changedBy": "billing.manager",
    "changedAt": "2024-01-20T09:00:00Z",
    "reason": "Price list update effective January 2024"
  }
]
```

---

### 18.5 Immutability enforcement — application level

> **Requirement source:** `06-cross-cutting.md` Rule 1 — "Audit logs are never updated or deleted. Once written, they are permanent."

The `BillingEventAuditLogRepository` interface must not expose any method that would allow mutation. The following methods must NOT be present:

- No `save(BillingEventAuditLog)` that accepts a detached entity with a modified state — use `saveAll` only at insert time, never for updates
- No `delete(...)`, `deleteById(...)`, or `deleteAll(...)`
- No `@Modifying` queries with UPDATE or DELETE statements

To enforce this at the application level, the repository interface should override `delete` methods to throw `UnsupportedOperationException`:

```java
public interface BillingEventAuditLogRepository extends JpaRepository<BillingEventAuditLog, Long> {

    // READ queries (from Step 10) ...

    // -----------------------------------------------------------------------
    // Mutation guard — audit logs are insert-only
    // -----------------------------------------------------------------------
    @Override
    default void delete(BillingEventAuditLog entity) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll(Iterable<? extends BillingEventAuditLog> entities) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }
}
```

The database-level REVOKE from Step 10's migration (`REVOKE UPDATE, DELETE ON billing_event_audit_log FROM invoicing_app_user`) provides the hard-stop if application-level enforcement is ever bypassed.

---

## Frontend

### 18.6 AuditTrailTab Component (extended from Step 13)

**File:** `invoicing-fe/src/components/billing/AuditTrailTab.jsx`

The tab was introduced in Step 13 as a simple table. This step adds filtering capabilities.

**Filter controls (above the table):**
- User filter — text input, filters by `changedBy` (client-side on the loaded data)
- Date range filter — date-from / date-to, filters by `changedAt`
- Field filter — dropdown with all unique field names that appear in the audit data

**Table columns:**
| Column | Source | Notes |
|---|---|---|
| Timestamp | `changedAt` | Format: `DD.MM.YYYY HH:mm:ss` |
| User | `changedBy` | Display as-is |
| Field | `field` | Human-readable label map (see below) |
| Previous value | `oldValue` | Show `—` if null |
| New value | `newValue` | Show `—` if null |
| Reason | `reason` | Truncate to 100 chars, expand on hover |

**Human-readable field labels:**

```js
const FIELD_LABELS = {
  eventDate:         'Event Date',
  product:           'Product',
  wasteFeePrice:     'Waste Fee',
  transportFeePrice: 'Transport Fee',
  ecoFeePrice:       'Eco Fee',
  quantity:          'Quantity',
  weight:            'Weight',
  vehicleId:         'Vehicle',
  driverId:          'Driver',
  customerNumber:    'Customer Number',
  contractor:        'Contractor',
  locationId:        'Location',
  municipalityId:    'Municipality',
  comments:          'Comments',
  excluded:          'Excluded',
  status:            'Status',
}
```

**Empty state:** "No changes recorded for this event."

---

### 18.7 AuditTrailTab full implementation

```jsx
import { useState, useMemo } from 'react'
import { getAuditLog } from '../../api/billingEvents'
import { useQuery } from '@tanstack/react-query'

export default function AuditTrailTab({ eventId }) {
  const [userFilter, setUserFilter]   = useState('')
  const [fieldFilter, setFieldFilter] = useState('')
  const [dateFrom, setDateFrom]       = useState('')
  const [dateTo, setDateTo]           = useState('')

  const { data: entries = [], isLoading } = useQuery({
    queryKey: ['audit-log', eventId],
    queryFn: () => getAuditLog(eventId).then(r => r.data),
  })

  const filtered = useMemo(() => entries.filter(e => {
    if (userFilter  && !e.changedBy.toLowerCase().includes(userFilter.toLowerCase())) return false
    if (fieldFilter && e.field !== fieldFilter) return false
    if (dateFrom    && new Date(e.changedAt) < new Date(dateFrom)) return false
    if (dateTo      && new Date(e.changedAt) > new Date(dateTo + 'T23:59:59Z')) return false
    return true
  }), [entries, userFilter, fieldFilter, dateFrom, dateTo])

  const uniqueFields = [...new Set(entries.map(e => e.field))]

  if (isLoading) return <p>Loading audit trail…</p>

  return (
    <div>
      {/* Filter bar */}
      <div className="flex gap-3 mb-4 flex-wrap">
        <input
          type="text" placeholder="Filter by user"
          value={userFilter} onChange={e => setUserFilter(e.target.value)}
          className="input-field w-48"
        />
        <select
          value={fieldFilter} onChange={e => setFieldFilter(e.target.value)}
          className="input-field w-48"
        >
          <option value="">All fields</option>
          {uniqueFields.map(f => (
            <option key={f} value={f}>{FIELD_LABELS[f] ?? f}</option>
          ))}
        </select>
        <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)}
          className="input-field" />
        <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)}
          className="input-field" />
      </div>

      {/* Audit table */}
      {filtered.length === 0 ? (
        <p className="text-gray-500 text-sm">
          {entries.length === 0
            ? 'No changes recorded for this event.'
            : 'No entries match the current filters.'}
        </p>
      ) : (
        <table className="min-w-full text-sm">
          <thead>
            <tr className="bg-gray-50 text-left">
              <th className="px-3 py-2">Timestamp</th>
              <th className="px-3 py-2">User</th>
              <th className="px-3 py-2">Field</th>
              <th className="px-3 py-2">Previous Value</th>
              <th className="px-3 py-2">New Value</th>
              <th className="px-3 py-2">Reason</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(entry => (
              <tr key={entry.id} className="border-t hover:bg-gray-50">
                <td className="px-3 py-2 whitespace-nowrap">
                  {new Date(entry.changedAt).toLocaleString('fi-FI')}
                </td>
                <td className="px-3 py-2">{entry.changedBy}</td>
                <td className="px-3 py-2">{FIELD_LABELS[entry.field] ?? entry.field}</td>
                <td className="px-3 py-2 text-gray-500">{entry.oldValue ?? '—'}</td>
                <td className="px-3 py-2 font-medium">{entry.newValue ?? '—'}</td>
                <td className="px-3 py-2 max-w-xs truncate" title={entry.reason}>
                  {entry.reason}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
```

---

### 18.8 API helpers (already in billingEvents.js from Step 13)

The `getAuditLog` function was added in Step 13. No new API calls needed for the event-level audit tab.

For the admin audit query endpoints, add to a new file:

**File:** `invoicing-fe/src/api/auditLogs.js`

```js
import axios from './axios'

export const getUserActivityAudit = (userId, from, to) =>
  axios.get('/api/v1/audit/user-activity', { params: { userId, from, to } })

export const getChangesByField = (fieldName) =>
  axios.get('/api/v1/audit/by-field', { params: { fieldName } })
```

---

## Verification Checklist

1. `GET /api/v1/billing-events/{id}/audit-log` — returns entries ordered by `changedAt` DESC.
2. Edit an event (change two fields) → `GET /{id}/audit-log` returns exactly those two entries, plus any prior entries.
3. `GET /api/v1/billing-events/{id}/audit-log` for a brand-new event — returns `[]`.
4. `GET /api/v1/audit/user-activity?userId=anna.virtanen&from=...&to=...` — returns only entries where `changedBy = anna.virtanen` within the date range.
5. `GET /api/v1/audit/by-field?fieldName=wasteFeePrice` — returns all audit entries where `field = wasteFeePrice`.
6. Attempt to call `DELETE /api/v1/billing-events/{id}/audit-log` — endpoint does not exist, returns 405.
7. Attempt to call `auditLogRepository.deleteById(1L)` in a unit test — verify `UnsupportedOperationException` is thrown.
8. Attempt to run `UPDATE billing_event_audit_log SET reason = 'changed' WHERE id = 1` with the application DB user — verify permission denied (from the REVOKE in V10 migration).
9. Attempt to run `DELETE FROM billing_event_audit_log WHERE id = 1` with the application DB user — verify permission denied.
10. Frontend: AuditTrailTab loads entries for an event that has been edited.
11. Frontend: filtering by user "anna" shows only entries where `changedBy` contains "anna".
12. Frontend: filtering by field "quantity" shows only quantity-change entries.
13. Frontend: filtering by date range excludes entries outside the range.
14. Frontend: reason text longer than 100 characters is truncated with full text visible on hover.

---

## File Checklist

### Backend
- [ ] `billingevent/audit/AuditLogQueryService.java`
- [ ] `billingevent/audit/dto/AuditLogEntryResponse.java`
- [ ] `billingevent/BillingEventController.java` — `GET /{id}/audit-log` (may already exist from Step 13 — verify)
- [ ] `billingevent/audit/AuditLogController.java` — `GET /api/v1/audit/user-activity`, `GET /api/v1/audit/by-field`
- [ ] `billingevent/audit/BillingEventAuditLogRepository.java` — add delete-guard override methods

### Frontend
- [ ] `src/components/billing/AuditTrailTab.jsx` — extend with filters (if created in Step 13, update in place)
- [ ] `src/api/auditLogs.js`
