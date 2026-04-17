# PD-177: Billing Event Data for Reporting
**Jira:** https://ioteelab.atlassian.net/browse/PD-177
**Linked requirement:** PD-117 (Property Information Requirements)
**Status:** Partially implemented — 5 gaps to close

---

## Overview

PD-177 requires that every billing event records a complete set of financial and operational metadata so accounting users can reconcile expenses and export structured data to external reporting tools (BI, ERP, finance systems).

PD-117 defines the property data model that the system must support. The `Property` entity currently exists only as a minimal lookup stub used in the credit-transfer modal. It must be expanded to carry the full property register data as specified by PD-117.

---

## Gap 1 — Missing `wasteType` field on BillingEvent

**Requirement:** Each billing event must record the *category* of waste handled (e.g., mixed waste, biowaste, paper), not only the fee prices.

**Current state:** `BillingEvent.java` has `wasteFeePrice`, `transportFeePrice`, `ecoFeePrice` (price amounts only). There is no field that categorises the waste type.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEvent.java` | Add `wasteType` field (String or enum) |
| Create DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventCreateRequest.java` | Add `wasteType` field |
| Manual create DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventManualCreateRequest.java` | Add `wasteType` field |
| Update DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventUpdateRequest.java` | Add optional `wasteType` field |
| Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventResponse.java` | Include `wasteType` in output |
| Detail Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventDetailResponse.java` | Include `wasteType` in output |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | Map `wasteType` on create and update |
| DB migration | `invoicing/src/main/resources/db/migration/` (new migration file) | `ALTER TABLE billing_event ADD COLUMN waste_type VARCHAR(100)` |
| Seed data | `seed.sql` | Add `wasteType` values to existing event rows |

---

## Gap 2 — Missing `receivingSite` field on BillingEvent

**Requirement:** Each billing event must record the receiving site — the physical location where the waste was processed or deposited.

**Current state:** `CostCenter.java` has a `receptionSegment` field which is a cost-accounting segment code, not a human-readable receiving site name/identifier. There is no dedicated `receivingSite` field on `BillingEvent`.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEvent.java` | Add `receivingSite` field (String) |
| Create DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventCreateRequest.java` | Add `receivingSite` field |
| Manual create DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventManualCreateRequest.java` | Add `receivingSite` field |
| Update DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventUpdateRequest.java` | Add optional `receivingSite` field |
| Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventResponse.java` | Include `receivingSite` in output |
| Detail Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventDetailResponse.java` | Include `receivingSite` in output |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | Map `receivingSite` on create and update |
| DB migration | `invoicing/src/main/resources/db/migration/` (new migration file) | `ALTER TABLE billing_event ADD COLUMN receiving_site VARCHAR(255)` |
| Seed data | `seed.sql` | Add `receivingSite` values to existing event rows |

---

## Gap 3 — `responsibilityArea` and `serviceResponsibility` are not independent fields

**Requirement:** Responsibility area and service responsibility must be traceable per event independently.

**Current state:** Both are encoded as segments within the `CostCenter` composite code (`responsibilitySegment`, `receptionSegment`). They are not surfaced as separate, queryable fields on the billing event itself. This makes external reporting systems dependent on parsing the composite cost center code.

**Decision to make:** Either promote these to first-class fields on `BillingEvent`, or explicitly expose them in the response DTO by reading them from the resolved cost center. The simpler path is to include them in the response DTO via the resolved cost center.

### Files to change

| Layer | File | Change needed |
|---|---|---|
| Detail Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventDetailResponse.java` | Add `responsibilityArea` (from `costCenter.responsibilitySegment`) and `serviceResponsibility` (from `costCenter.receptionSegment`) as explicit fields in the response |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | Populate `responsibilityArea` and `serviceResponsibility` when building the detail response |
| List Response DTO | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventResponse.java` | Consider whether these also belong in the list view for filtering |

> **Note:** No DB schema change required if reading from the existing cost center relationship. If independently stored on billing event for denormalisation/performance, add DB columns similar to Gap 1 and Gap 2.

---

## Gap 4 — No reporting/export endpoint

**Requirement:** The system must support "seamless export of data to external finance, business intelligence, and reporting applications" with structured output compatible with company-specific financial reporting requirements.

**Current state:** There is no export endpoint. Data is accessible only via `GET /api/v1/billing-events` (paginated list) and `GET /api/v1/billing-events/{id}` (single detail). No endpoint exists for bulk export or structured reporting output.

### Files to change / create

