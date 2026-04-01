# Step 32 — Shared Services (PropertyGroup + SharedServiceParticipant)

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: PropertyGroup, SharedServiceParticipant
- `Docs/structured_breakdown/02-data-layer.md` → Section: SharedServiceParticipantRepository, PropertyGroupRepository
- `Docs/structured_breakdown/04-api-layer.md` → Section: Shared Services
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 8: "The Shared Service 100% Rule"

---

## Goal
Implement `PropertyGroup` (the shared service arrangement entity) and `SharedServiceParticipant` (one record per household/customer in the group), along with strict enforcement of the 100% rule: the sum of `sharePercentage` for all active participants must equal exactly `100.00` at all times. Validation occurs on every write. The repository supports participant queries by date for retroactive redistribution.

---

## Backend

### 32.1 PropertyGroup Entity

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/PropertyGroup.java`

> **Requirement source:** `01-domain-model.md` — PropertyGroup entity

```java
@Entity
@Table(name = "property_groups")
public class PropertyGroup extends BaseAuditEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "propertyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedServiceParticipant> participants = new ArrayList<>();
}
```

---

### 32.2 SharedServiceParticipant Entity

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/SharedServiceParticipant.java`

> **Requirement source:** `01-domain-model.md` — SharedServiceParticipant entity
> **Requirement source:** `06-cross-cutting.md` — "BigDecimal with scale 2 and ROUND_HALF_UP throughout"

```java
@Entity
@Table(name = "shared_service_participants")
public class SharedServiceParticipant extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_group_id", nullable = false)
    private PropertyGroup propertyGroup;

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;             // 6–9 digit customer ID

    @Column(name = "share_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercentage;        // must use ROUND_HALF_UP, scale 2

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;                 // null = currently active, no end date
}
```

---

### 32.3 SharedServiceParticipantRepository

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/SharedServiceParticipantRepository.java`

> **Requirement source:** `02-data-layer.md` — SharedServiceParticipantRepository

```java
public interface SharedServiceParticipantRepository extends JpaRepository<SharedServiceParticipant, Long> {

    // Find participants active at a given date — critical for retroactive redistribution (PD-279)
    @Query("SELECT p FROM SharedServiceParticipant p WHERE p.propertyGroup.id = :groupId " +
           "AND p.validFrom <= :date AND (p.validTo IS NULL OR p.validTo >= :date)")
    List<SharedServiceParticipant> findActiveAtDate(
        @Param("groupId") Long groupId,
        @Param("date") LocalDate date);

    // Sum of shares for active participants — used for 100% validation
    @Query("SELECT SUM(p.sharePercentage) FROM SharedServiceParticipant p WHERE p.propertyGroup.id = :groupId " +
           "AND p.validFrom <= :today AND (p.validTo IS NULL OR p.validTo >= :today)")
    BigDecimal sumActiveSharePercentages(
        @Param("groupId") Long groupId,
        @Param("today") LocalDate today);
}
```

---

### 32.4 PropertyGroupRepository

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/PropertyGroupRepository.java`

> **Requirement source:** `02-data-layer.md` — PropertyGroupRepository

```java
public interface PropertyGroupRepository extends JpaRepository<PropertyGroup, Long> {

    // Find all groups a customer participates in — used to check for incomplete allocations
    @Query("SELECT DISTINCT p.propertyGroup FROM SharedServiceParticipant p WHERE p.customerNumber = :customerNumber")
    List<PropertyGroup> findGroupsByCustomerNumber(@Param("customerNumber") String customerNumber);
}
```

---

