# Bridge Domain — Business Description

## What this domain is about

The Bridge domain manages WasteHero's integrations with external government registries, business registries, and commercial systems that provide authoritative data or delivery services. These integrations ensure that WasteHero maintains accurate property records, company information, and invoice data, and can reliably send official documents to citizens and businesses via their preferred digital channels. Without the Bridge, WasteHero would operate on stale or manually-entered data where regulators and municipalities require authoritative sources.

## External systems and what they provide

### Norwegian Property Registry (Kartverket Matrikkel)
Provides authoritative property data: real estate boundaries, property owner information, building details, and addresses. WasteHero synchronises this data nightly to maintain accurate property records for waste collection routing, billing, and to obtain the national property IDs (gnr/bnr) required for citizen login via BankID in the self-service portal.

### Norwegian Business Registry (Enhetsregisteret)
Delivers up-to-date information about Norwegian companies: business names, registration statuses, organisational structure, and contact details. WasteHero synchronises this data to verify business identities and obtain authoritative company information for invoicing and correspondence.

### Finnish Business Register (YTJ)
Provides real-time Finnish company data via a public OpenData API. WasteHero calls this live (with short-term caching) to resolve Finnish business identities and company status at the moment of request — no local copy is maintained.

### Finnish E-invoicing Directory (TIEKE)
Distributes a nightly CSV file containing Finnish organisations and their electronic invoicing addresses (OVT numbers). WasteHero downloads this file nightly and imports it for fast lookups when routing invoices to Finnish customers.

### Visma ERP
WasteHero's accounting system. WasteHero pushes customer, agreement, and invoice data to Visma on a scheduled basis so that Visma can generate formal accounting records and maintain a unified view of billing and revenue.

### SvarUt (KS FIKS digital mail)
Norway's national digital mail delivery infrastructure. WasteHero sends invoices, official letters, and correspondence through SvarUt. SvarUt automatically checks whether recipients have registered digital mailboxes and falls back to physical printing and postal delivery if they have not. Documents delivered via SvarUt are automatically archived into ACOS.

### ACOS WebSak (NOARK 5 Archive)
Norway's standardised document archiving system. WasteHero archives official documents (invoices, letters, official correspondence) to ACOS to comply with the Norwegian Archive Act (Arkivloven). Requires correct metadata (classification, keywords, sender, recipient, document type).

### F24 ServiceAlert (SMS)
SMS delivery service for sending text message notifications to customers (service alerts, payment reminders, etc.).

## Business flows

### Property and owner synchronisation (nightly)
Every night, WasteHero fetches all property and owner records from Kartverket Matrikkel, validates each row, and bulk-imports them into the local database atomically. Properties that are no longer in Matrikkel and have not been seen for a configurable staleness window are hard-deleted. This keeps WasteHero's property database current with the authoritative national cadastre.

### Business registry synchronisation (nightly)
Every night, WasteHero fetches all business records from Enhetsregisteret, validates them, and bulk-imports them. Records that have become stale (no longer present in the registry after the staleness window) are hard-deleted. This ensures WasteHero always has the current legal status and details of registered Norwegian businesses.

### Finnish business data lookup (on-demand + nightly)
When WasteHero needs to look up a Finnish company, it first tries the local cache (1-hour TTL). On a cache miss, it calls the live YTJ API. Separately, at 3 AM nightly, WasteHero downloads the TIEKE CSV and bulk-imports all organisations and e-invoicing addresses. When both sources are available, YTJ company status data and TIEKE invoicing addresses are merged into a combined response.

### Invoice push to accounting (scheduled)
On a defined schedule, WasteHero extracts customer, agreement, and invoice records and pushes them to Visma ERP. Visma then generates formal accounting entries. This decouples WasteHero's operational data from Visma's financial records.

### Digital document delivery (on-demand)
When WasteHero needs to send an official document to a recipient, it submits the document to SvarUt along with the recipient's Norwegian national ID or organisation number. SvarUt checks whether the recipient has a registered digital mailbox:
- If yes: SvarUt delivers the document digitally.
- If no: SvarUt prints and sends the document by post.
Either way, SvarUt automatically archives the delivered document to ACOS. WasteHero does not need to manage this manually.

### Document archiving (automatic + manual)
Official documents generated by WasteHero are converted to PDF and uploaded to ACOS via the NOARK 5 REST API with required metadata (classification, document type, sender, recipient). This can also be triggered manually from the WasteHero UI. Direct archiving to ACOS is required for official correspondence that does not flow through SvarUt.

