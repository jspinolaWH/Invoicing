# Ledger Domain — Business Description

## What this domain is about

The Ledger domain manages WasteHero's pricing, billing, and financial record-keeping across products and skip hire services. It defines what products cost, how prices vary by geography and service level, and tracks the financial components that make up billable service offerings.

The domain coordinates three key systems: the product catalog (what services are sold), the pricing engine (what they cost), and skip hire operations (where and how those services are delivered). Products have prices; prices exist in lists that can be scheduled for future activation; those prices apply at different rates depending on the customer's zone and chosen service level.

## Core business entities

**Product** — A sellable service, material, or container (e.g., "mixed waste collection," "240L waste bin," "premium delivery"). Products belong to a category that determines which fields are applicable. A product may have a waste fraction (e.g., "mixed municipal waste"), a pricing unit (e.g., "per kilogram"), and a base price. Product types: Service, Container, Additional Service, OneOff charge, Recurring fee.

**Category** — Organises products by type and controls which fields are available and required. Types: Service, Container, Additional, OneOff, Recurring. Controls field applicability (waste fraction, direction, container type, dimensions, etc.).

**Pricing Unit** — Defines how quantities are measured for billing (e.g., "kilogram," "cubic meter," "per collection"). Uses EU PEPPOL-compliant codes for international commerce.

**Waste Fraction** — Classifies waste by European Waste Catalogue (EWC) code and recovery/disposal method. Tracks hazard status, recyclability, and recovery percentages. Links products to waste reporting obligations (YLVA).

**Price List** — A named collection of price rows defining unit costs for products. Status lifecycle: Draft → Scheduled (if future-dated) → Active → Inactive. All pricing for a billing period runs through one active price list.

**Price Row** — Links a product to a price list with a unit price in a specific zone (or all zones if no zone is specified). Zone-specific rows take precedence over catch-all rows. Unique per (price list, product, zone) combination.

**Service Level** — Named service tiers (e.g., "Express," "Standard," "Economy") with optional surcharges and response timeframes. Can apply to specific waste fractions, products, or billing periods.

**Zone** — Geographic service area (Urban, Suburban, Rural) used for rate differentiation. Controls regional pricing variation. Global reference data seeded at system setup.

**Product Component / Bill of Materials (BOM)** — Breaks down a product's cost into constituent parts (labour, materials, disposal fees). Each component has a price and pricing unit. The `included_in_total` flag determines whether the component rolls up into the product's final price. BOM templates are reusable; product-level snapshots are taken at link time to prevent template changes from cascading retroactively.

**Additional Service** — An add-on linked to a base product (e.g., "hazardous waste handling," "priority pickup"). Defines trigger type (Automatic, Manual, Driver-Initiated) and approval requirements.

**Skip** — A physical waste container asset with a unique identifier, size (2–40 cubic yards), type (open, covered, enclosed, drop-door), and current status. Status lifecycle: Available → On Hire → In Transit → Maintenance/Damaged → Retired.

**Work Order** — A customer request for skip hire services (delivery, pickup, exchange, etc.). Has a scheduled date, optional time window, and status lifecycle: Draft → Scheduled → In Progress → Completed/Cancelled/Failed.

**Task** — A single driver action within a work order (e.g., "deliver skip SKIP-001 to property XYZ"). Assigned to a driver and vehicle, tracked with actual start/end times. Status: Pending → In Progress → Completed/Failed.

**Evidence** — Digital documentation (photos, signatures, voice notes) captured during work order execution. Linked to a skip, work order, task, or lifecycle event. Includes GPS coordinates, capture timestamp, validation status, and file metadata. Immutable after capture.

**Skip Lifecycle Event** — Records a state transition in a skip's lifecycle. 9 states: Empty → Delivered → Placed → Full → Picked → Emptied → Cleaned → Ready → Stored. Requires driver ID, GPS, evidence, and reason. Immutable audit trail.

## Business rules

### Product and category rules
- A product can only set fields that its category marks as enabled; required fields must be provided.
- Products cannot be created or updated if their category is inactive.
- A product cannot be soft-deleted if it is referenced by active price rows (returns a conflict error).

### Pricing rules
- Zone-specific price rows take precedence over catch-all rows (zone = null).
- No two active rows can cover the same (price list, product, zone) combination.
- Price list status lifecycle: Draft ↔ Active; Draft → Scheduled (if future-dated); Scheduled → Active automatically on effective date; Active ↔ Inactive.
- Effective dating is automatic: setting a future date auto-sets the list to Scheduled.

### Bill of materials rules
- BOM component values are snapshots: changes to the template do not affect existing product items.
- Only components marked `included_in_total = true` contribute to the product's computed price.

### Skip lifecycle rules
- State machine governs valid transitions; not all transitions are permitted.
- Evidence (photo, signature, or voice note) is mandatory for every state transition.
- GPS coordinates are required at every state transition.
- Every lifecycle event records the driver and timestamp; immutable after creation.

