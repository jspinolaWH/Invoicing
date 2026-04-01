# Step 39 — Invoice Run Lock

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: InvoiceRun (lock tracking)
- `Docs/structured_breakdown/02-data-layer.md` → Section: ActiveRunLockRepository
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceRunLockService
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 5: "Data Locking During Billing Runs"

---

## Goal
Implement `ActiveRunLock` entity and `InvoiceRunLockService` to protect billing-critical data during an active invoice run. When a run starts, all in-scope customer IDs are locked. Any write attempt to a locked customer's billing address or billing group returns HTTP 423 Locked with a `Retry-After` header. Locks are released atomically when the run ends — whether by success, failure, or cancellation. Fast lookup by `customerNumber` is essential as it occurs on every billing profile write.

---

## Backend

### 39.1 ActiveRunLock Entity

**File:** `invoicing/src/main/java/com/example/invoicing/run/ActiveRunLock.java`

> **Requirement source:** `02-data-layer.md` — ActiveRunLockRepository; `06-cross-cutting.md` — Rule 5

```java
@Entity
@Table(name = "active_run_locks",
    indexes = {
        @Index(name = "idx_run_lock_customer", columnList = "customer_number")
    })
public class ActiveRunLock extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;
}
```

The index on `customer_number` is mandatory — lock checks occur on every billing profile write during a run, so the lookup must be sub-millisecond.

---

### 39.2 ActiveRunLockRepository

**File:** `invoicing/src/main/java/com/example/invoicing/run/ActiveRunLockRepository.java`

> **Requirement source:** `02-data-layer.md` — ActiveRunLockRepository

```java
public interface ActiveRunLockRepository extends JpaRepository<ActiveRunLock, Long> {

    // Fast existence check — called on every billing profile write
    boolean existsByCustomerNumber(String customerNumber);

    // Lookup for a specific run (used to get runId for Retry-After calculation)
    Optional<ActiveRunLock> findByCustomerNumber(String customerNumber);

    // Bulk insert at run start — native query for performance with thousands of customers
    @Modifying
    @Query(value = "INSERT INTO active_run_locks (customer_number, run_id, locked_at, created_by, created_at) " +
                   "VALUES (:customerNumber, :runId, :lockedAt, 'SYSTEM', :lockedAt)",
           nativeQuery = true)
    void insertLock(@Param("customerNumber") String customerNumber,
                    @Param("runId") Long runId,
                    @Param("lockedAt") Instant lockedAt);

    // Bulk delete at run end — one call removes all locks for the run
    @Modifying
    @Query("DELETE FROM ActiveRunLock l WHERE l.runId = :runId")
    void deleteByRunId(@Param("runId") Long runId);
}
```

For bulk insert at run start with thousands of customers, use JDBC batch insert (not JPA `save` per entity):

```java
private void bulkInsertLocks(List<String> customerNumbers, Long runId) {
    jdbcTemplate.batchUpdate(
        "INSERT INTO active_run_locks (customer_number, run_id, locked_at, created_by, created_at) " +
        "VALUES (?, ?, ?, 'SYSTEM', ?)",
        customerNumbers,
        500,  // batch size
        (ps, cn) -> {
            ps.setString(1, cn);
            ps.setLong(2, runId);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
        }
    );
}
```

---

### 39.3 InvoiceRunLockService

**File:** `invoicing/src/main/java/com/example/invoicing/run/InvoiceRunLockService.java`

> **Requirement source:** `03-business-logic.md` — InvoiceRunLockService
> **Requirement source:** `06-cross-cutting.md` — Rule 5 error message: "Invoice processing in progress. Address changes cannot be made during this time."

```java
@Service
@Transactional
public class InvoiceRunLockService {

    /**
     * Lock all customers in scope at run start.
     * Uses JDBC batch insert for performance (runs can have thousands of customers).
     */
    public void lockCustomers(Long runId, List<String> customerNumbers) {
        bulkInsertLocks(customerNumbers, runId);
        log.info("Locked {} customers for run {}", customerNumbers.size(), runId);
    }

    /**
     * Release all locks for a run (success, failure, or cancellation).
     */
    public void releaseLocksForRun(Long runId) {
        lockRepository.deleteByRunId(runId);
        log.info("Released all locks for run {}", runId);
    }

    /**
     * Called by BillingProfileService and BillingGroupService before any write.
     * Throws CustomerLockedException if the customer is in an active run.
     */
    public void assertNotLocked(String customerNumber) {
        if (lockRepository.existsByCustomerNumber(customerNumber)) {
            ActiveRunLock lock = lockRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow();
            throw new CustomerLockedException(customerNumber, lock.getRunId(), lock.getLockedAt());
        }
    }

    /**
     * Returns true/false — use when the caller wants to check without throwing.
     */
    public boolean isLocked(String customerNumber) {
        return lockRepository.existsByCustomerNumber(customerNumber);
    }
}
```

