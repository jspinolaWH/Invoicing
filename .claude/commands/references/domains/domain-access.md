# Access Domain — Business Description

## What this domain is about

The Access domain is responsible for migrating customer data from legacy waste management systems into WasteHero's platform. It extracts customer, property, and relationship data from source systems (e.g., municipal ERPs like Vingo), validates data quality, transforms it into WasteHero's standard format, and loads it into the platform's live services. The domain handles the complete migration lifecycle: extract, validate, quality score, UAT review, and load — with full traceability and auditability at every step.

## What gets migrated

**Customers** — Individual customers (people) and companies (organisations), including:
- Identity (name, customer type, active status)
- Contact information (emails, phone numbers)
- Identifications (Y-tunnus/tax IDs, EAN codes)
- Billing settings (e-invoice preferences, payment methods, credit restrictions)
- Contracts (start/end dates)
- Hierarchical relationships (parent consortiums, sub-customers)

**Properties (Buildings/Real Estate)** — Physical building records, including:
- Property identifiers (property IDs, building numbers)
- Address and location data (street, postal code, city)
- Geographic coordinates (latitude/longitude)
- Building classification (property type codes, usage status)
- Physical characteristics (floor area, volume, floor count, apartment count)
- Ownership links (which customer owns the property)
- Completion dates and construction history

**Property-Customer Relationships** — Many-to-many links between properties and customers, including customer roles (owner, manager, bill payer, contact) and service arrangements.

**Consortiums (Parent Organisations)** — Hierarchical customer groupings for companies with multiple sub-entities. Sub-customers are consolidated into the main entity during migration.

**Property Managers** — Third-party management entities that manage properties on behalf of customers.

**Source systems**: Data originates from legacy ERPs (e.g., Vingo for Finnish municipalities), CSV exports, SQL database dumps, and other municipal or commercial waste management systems.

## Business flows

### 1. Extract and stage
1. Customer provides a raw data export (CSV files or database dump).
2. The pipeline reads files with encoding and field delimiter detection.
3. Raw data is loaded into a staging area as-is, preserving the original structure.
4. A migration batch is created with a unique identifier (e.g., `pjh-poc-001`).
5. Status: **Extracted** — ready for transformation.

### 2. Transform to canonical format
1. A source adapter reads from the staging area and maps source fields to WasteHero's standard (canonical) format.
2. Business logic transformations are applied:
   - Address parsing: split combined fields (e.g., "PUSKINTIE 39") into street name and number.
   - Coordinate conversion: Finnish national grid coordinates → WGS84 (GPS standard).
   - Customer type detection: classify individual vs. company from name/ID heuristics.
   - Phone normalisation: fix formatting errors, validate Finnish format.
   - Property type mapping: convert legacy classification codes to current standard.
   - E-invoice classification: determine invoicing method, validate addresses and operator codes.
   - Sub-customer consolidation: merge entities (XX-XXXXXXX-01, -02, -03) into their parent (XX-XXXXXXX-00), preserving all property links.
3. Fields that cannot be mapped to the canonical model are stored separately as metadata.
4. Status: **Transformed** — ready for validation.

### 3. Validate and quality score
1. Validator applies 60+ business rules per entity type.
2. Each entity receives:
   - A **quality score** (0.0–1.0) based on data completeness.
   - A **validation status**: valid, warnings only, or has errors.
   - An **error list**: blocking issues that prevent loading.
   - A **warning list**: data quality concerns that don't block loading.
3. Status: **Validated** — ready for review.

### 4. UAT review
1. Stakeholders review validated data via an interactive UI with search, filters, charts, and maps.
2. Data can be exported to Excel workbooks (summary, valid records, warnings, errors, raw data).
3. Stakeholders approve records or request corrections.
4. Status: **Approved** — ready for loading.

### 5. Load to platform
1. Approved canonical records are sent to WasteHero's live services in bulk batches (100–500 records per request).
2. Each record is inserted with its source ID for idempotency (re-running does not create duplicates).
3. Success and failure are logged per record.
4. Status: **Loaded** (or **Load Failed** for records that errored).

### 6. Post-load reconciliation
1. Counts are compared across all stages (source → staged → transformed → validated → loaded) to detect data loss.
2. Referential integrity is verified (e.g., every property has a valid owner customer).
3. An audit report is generated for handover.

## Business rules

### Customer rules
- Every customer must have a name (required; blocks loading if missing).
- Every customer must have a customer type (Individual or Company).
- At least one address (billing, emptying, or marketing) is required.
- Companies must have a Y-tunnus (Finnish tax ID) if available; warning if missing.
- At least one contact method (email or phone) is strongly recommended.
- If e-invoicing is enabled, both an e-invoice address and an operator code are required.
- Sub-customers are consolidated into their parent; all property links from sub-entities are collected under the main customer.