### Work order rules
- A work order must have at least one task.
- Service type determines task requirements: delivery orders require a skip and destination; pickup orders require a source; exchange orders require both.
- A task transitions to In Progress only after driver and actual start time are set.

### General rules
- All entities are tenant-scoped (belong to one company).
- Soft delete is universal: queries exclude soft-deleted records by default.
- Every mutation records who made the change and when.

## Business flows

### Setting up a product catalog
1. Create categories (Service, Container, Additional, OneOff, Recurring) with field rules.
2. Define reference data: pricing units, waste fractions (EWC codes), service levels, zones.
3. Create products, filling in category-dependent fields and assigning a pricing unit.
4. Optionally link bill-of-materials components and additional services to products.

### Creating and activating a price list
1. Create a price list in Draft status.
2. Add price rows for each (product, zone) combination with unit price and pricing unit.
3. Activate immediately (status → Active) or schedule for a future date (status → Scheduled, auto-transitions to Active on the date).
4. The previous active price list transitions to Inactive.

### Running a skip hire work order
1. Customer requests skip delivery, pickup, or exchange for a property.
2. Dispatcher creates a work order with order type, scheduled date, and time window.
3. Tasks are created for each skip or service needed and assigned to drivers and vehicles.
4. Work order transitions from Draft → Scheduled.
5. Driver starts the task; actual start time is recorded. Task transitions to In Progress.
6. Driver executes the service (delivers, collects, exchanges skip) and captures evidence (photo, signature, GPS) at each step.
7. Each skip state transition is recorded as a lifecycle event with evidence and GPS.
8. All tasks reach Completed; work order transitions to Completed.

### Generating a bill from the Ledger
1. Identify billable services delivered in the billing period.
2. Fetch the active price list for that period.
3. For each service line: find the product, identify the customer's zone, look up the matching price row (zone-specific first, then catch-all).
4. Apply service level surcharges if applicable.
5. Add bill-of-materials components marked `included_in_total`.
6. Sum all line amounts to produce the invoice total.

### Tracking a skip through its lifecycle
1. Skip enters the system with status Available.
2. Work order dispatches the skip; status changes to On Hire.
3. At each field step, driver records a state transition with evidence and GPS: Empty → Delivered → Placed → Full → Picked → Emptied → Cleaned → Ready → Stored.
4. Each event is immutable; the lifecycle event table is the audit trail.
5. Skip returns to Available/Stored at the yard.

## Key business terminology

- **EWC Code** (European Waste Catalogue): Standardised 6-digit classification for waste types (e.g., "20 03 01" = mixed municipal waste). Required for YLVA compliance reporting.
- **Price Row**: A single pricing entry: (product, zone, unit price). Zone can be null (applies to all zones).
- **Zone**: Geographic service area (Urban, Suburban, Rural) for regional pricing variation.
- **Service Level**: Named service tier (e.g., "Express," "Standard") with optional surcharge and response timeframe.
- **Work Order**: A customer request for skip hire service, grouping one or more tasks for a single date/time window.
- **Task**: A single driver action within a work order (e.g., "deliver skip to 123 Main St").
- **Lifecycle State**: One of 9 states in a skip's journey: Empty, Delivered, Placed, Full, Picked, Emptied, Cleaned, Ready, Stored.
- **Lifecycle Event**: An immutable record of a skip transitioning between states, including timestamp, driver, evidence, and GPS.
- **Evidence**: A digital artefact (photo, signature, voice note) captured during work order execution for audit or compliance.
- **Bill of Materials (BOM)**: Breakdown of a product's cost into constituent labour, materials, and disposal components.
- **Included in Total**: Flag on a BOM component; if true, its cost rolls up into the product's computed price.
- **Waste Fraction**: Classification of waste material with EWC code, recovery/disposal method, and hazard status.
- **Pricing Unit**: Unit of measurement for billing (e.g., "kilogram," "cubic meter," "per collection"). Uses EU PEPPOL codes.
- **Schema Config**: JSON structure on a category that defines field visibility, requirement, and validation rules for products in that category.
- **YLVA**: Finnish waste reporting system; requires EWC codes and waste fractions for statutory reporting.

## What this domain does NOT handle

- **Invoicing and payment processing**: Ledger defines prices and records billable events; invoice generation and payment collection are handled by downstream systems.
- **Customer management**: Customer master data (name, address, contact info) is managed by the Nexus domain.
- **Driver and fleet management**: Driver schedules, vehicle maintenance, and certifications are managed by the Compass domain.
- **Logistics and routing**: Work orders and tasks are tracked here; route optimisation and dispatch belong to Compass.
- **Waste disposal operations**: Ledger records that a skip was emptied; actual recycling and disposal facility operations are external.
- **Real-time GPS tracking**: Ledger captures GPS coordinates at state transitions; continuous real-time tracking belongs to the Compass domain.
- **Statutory reporting**: Ledger captures EWC codes for YLVA; report generation and submission are external.