---

### 39.4 CustomerLockedException and HTTP 423 Response

**File:** `invoicing/src/main/java/com/example/invoicing/run/CustomerLockedException.java`

```java
public class CustomerLockedException extends RuntimeException {
    private final String customerNumber;
    private final Long runId;
    private final Instant lockedAt;
}
```

**Global exception handler** — maps to HTTP 423 with `Retry-After` header:

```java
@ExceptionHandler(CustomerLockedException.class)
public ResponseEntity<ErrorResponse> handleCustomerLocked(CustomerLockedException ex) {
    return ResponseEntity
        .status(HttpStatus.LOCKED)
        .header("Retry-After", "300")  // suggest retry in 5 minutes
        .body(new ErrorResponse(
            "CUSTOMER_LOCKED",
            "Invoice processing in progress. Address changes cannot be made during this time.",
            Map.of("customerNumber", ex.getCustomerNumber(), "runId", ex.getRunId())
        ));
}
```

The error message is exactly as specified in PD-270: "Invoice processing in progress. Address changes cannot be made during this time."

---

### 39.5 Integration with BillingProfileService

In `BillingProfileService.update()`:
```java
public BillingProfileResponse update(Long customerId, BillingProfileRequest request) {
    String customerNumber = getCustomerNumber(customerId);
    invoiceRunLockService.assertNotLocked(customerNumber);  // throws 423 if locked
    // ... proceed with update
}
```

Same guard in any service that updates billing groups (bundling rules) during a run.

---

## Frontend

### 39.6 Customer Locked Warning Badge

**File:** `invoicing-fe/src/components/CustomerLockedBadge.jsx`

A reusable badge component displayed on:
- Customer billing profile page (next to the "Edit" button)
- Billing group / bundling rules pages for a customer

**Props:**
```js
<CustomerLockedBadge customerNumber={customerNumber} />
```

The component makes a lightweight call to check lock status, or receives `isLocked` as a prop if the parent already has the information.

**Visual:** amber/yellow badge with text "In Active Run — Locked". Tooltip: "Billing address and billing group changes are locked while an invoice run is in progress."

**Error handling when the user tries to save anyway:**
```js
// In BillingProfileEditForm.jsx
try {
  await updateBillingProfile(customerId, formData)
} catch (err) {
  if (err.response?.status === 423) {
    setError('Invoice processing in progress. Address changes cannot be made during this time.')
  }
}
```

**API calls via `src/api/locks.js`:**
```js
export const checkCustomerLock = (customerNumber) =>
  axios.get(`/api/v1/run-locks/check/${customerNumber}`)
```

**Convenience endpoint** (optional):

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/run-locks/check/{customerNumber}` | Returns lock status for a customer | INVOICING_USER |

Response:
```json
{
  "customerNumber": "123456",
  "locked": true,
  "runId": 42,
  "lockedAt": "2024-02-01T08:00:00Z"
}
```

---

## Verification Checklist

1. Insert 1000 lock records via `lockCustomers()` — measure execution time; should complete in < 2 seconds using JDBC batch insert.
2. Call `assertNotLocked("123456")` for a locked customer → `CustomerLockedException` thrown.
3. Call `assertNotLocked("999999")` for an unlocked customer → no exception.
4. HTTP response for locked customer PUT on billing profile → status 423, `Retry-After: 300` header, message "Invoice processing in progress. Address changes cannot be made during this time."
5. `releaseLocksForRun(runId)` deletes all `ActiveRunLock` rows for the given runId; subsequent `existsByCustomerNumber` returns false.
6. Two concurrent runs cannot lock the same customer number (attempt → detect overlap before starting second run or gracefully handle).
7. `findByCustomerNumber` uses the index on `customer_number` — verify via `EXPLAIN` in PostgreSQL that the index is used.
8. Run completes with error (exception thrown mid-run) → locks are still released in the `finally` block of `InvoiceRunService`.
9. Open billing profile edit page in FE for a locked customer — `CustomerLockedBadge` is visible; "Edit" triggers 423 → FE shows the correct error message.
10. After run completes and locks are released — reload the billing profile page — badge is gone; editing succeeds.

---

## File Checklist

### Backend
- [ ] `run/ActiveRunLock.java`
- [ ] `run/ActiveRunLockRepository.java`
- [ ] `run/InvoiceRunLockService.java`
- [ ] `run/CustomerLockedException.java`
- [ ] `run/RunLockController.java` (optional check endpoint)
- [ ] `common/exception/GlobalExceptionHandler.java` — add 423 handler

### Frontend
- [ ] `src/components/CustomerLockedBadge.jsx`
- [ ] `src/api/locks.js`
- [ ] `src/pages/customers/BillingProfileEditForm.jsx` — handle 423 error response