### 32.5 SharedServiceValidationService (100% rule enforcement)

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/SharedServiceValidationService.java`

> **Requirement source:** `06-cross-cutting.md` — Rule 8: exact decimal arithmetic, scale 2, ROUND_HALF_UP

```java
@Service
public class SharedServiceValidationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // Called before every write to a participant list
    public void validateTotalEquals100(Long groupId) {
        BigDecimal total = participantRepository
            .sumActiveSharePercentages(groupId, LocalDate.now());
        if (total == null) total = BigDecimal.ZERO;
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(HUNDRED) != 0) {
            throw new SharedServicePercentageException(
                "Participant shares sum to " + total + "% but must equal exactly 100.00%");
        }
    }

    // Returns true/false — used by the validate endpoint and pre-billing checks
    public boolean isTotalValid(Long groupId) {
        try {
            validateTotalEquals100(groupId);
            return true;
        } catch (SharedServicePercentageException e) {
            return false;
        }
    }
}
```

---

### 32.6 PropertyGroupService

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/PropertyGroupService.java`

Method signatures:
- `findAll()` → `List<PropertyGroupResponse>`
- `findById(Long id)` → `PropertyGroupResponse` (includes participants)
- `create(PropertyGroupRequest request)` → `PropertyGroupResponse`
- `replaceParticipants(Long groupId, List<ParticipantRequest> participants)` → `PropertyGroupResponse`
  - Replaces all current participants. Calls `validateTotalEquals100` after saving.
- `addParticipantRetroactive(Long groupId, RetroactiveParticipantRequest request)` → `PropertyGroupResponse`
  - Adds participant with `validFrom` in the past. Calls `SharedServiceInvoicingService.redistributeRetroactive(...)`. Calls `validateTotalEquals100` after redistribution.
- `validate(Long groupId)` → `ValidationResultResponse`

---

### 32.7 PropertyGroupController

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/PropertyGroupController.java`

Base path: `/api/v1/property-groups`

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/property-groups` | List all property groups | INVOICING_USER |
| POST | `/api/v1/property-groups` | Create a new property group | INVOICING_USER |
| GET | `/api/v1/property-groups/{id}` | Get group with all participants | INVOICING_USER |
| PUT | `/api/v1/property-groups/{id}/participants` | Replace participant list (validates 100%) | INVOICING_USER |
| POST | `/api/v1/property-groups/{id}/add-participant-retroactive` | Add participant retroactively | FUNCTION_ADMIN |
| GET | `/api/v1/property-groups/{id}/validate` | Check that shares sum to 100% | INVOICING_USER |

**POST /api/v1/property-groups request:**
```json
{
  "name": "Maple Street Shared Bio-Waste",
  "description": "Four households sharing one 240L bio-waste container"
}
```

**PUT /api/v1/property-groups/{id}/participants request:**
```json
[
  { "customerNumber": "123456", "sharePercentage": "25.00", "validFrom": "2024-01-01", "validTo": null },
  { "customerNumber": "234567", "sharePercentage": "25.00", "validFrom": "2024-01-01", "validTo": null },
  { "customerNumber": "345678", "sharePercentage": "25.00", "validFrom": "2024-01-01", "validTo": null },
  { "customerNumber": "456789", "sharePercentage": "25.00", "validFrom": "2024-01-01", "validTo": null }
]
```

**POST add-participant-retroactive request:**
```json
{
  "customerNumber": "567890",
  "sharePercentage": "20.00",
  "validFrom": "2024-02-01",
  "adjustOtherParticipants": true
}
```
`adjustOtherParticipants: true` redistributes the remaining shares equally across current participants to maintain 100%. The caller is responsible for confirming the redistribution looks correct.

**GET /api/v1/property-groups/{id}/validate response:**
```json
{
  "groupId": 1,
  "totalSharePercentage": "100.00",
  "valid": true,
  "message": null
}
```
Or on failure:
```json
{
  "groupId": 2,
  "totalSharePercentage": "85.00",
  "valid": false,
  "message": "Participant shares sum to 85.00% but must equal exactly 100.00%"
}
```

---

## Frontend

### 32.8 Property Groups Page

**File:** `invoicing-fe/src/pages/sharedservices/PropertyGroupsPage.jsx`