### Property rules
- A complete address is strongly recommended.
- Geographic coordinates are highly valuable and improve quality score significantly.
- Property type code (2018 classification) is required.
- Every property must link to an owner customer.

### Address rules
- Country defaults to Finland (FI) if not specified.
- Postal code must be 5 digits.
- City name is required if a street is present.
- Coordinates must fall within Finland (59–71°N, 19–32°E).

### Relationship rules
- Every property must have an owner customer.
- Consolidation of sub-customers preserves all property links.
- All property-customer links must maintain referential integrity.

### Exclusion rules
- For Finnish municipalities: internal billing groups (e.g., "KI" community services, "A" own services) are excluded from migration — these are internal cost centres, not external customers.

### Idempotency
- Unique constraint on (source ID, migration batch ID) in the staging area prevents duplicate records.
- Loading uses the source ID as the idempotency key: re-running a load does not create duplicate records in the platform.

## Data quality and validation

**Quality scoring (0.0–1.0)**:

Customer score contributions:
- Name present: +0.20
- Primary email present: +0.15
- Phone present: +0.15
- Complete address: +0.15
- Y-tunnus (tax ID): +0.10
- Geographic coordinates: +0.10
- Billing info: +0.05
- Emptying address: +0.05
- No blocking errors: +0.05

Property score contributions:
- Complete address: +0.25
- Geographic coordinates: +0.20
- Property type: +0.15
- Owner ID: +0.15
- Floor area: +0.10
- Completion date: +0.05
- Apartment count: +0.05
- No blocking errors: +0.05

**Thresholds**:
- 0.8–1.0: Excellent quality
- 0.5–0.79: Acceptable (minimum threshold for loading)
- Below 0.5: Poor — requires review or exclusion

**What fails a migration**:
- Hard blocks: missing name, missing customer type, missing required address, referential integrity failures.
- Soft issues (warnings): missing contact info, low quality score, invalid formats.

## Key business terminology

- **Canonical Format / Model**: The standard data structure for customer and property records in WasteHero (e.g., CustomerInterchange, PropertyInterchange). Defines what "correct" migrated data looks like.
- **Source Adapter**: A pluggable module that reads data from a specific source system (Vingo, WH1, CSV, SQL) and maps it to the canonical format.
- **Target Loader**: A pluggable module that loads canonical records into a specific target (WasteHero 2.0 microservices, WasteHero 1.0).
- **Migration Batch**: A single project run grouping all entities extracted from one source in one session (e.g., "pjh-poc-001").
- **Staging Area**: Intermediate storage for raw source data, preserved for traceability and audit. Raw data is never modified; transformations produce a separate canonical copy.
- **Sub-customer Consolidation**: Merging a main customer (XX-XXXXXXX-00) with its sub-entities (XX-XXXXXXX-01, -02, -03). All property links from sub-entities are collected under the main customer record.
- **Quality Score**: A numeric rating (0.0–1.0) based on data completeness, used to prioritise records for review.
- **Validation Status**: The loadability state of each entity: valid, warnings only, or has errors.
- **Source ID / Migrated From ID**: The original identifier from the source system, preserved for audit trail and idempotency.
- **Metadata**: Custom fields from the source that cannot be mapped to the canonical model. Stored separately and can be linked to the entity after migration.
- **Y-tunnus**: Finnish business ID (tax identifier). Required for companies in Finland.
- **OVT Number**: Finnish electronic invoicing address. Required for customers who receive invoices electronically.
- **Reconciliation**: Post-load verification comparing record counts across all stages to detect data loss or errors.
- **UAT (User Acceptance Testing)**: Stakeholder review phase where migrated data is inspected and approved before loading into production.

## What this domain does NOT handle

- **Live transaction processing**: The Access domain handles one-time bulk migration, not real-time ongoing data sync.
- **Ongoing integrations with external systems**: Continuous data feeds from government registries or ERPs belong to the Bridge domain.
- **Customer billing or invoicing**: Billing rules and invoice generation belong to the Ledger domain.
- **Service fulfilment**: Pickups, routes, and collection operations belong to the Compass domain.
- **Account or user management**: Authentication and role management belong to the Forge domain.
- **Real-time sync after initial load**: The Access domain performs one-time bulk migration. Keeping data current after go-live belongs to other domains.
- **Apartment-level data**: Currently migrated at building/customer level; apartment-level detail is deferred.