| Layer | File | Change needed |
|---|---|---|
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/billingevent/BillingEventController.java` | Add `GET /api/v1/billing-events/export` endpoint that returns all reporting fields for a given date range / filter |
| Export DTO (new) | `invoicing/src/main/java/com/example/invoicing/entity/billingevent/BillingEventExportRow.java` | New DTO containing all 10 PD-177 fields in flat structure: `eventId`, `eventDate`, `accountingAccount`, `responsibilityArea`, `productGroup`, `wasteType`, `serviceResponsibility`, `locationId`, `municipalityId`, `projectCode`, `costCenter`, `receivingSite`, `calculatedAmountNet`, `calculatedAmountVat`, `calculatedAmountGross`, `customerNumber` |
| Service | `invoicing/src/main/java/com/example/invoicing/service/BillingEventService.java` | Add `exportEvents(filters)` method that returns a list of `BillingEventExportRow` |
| Repository | `invoicing/src/main/java/com/example/invoicing/repository/BillingEventRepository.java` | Add query method for filtered export (by dateFrom, dateTo, status, customerId, municipalityId) — without pagination |

**Export endpoint behaviour:**
- Query params: `dateFrom` (required), `dateTo` (required), `status` (optional), `customerNumber` (optional), `municipalityId` (optional)
- Response: JSON array of flat export rows (one per event)
- Should include all 10 PD-177 metadata fields plus calculated amounts
- Does **not** need to be a file download — structured JSON is sufficient for external tool integration

---

## Gap 5 — Property entity does not implement PD-117 requirements

**Requirement (PD-117):** The Property entity must carry property register data sourced from DVV's real estate register or the RYHTI system. Eight categories of fields are required.

**Current state:** `Property.java` has only 5 fields: `propertyId`, `streetAddress`, `city`, `postalCode`, `customerNumber`. This makes it a lookup stub, not a property register entity.

### Required fields to add to Property entity

**Basic / Classification fields:**
- `countryCode` — ISO country code (e.g., `FI`)
- `country` — Country name
- `municipalityCode` — Administrative municipality code
- `buildingClassification` — Enum: `DETACHED_HOUSE`, `LEISURE_BUILDING`, `HOUSING_ASSOCIATION`, `COMPANY`, `MUNICIPAL_SERVICE_OPERATION`
- `numberOfApartments` — Integer

**R1 — Building information:**
- `buildingStatus` — Enum or String: `COMPLETED`, `UNDER_CONSTRUCTION`, `UNFINISHED`
- `buildingIdentifier` — PRT-tunnus (Finnish building identifier)
- `buildingType` — Type code
- `constructionYear` — Integer
- `usageType` — Usage classification
- `numberOfFloors` — Integer
- `totalArea` — Decimal (m²)

**R3 — Location address:**
- `postalCode` — Already present
- `streetAddress` — Already present
- `city` — Already present
- `addressValidFrom` — Date (address validity start)
- `addressValidTo` — Date (address validity end, null = currently valid)

**R4 — Owner information** (new related entity `PropertyOwner`):
- `ownerId` — External identifier
- `ownerName` — Name
- `ownerContactInfo` — Contact details (email/phone/address)
- `ownershipType` — Type of ownership
- `ownershipPercentage` — Decimal
- `propertyId` — FK back to Property

**R9 — Oldest resident:**
- `oldestResidentYear` — Birth year of oldest resident (used for waste management discount eligibility; do not store full identity data)

### Files to change / create

| Layer | File | Change needed |
|---|---|---|
| Entity | `invoicing/src/main/java/com/example/invoicing/entity/property/Property.java` | Add all R1, R3 fields and basic/classification fields listed above |
| New entity | `invoicing/src/main/java/com/example/invoicing/entity/property/PropertyOwner.java` | New entity for R4 owner data |
| Enum (new) | `invoicing/src/main/java/com/example/invoicing/entity/property/BuildingClassification.java` | New enum: `DETACHED_HOUSE`, `LEISURE_BUILDING`, `HOUSING_ASSOCIATION`, `COMPANY`, `MUNICIPAL_SERVICE_OPERATION` |
| Enum (new) | `invoicing/src/main/java/com/example/invoicing/entity/property/BuildingStatus.java` | New enum: `COMPLETED`, `UNDER_CONSTRUCTION`, `UNFINISHED` |
| Search Result DTO | `invoicing/src/main/java/com/example/invoicing/entity/property/PropertySearchResult.java` | Add `municipalityCode`, `buildingClassification`, `numberOfApartments` to search result |
| New response DTO | `invoicing/src/main/java/com/example/invoicing/entity/property/PropertyDetailResponse.java` | Full property detail with all fields including owners and address validity |
| New repository | `invoicing/src/main/java/com/example/invoicing/repository/PropertyOwnerRepository.java` | JPA repository for `PropertyOwner` |
| Controller | `invoicing/src/main/java/com/example/invoicing/controller/property/PropertyController.java` | Add `GET /api/v1/properties/{id}` endpoint returning full `PropertyDetailResponse` |
| Service (new or expand) | `invoicing/src/main/java/com/example/invoicing/service/PropertyService.java` | Add `getPropertyDetail(propertyId)` and `upsertProperty(propertyData)` methods |
| DB migration | `invoicing/src/main/resources/db/migration/` (new migration file) | Add all new columns to `property` table; create `property_owner` table |
| Seed data | `seed.sql` | Add representative property rows with full field set |

---

## Checklist

- [ ] Gap 1: Add `wasteType` to `BillingEvent` entity, DTOs, service, and DB migration
- [ ] Gap 2: Add `receivingSite` to `BillingEvent` entity, DTOs, service, and DB migration
- [ ] Gap 3: Expose `responsibilityArea` and `serviceResponsibility` as explicit fields in `BillingEventDetailResponse`
- [ ] Gap 4: Create `GET /api/v1/billing-events/export` endpoint with `BillingEventExportRow` DTO
- [ ] Gap 5a: Expand `Property` entity with all PD-117 fields (R1, R3 direct fields + classification fields)
- [ ] Gap 5b: Create `PropertyOwner` entity and repository (R4)
- [ ] Gap 5c: Create `BuildingClassification` and `BuildingStatus` enums
- [ ] Gap 5d: Add `GET /api/v1/properties/{id}` detail endpoint
- [ ] Gap 5e: Update `PropertySearchResult` DTO to include `municipalityCode` and `buildingClassification`
- [ ] DB migrations for all schema changes
- [ ] Update `seed.sql` with realistic test data covering all new fields
- [ ] Unit tests for export service and property detail endpoint
