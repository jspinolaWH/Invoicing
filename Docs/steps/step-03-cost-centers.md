# Step 03 — Cost Centers

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: CostCenter
  - "Cost centers composite (product + reception point + service responsibility)"
- `Docs/structured_breakdown/03-business-logic.md` → Section: CostCenterCompositionService (Area 6)
  - "Assemble composite code from product + reception + responsibility segments"
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data
  - "CRUD endpoints for each: accounting-accounts, cost-centers, vat-rates, allocation-rules"
- `Docs/structured_breakdown/07-build-order.md` → Step 1: Master Data Foundation

---

## Goal
Implement the `CostCenter` reference data entity. A cost center is a composite identifier built from three segments: product segment, reception point segment, and service responsibility segment. It is assembled by `CostCenterCompositionService` (built in Step 9 — Accounting Allocation). For now, the composite code can be stored and managed directly as a concatenated string or derived on read.

**Prerequisite:** Step 01 must be complete (`BaseAuditEntity` must exist).

---

## Backend

### 3.1 CostCenter Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/costcenter/CostCenter.java`

> **Requirement source:** `01-domain-model.md` — CostCenter:
> - "Cost centers composite (product + reception point + service responsibility)"
> - Used in BillingEvent and InvoiceLineItem for ledger assignment

```java
@Entity
@Table(name = "cost_centers")
public class CostCenter extends BaseAuditEntity {

    @Column(nullable = false)
    private String productSegment;          // e.g. "WASTE"

    @Column(nullable = false)
    private String receptionSegment;        // e.g. "HELSINKI-01"

    @Column(nullable = false)
    private String responsibilitySegment;   // e.g. "MUNICIPAL"

    @Column(nullable = false, unique = true)
    private String compositeCode;           // e.g. "WASTE-HELSINKI-01-MUNICIPAL"
                                           // Stored for fast lookup; also derivable from segments

    @Column(nullable = true)
    private String description;             // Optional human-readable label
}
```

**Note on compositeCode:** Per `03-business-logic.md`, the `CostCenterCompositionService` (Step 9) will eventually assemble this code from segments dynamically per event. For now, we store it directly as managed reference data. The service layer will auto-derive `compositeCode` from the three segments on create/update.

---

### 3.2 CostCenterRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/costcenter/CostCenterRepository.java`

```java
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    Optional<CostCenter> findByCompositeCode(String compositeCode);

    List<CostCenter> findByProductSegment(String productSegment);

    List<CostCenter> findByResponsibilitySegment(String responsibilitySegment);
}
```

---

### 3.3 CostCenterService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/costcenter/CostCenterService.java`

Methods:
- `findAll()` → `List<CostCenter>`
- `findById(Long id)` → `CostCenter`
- `findByCompositeCode(String code)` → `CostCenter`
- `create(CostCenterRequest)` → `CostCenter`
  - Auto-derives `compositeCode = productSegment + "-" + receptionSegment + "-" + responsibilitySegment`
- `update(Long id, CostCenterRequest)` → `CostCenter` (re-derives compositeCode)
- `delete(Long id)` → void

---

### 3.4 CostCenterController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/costcenter/CostCenterController.java`

Base path: `/api/v1/cost-centers`

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/api/v1/cost-centers` | none | All cost centers |
| GET | `/api/v1/cost-centers` | `productSegment=` | Filter by product segment |
| GET | `/api/v1/cost-centers` | `responsibilitySegment=` | Filter by responsibility |
| GET | `/api/v1/cost-centers/{id}` | — | Single cost center |
| POST | `/api/v1/cost-centers` | — | Create cost center |
| PUT | `/api/v1/cost-centers/{id}` | — | Update cost center |
| DELETE | `/api/v1/cost-centers/{id}` | — | Delete cost center |

**Request body (POST/PUT):**
```json
{
  "productSegment": "WASTE",
  "receptionSegment": "HELSINKI-01",
  "responsibilitySegment": "MUNICIPAL",
  "description": "Helsinki municipal waste collection"
}
```

**Response body:**
```json
{
  "id": 1,
  "productSegment": "WASTE",
  "receptionSegment": "HELSINKI-01",
  "responsibilitySegment": "MUNICIPAL",
  "compositeCode": "WASTE-HELSINKI-01-MUNICIPAL",
  "description": "Helsinki municipal waste collection",
  "createdBy": "system",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

---

## Frontend

### 3.5 Cost Centers Page

**File:** `invoicing-fe/src/pages/masterdata/CostCentersPage.jsx`

Components:
- **CostCentersTable** — columns: Composite Code, Product Segment, Reception Segment, Responsibility Segment, Description, Actions (Edit / Delete)
- **CostCenterModal** — form for Add/Edit: productSegment (text), receptionSegment (text), responsibilitySegment (text), description (text, optional)
- Show derived `compositeCode` as a read-only preview in the modal while the user types the segments

Add nav item under Master Data in `Layout.jsx`.

API calls in `src/api/costCenters.js`:
```js
export const getCostCenters = (params) => axios.get('/api/v1/cost-centers', { params })
export const createCostCenter = (data) => axios.post('/api/v1/cost-centers', data)
export const updateCostCenter = (id, data) => axios.put(`/api/v1/cost-centers/${id}`, data)
export const deleteCostCenter = (id) => axios.delete(`/api/v1/cost-centers/${id}`)
```

---

## Verification Checklist

1. `mvn spring-boot:run` — Hibernate creates `cost_centers` table
2. `POST /api/v1/cost-centers` with all 3 segments — response contains derived `compositeCode`
3. `GET /api/v1/cost-centers?productSegment=WASTE` — filters correctly
4. `PUT /api/v1/cost-centers/{id}` — updates segments and re-derives compositeCode
5. Verify `compositeCode` unique constraint blocks duplicates (POST same segments twice → 400/409)
6. Open FE Cost Centers page — table loads, shows composite code column
7. Add / Edit / Delete via modal — all work, compositeCode preview updates in real-time

---

## File Checklist

### Backend
- [ ] `masterdata/costcenter/CostCenter.java`
- [ ] `masterdata/costcenter/CostCenterRepository.java`
- [ ] `masterdata/costcenter/CostCenterService.java`
- [ ] `masterdata/costcenter/CostCenterController.java`
- [ ] `masterdata/costcenter/dto/CostCenterRequest.java`
- [ ] `masterdata/costcenter/dto/CostCenterResponse.java`

### Frontend
- [ ] `src/api/costCenters.js`
- [ ] `src/pages/masterdata/CostCentersPage.jsx`
- [ ] `src/App.jsx` — add route for `/master-data/cost-centers`
- [ ] `src/components/Layout.jsx` — add nav item