Components:
- **PropertyGroupsTable** — columns: Name, Description, Participant Count, Valid Status (100% indicator), Active, Actions (View / Edit).
- **PropertyGroupDetailPanel** — shows group details and `ParticipantsTable` below.
- **ParticipantsTable** — columns: Customer Number, Share %, Valid From, Valid To, Actions (Remove). Row highlighting when share total ≠ 100%.
- **ParticipantForm** — modal: customer number, share percentage, valid from, valid to (optional).
- **AddParticipantRetroactiveModal** — extended form with `validFrom` date picker and `adjustOtherParticipants` toggle.
- **PercentageSummaryBar** — visual indicator showing total percentage (green at 100%, red otherwise).

**API calls via `src/api/propertyGroups.js`:**
```js
export const getPropertyGroups = () => axios.get('/api/v1/property-groups')
export const createPropertyGroup = (data) => axios.post('/api/v1/property-groups', data)
export const getPropertyGroup = (id) => axios.get(`/api/v1/property-groups/${id}`)
export const replaceParticipants = (id, participants) =>
  axios.put(`/api/v1/property-groups/${id}/participants`, participants)
export const addParticipantRetroactive = (id, data) =>
  axios.post(`/api/v1/property-groups/${id}/add-participant-retroactive`, data)
export const validatePropertyGroup = (id) =>
  axios.get(`/api/v1/property-groups/${id}/validate`)
```

---

## Verification Checklist

1. Create a property group and add 4 participants with 25% each. Call `GET /api/v1/property-groups/{id}/validate` → `valid: true`.
2. Attempt `PUT participants` with three participants summing to 75% → expect HTTP 400 with message "must equal exactly 100.00%".
3. Attempt `PUT participants` with shares summing to 100.01% → expect HTTP 400 (exact comparison, not approximate).
4. Query `findActiveAtDate(groupId, date)` — returns only participants whose `validFrom <= date <= validTo` (or `validTo` is null).
5. `sumActiveSharePercentages` returns a `BigDecimal` with scale 2; verify `100.00` not `100` or `100.0`.
6. Add a retroactive participant (`validFrom` = past date): `SharedServiceInvoicingService.redistributeRetroactive()` is triggered.
7. After retroactive addition, `validate` endpoint still returns `valid: true` (redistribution maintains 100%).
8. `findGroupsByCustomerNumber("123456")` returns all groups the customer participates in.
9. Open `PropertyGroupsPage` in FE — `PercentageSummaryBar` shows green at 100%, red when off.
10. Attempt save via the `ParticipantsTable` edit with an invalid total — UI shows error toast from the HTTP 400 response.

---

## File Checklist

### Backend
- [ ] `sharedservice/PropertyGroup.java`
- [ ] `sharedservice/SharedServiceParticipant.java`
- [ ] `sharedservice/PropertyGroupRepository.java`
- [ ] `sharedservice/SharedServiceParticipantRepository.java`
- [ ] `sharedservice/PropertyGroupService.java`
- [ ] `sharedservice/SharedServiceValidationService.java`
- [ ] `sharedservice/PropertyGroupController.java`
- [ ] `sharedservice/SharedServicePercentageException.java`
- [ ] `sharedservice/dto/PropertyGroupRequest.java`
- [ ] `sharedservice/dto/PropertyGroupResponse.java`
- [ ] `sharedservice/dto/ParticipantRequest.java`
- [ ] `sharedservice/dto/RetroactiveParticipantRequest.java`
- [ ] `sharedservice/dto/ValidationResultResponse.java`

### Frontend
- [ ] `src/pages/sharedservices/PropertyGroupsPage.jsx`
- [ ] `src/pages/sharedservices/components/PropertyGroupsTable.jsx`
- [ ] `src/pages/sharedservices/components/PropertyGroupDetailPanel.jsx`
- [ ] `src/pages/sharedservices/components/ParticipantsTable.jsx`
- [ ] `src/pages/sharedservices/components/ParticipantForm.jsx`
- [ ] `src/pages/sharedservices/components/AddParticipantRetroactiveModal.jsx`
- [ ] `src/pages/sharedservices/components/PercentageSummaryBar.jsx`
- [ ] `src/api/propertyGroups.js`
