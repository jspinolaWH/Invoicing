# Nexus Domain — Business Description

## What this domain is about

The Nexus domain manages the core operational infrastructure of waste management: customers, properties, and the physical containers that collect waste. It orchestrates the relationship between businesses and individuals who generate waste (customers), the locations where waste is collected (properties), and the collection equipment itself (containers). This domain handles the master data, hierarchical relationships, and temporal tracking needed to operate a modern waste management service across multiple tenants.

## Core business entities

**Customer** — A business or individual who generates waste and is billed for waste management services. Features: customer type (Individual/Company), optional segments, hierarchical parent/child relationships, invoicing methods, billing rhythm, active status, and Nordic regulatory compliance (non-disclosure restrictions per Finnish Act 661/2009).

**Property** — A real-world location where waste collection occurs (building, address, or collection point). Properties are where containers are placed and emptied. Features: property type classification, hierarchical relationships, multiple addresses with one primary (supports Nordic language translations), active status, and flexible metadata for property-specific attributes.

**Container** — A physical piece of collection equipment (wheelie bin, dumpster, compactor) that holds waste at a property. Features: container type definition (capacity, compartments), lifecycle status (Active, Inactive, Damaged, Removed), optional waste fraction assignment, GPS coordinates, and active indicator.

**Property-Container Association** — Temporal binding tracking container deployment history. Start date and end date (null = currently active) with constraints preventing overlapping assignments of the same container to the same property. Used for billing and audit purposes.

**Customer Contact** — Phone, email, or mobile contact methods for customers. Features: verification status, primary selection per contact type, billing designation flag.

**Service Responsibility** — Categorises services into responsibility types (Municipal, TSV, Market-based, Residential are system-managed). Used for Nordic regulatory compliance and operational tracking.

**Property Group** — A logical grouping of properties for operational management and consolidated billing. Has a type (e.g., SINGLE, SPLIT) and an optional billing address.

**Notes** — Freeform notes with file attachments (photos, documents) attached to any entity. Have three visibility levels: Private (owner only), Shared (specific roles), Public (all). Organised into dynamic, user-defined collections.

## Business rules

- **Customer uniqueness**: Within a company; migrated_from_id provides global uniqueness across the system for duplicate prevention during migration.
- **Property uniqueness**: Global entities with duplicate detection via migrated_from_id (global) and address combination (company-scoped). Force flags allow overriding in edge cases.
- **Container assignment**: One active assignment per property at a time; temporal with inclusive end dates for billing. Overlapping assignments for the same container are blocked.
- **Primary address**: Only one per property; setting a new primary atomically updates the old one.
- **Contact requirements**: Customers must have at least one contact method.
- **Service responsibility immutability**: System types (Municipal, TSV, Market-based, Residential) cannot be modified; custom types are freely managed.
- **Multi-tenancy isolation**: All data isolated by company. Users only see their company's data.
- **Soft delete preservation**: Deleted records are soft-deleted with audit trail preserved; deleted properties do not block new records from being created with the same address.
- **Hierarchical non-circularity**: Parent-child relationships cannot be self-referential.

## Business flows

### Onboarding a new customer
1. Create customer with name, type (Individual/Company), and billing rhythm
2. Add at least one contact method (phone or email), mark one as primary per type
3. Assign customer type and optional segment/category
4. Optionally link to a parent customer (for multi-site organisations)
5. Mark customer as active

### Registering a property and assigning containers
1. Create property with property type
2. Add one or more addresses; designate one as primary
3. Add optional metadata (property-specific attributes)
4. Create or select a container with the appropriate type and waste fraction
5. Assign the container to the property with a start date (creating a Property-Container Association)
6. Mark property as active

### Changing a container at a property
1. End the current Property-Container Association by setting its end date
2. Select or create the new container
3. Create a new Property-Container Association with the new container and start date

### Tracking container history for billing
1. Query Property-Container Associations for a given period
2. Calculate billable days per container using start and end dates
3. Hand off to Ledger domain for pricing and invoice generation

### Importing from a legacy system
1. Load records with migrated_from_id from the source system
2. Check global uniqueness on migrated_from_id to prevent duplicates
3. Create records preserving the source system's identifier for traceability

### Organising properties into a group
1. Create a Property Group with a type and optional billing address
2. Add properties to the group
3. Use the group for consolidated billing or operational routing

### Recording a fill-level reading from a sensor
1. IoT sensor transmits fill-level data via MQTT
2. Data is associated with the container at the correct property
3. Fill-level reading is stored with timestamp for routing and analytics

## Key business terminology

- **Property Group**: Logical grouping of properties for operational management and consolidated billing.
- **Property Group Type**: Classification (SINGLE, SPLIT, etc.) defining group operational characteristics.
- **Service Responsibility**: Regulatory classification (Municipal, TSV, Market-Based, Residential) per Nordic frameworks.
- **Customer Type**: Classification (Individual/Company) for different business rules and pricing.
- **Container Type**: Classification by physical attributes (capacity, compartments, lifting mechanism).
- **Container Status**: Lifecycle state (Active, Inactive, Damaged, Removed).
- **Waste Fraction**: Classification of the waste material a container is designated for (general waste, recyclables, hazardous, etc.).
- **Primary Address**: Designated main address for billing correspondence and service dispatch.
- **Migrated From ID**: Legacy system identifier carried over during migration, providing global uniqueness for duplicate prevention.
- **Property-Container Association**: Time-based assignment linking a container to a property, with start and end dates for billing.
- **Billing Rhythm**: The invoicing frequency for a customer (monthly, quarterly, annually, etc.).
- **Non-disclosure restriction (turvakielto)**: Finnish regulatory requirement that prevents a customer's personal data from being shared; enforced at the customer record level.

## What this domain does NOT handle

- **Waste collection logistics**: Route planning, truck scheduling, and collection execution belong to the Compass domain.
- **Pricing and billing generation**: Rate calculation and invoice generation belong to the Ledger domain.
- **Geospatial operations**: Geocoding and address-to-coordinate resolution is handled by a shared platform service.
- **User authentication and authorisation**: User identity and permissions are managed by the Forge domain (IAM).
- **Citizen self-service**: The citizen-facing portal for residents to manage their waste services belongs to the Beacon domain.
- **External registry sync**: Syncing property ownership data from national registers (e.g., Kartverket Matrikkel) belongs to the Bridge domain.
- **Financial settlements**: Payment processing and receivables belong to the Ledger domain.