### SMS notification delivery (on-demand)
When WasteHero needs to send an SMS, it submits the message and recipient phone number to F24. F24 delivers the SMS and returns delivery status.

## Business rules

- **Matrikkel is the source of truth for Norwegian properties**: Properties deleted from Matrikkel are hard-deleted from WasteHero after the staleness window — the external registry always takes precedence.
- **Enhetsregisteret is the source of truth for Norwegian businesses**: Similarly, businesses no longer registered are hard-deleted after the staleness window.
- **YTJ data is always live**: Because company status changes frequently, YTJ data is never stored locally. Each lookup either hits the short-lived cache or calls the live API.
- **TIEKE data is batch-refreshed nightly**: All e-invoicing routing information is refreshed at a fixed time. Organisations that drop out of the TIEKE CSV are hard-deleted after a staleness window.
- **SvarUt handles digital mailbox verification**: WasteHero does not check whether a recipient has a digital mailbox — SvarUt does this automatically on each send, with automatic fallback to print/post.
- **SvarUt auto-archives to ACOS**: Documents sent via SvarUt are automatically archived by SvarUt's ACOS integration; WasteHero does not need to archive them separately.
- **Direct ACOS archiving is required for non-SvarUt documents**: Official correspondence not flowing through SvarUt (e.g., internally generated records) must be explicitly archived to ACOS for compliance.
- **Visma is the system of record for accounting**: Once data is pushed to Visma, Visma owns the invoice and accounting records; WasteHero treats Visma as the downstream authoritative system for revenue.
- **Staleness window**: A configurable period (typically 7 days) after which data from an external source that has not been refreshed is eligible for hard deletion.

## Key business terminology

- **Matrikkel**: Norwegian cadastre (property register) maintained by Kartverket. Contains land parcels, buildings, addresses, and property ownership. Each property is identified by gnr/bnr (municipality number / property number).
- **Enhetsregisteret**: Norwegian business and organisation register maintained by Brønnøysund Register Centre. Records legal registration, status, and organisasjonsnummer (ORG number).
- **YTJ (Yritys- ja yhteisötietojärjestelmä)**: Finnish Business Register. Provides company names, statuses, and business identities. Accessed via a public OpenData API.
- **TIEKE**: Finnish e-invoicing network operator. Distributes a CSV file containing organisations and their OVT numbers. Used for routing invoices to Finnish customers via their preferred invoicing channel.
- **SvarUt**: KS FIKS's national digital mail delivery system in Norway. Operators submit documents; SvarUt routes them digitally to recipients with mailboxes, or falls back to print/post for others.
- **ACOS WebSak**: Norwegian NOARK 5 compliant document archive. Used to store official correspondence and records to satisfy the Norwegian Archive Act. Requires proper metadata for correct filing.
- **NOARK 5**: Norwegian standard for document and records management. Defines how documents must be classified, stored, and archived for legal compliance.
- **Visma ERP**: Commercial enterprise resource planning system used for accounting, invoicing, and financial management.
- **Gnr/Bnr**: Norwegian property identifiers. Gnr = municipality code, Bnr = property/land plot number. Together they uniquely identify a real estate parcel.
- **Organisasjonsnummer**: Norwegian organisation number. Unique identifier for registered companies and public bodies.
- **OVT Number**: Electronic invoicing address used in Finnish and Nordic e-invoicing networks. Routes invoices to the correct inbox within an e-invoicing operator's network.
- **Staleness Window**: A configurable time period after which data from an external source that has not been refreshed is considered stale and eligible for hard deletion.
- **Arkivloven**: Norwegian Archive Act. Mandates that public organisations archive official correspondence and documents in a standardised way (NOARK 5).

## What this domain does NOT handle

- **Generating invoices**: Invoice generation logic and billing rules belong to the Ledger domain. The Bridge only delivers the invoice and pushes it to Visma.
- **Email and SMS content authoring**: The content and templates for notifications belong to the Forge domain (communication services). Bridge handles delivery channel routing only.
- **Customer master data**: CRM and property entity management belong to the Nexus domain. Bridge provides the authoritative external data that Nexus stores.
- **User authentication**: Bridge provides property owner IDs needed for BankID login, but authentication itself belongs to the Forge domain.
- **Waste collection routing**: Route planning using property data belongs to the Compass domain.
- **Payment collection**: Financial transactions and payment collection belong to downstream billing systems.
