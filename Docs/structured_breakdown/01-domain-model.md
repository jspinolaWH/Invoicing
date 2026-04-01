# 01 — Domain Model
## What entities exist and why the requirements demand them

Every entity below is required by one or more PD tasks. The requirement quote is the justification for including it.

---

## BillingEvent

The foundation of the entire invoicing module. Everything starts here.

> **PD-299:** *"An event must be created in the application such that invoicing data can be generated from the same event."*

This is the record of something that happened and needs to be billed — a container emptying, a weighbridge weighing, a septic tank emptying, or anything else. The event schema must carry all of the following fields, because each one is explicitly required:

- **Date** — when it happened
- **Product / service** — what was done
- **Waste price and transport price** — two separate price components, always
- **Quantity** (pcs, m³) and **weight** (t/kg) — different products use different units
- **VAT price at 0% and at 24%** — both rates may apply on the same invoice
- **Registration number** — vehicle identifier
- **Accounts and cost centres** — mandatory for accounting allocation
- **Free-text comment** — for notes
- **Customer number** — the payer
- **Contractor** — who performed the service (may differ from the invoiced customer)
- **Load unloading location**
- **Event identifier**
- **Service responsibility** — whether it falls under municipal or market-based rules
- **Origin and direction**
- **Municipality**
- **Shared collection point group percentage** — for shared service splits (PD-280)
- **Event comments**

> **PD-299 (PJH addition):** *"Eco-fee is also a price formation component that must be visible on the event."*

So **eco-fee** is an additional required field on every event, on top of the base schema above.

Beyond the schema, the event needs:

> **PD-297:** *"In Progress: events that have been recorded, but for which an invoice has not yet been created. Sent: events for which an invoice and invoice data have been generated, but the invoice data has not yet been sent to the invoicing system. Error: events that contain incorrect data or where errors occurred during the transfer. Completed: an invoice has been created from the event and the invoice data has been successfully sent to and confirmed by the invoicing system."*

So the event carries a **status** that moves through that four-state lifecycle. And critically:

> **PD-297 (PJH):** *"The 'Sent' status should mean the billing file has actually been sent, at which point no further changes to the invoice content should be allowed."*

This means status is not just informational — it is a gate. Once an event reaches SENT, it is immutable.

The event also needs:

> **PD-228:** *"Events such as waste collection, waste weighing at a sorting station, or additional services performed on site are recorded with the assigned driver and vehicle details."*

So **driver ID** and **vehicle ID** are required fields on the event.

> **PD-318:** *"Events marked as excluded remain in the system but are flagged as non-billable."*

So the event needs an **excluded flag** and an **exclusion reason**. Excluded events are never deleted.

> **PD-285:** *"The system automatically determines the correct classification based on predefined criteria such as customer type, product or service classification, or regional regulations."*

So the event carries a **legal classification** field: PUBLIC_LAW or PRIVATE_LAW. This is set at creation time.

> **PD-287:** *"Projects (or work sites) can be created in three ways: as separate properties linked to the customer, as separate contracts for a property or a customer, as a customer hierarchy."*

So the event carries an optional **project ID** — which work site it belongs to.

> **PD-280:** *"Each subscriber in a shared service is assigned an individual invoice line, with the amount determined according to their percentage share."*

So the event carries a **shared service group ID** and a **percentage** when it belongs to a shared container arrangement.

> **PD-228:** *"It is necessary to separately define which events/additional tasks can proceed directly to the invoice and which require review by the office worker."*

So the event has a **requiresOfficeReview** flag and fields to track who reviewed it and when.

---

## Invoice

The document that goes to the customer. Generated from one or more BillingEvents.

> **PD-309:** *"Waste management companies can define, during the implementation phase, the required format for entering numbers, and the system will apply that model going forward when validating numbering."*

> **PD-309 (PJH):** *"The first digit indicates whether it is private law or public law, and the second digit represents the year. The same invoice number must not be reused."*

The Invoice needs an **invoice number** (assigned from a series) and a reference back to the **InvoiceNumberSeries** entity it came from. The number must be unique forever — numbers released by cancelled invoices go back into the pool but are tracked.

> **PD-307:** *"The WasteHero system will not create or manage invoice templates but rather send information about the desired invoice template to the invoicing system as a part of the FINVOICE data."*

So the Invoice carries a **template code** — a string that maps to a template in the external invoicing system. Not the template itself.

> **PD-308:** *"The system's default primary language is Finnish. A language code can be defined for a customer account (Finnish, Swedish, or English). Products have separate names defined for each language."*

The Invoice carries the **language code** resolved from the customer profile at generation time. The FINVOICE output uses this to select the right product names and address format.

> **PD-301:** *"The net sum shows the invoiceable sum without VAT, the applicable VAT and the total including VAT separately. The gross sum shows only the invoiceable sum including VAT with the VAT percentage shown."*

> **PD-301 (PJH):** *"The system must show the VAT breakdown line by line on the invoice. A VAT summary breakdown must also be visible."*

The Invoice has a **gross/net flag** — set from the customer profile. The PJH note is important: regardless of the flag, VAT must always be broken down per line and summarised at the bottom.

> **PD-300:** *"The system automatically applies reverse charge VAT when a product or service marked as subject to reverse VAT is added to an invoice."*

The Invoice has a **reverseChargeVat flag** and, when true, must carry the buyer's VAT number and the legal reference text required by Finnish tax law.

> **PD-302:** *"Users can add custom text to invoices either on a per-customer basis or across multiple invoices at once."*

The Invoice has a **customText** field — visible on the printed invoice and included in FINVOICE.

> **PD-269:** *"Users can define custom text for the credit note and also add an internal comment explaining the reason for the credit."*

The Invoice also has an **internalComment** field — not visible to the customer, stored for internal use only. Critical for credit notes.

> **PD-269:** *"The credit note is assigned the number of the invoice being credited."*

A credit note is itself an Invoice with a reference to the **original invoice ID** it credits. Not a separate entity — the same structure, with a type flag of CREDIT_NOTE.

> **PD-294:** *"An invoicing surcharge must be added to an invoice depending on the invoicing method, e.g. a paper invoice surcharge, email invoice surcharge, direct payment surcharge."*

The Invoice carries a **surcharge line item** (a special product line) when the surcharge applies. It can be removed manually per invoice even when the global toggle is on.

> **PD-273:** *"Default invoice dataset statuses: Draft, ready, and sent."*

The Invoice has its own **status** separate from the BillingEvent status: DRAFT → READY → SENT → COMPLETED / ERROR / CANCELLED.

> **PD-305:** *"Users can attach up to ten PDF documents to an invoice."*

The Invoice has a collection of **InvoiceAttachment** records — each with the attachment identifier (SHA1-based), filename, MIME type, security class (SEI code), and the base64 content.

---

## InvoiceLineItem

Each line on the invoice — derived from one or more BillingEvents depending on bundling rules.

> **PD-290:** *"All storage location emptying events shown as one consolidated line item, base service fees shown as a separate line item, pallet deliveries listed as individual events."*

A line item is either a 1-to-1 reflection of one BillingEvent, or a bundled aggregation of many BillingEvents into one line. The underlying events are never deleted — only their presentation is condensed.

> **PD-289:** *"Mandatory reporting information — such as public-law/private-law classification, accounting ledger codes, and VAT breakdowns — is automatically assigned to invoice lines based on customer and event data."*

> **PD-289 (PJH):** *"The examples are missing accounting cost object information, which must also be taken into account."*

Every line item carries: **legal classification**, **ledger account code**, **VAT breakdown**, and **cost object**. These are resolved automatically from the event's product and customer configuration — not entered manually.

> **PD-295:** *"The same product must be able to be routed to a different account and cost center based on e.g. price list or region."*

So a single product can appear on two different line items pointing to two different ledger accounts, depending on where the service was performed. This is the split that makes FINVOICE accounting data differ from what the customer sees.

---

## InvoiceRun

The batch operation that generates invoices. Not a single invoice — the container for a whole invoicing session.

> **PD-272:** *"Users can run a test run that generates invoice data without finalizing or sending invoices. The simulation generates data that reflects the invoice structure but is marked as test data, preventing accidental processing."*

The InvoiceRun has a **simulation flag**. When true, no invoice numbers are assigned, no event statuses change, nothing is transmitted.

> **PD-272:** *"A summary report is produced: total number of generated invoices, breakdown of billed events by category, VAT calculations and cost center allocations, missing or incorrect data identified during the simulation."*

The InvoiceRun carries a **ValidationReport** embedded in it — populated during simulation and then re-run on the real run.

> **PD-293:** *"The following can be used as selection criteria for invoicing events per invoicing run: municipality, minimum invoice amount, invoicing period, customer type, events of a certain type of service, reception location, service responsibility."*

The InvoiceRun carries a **filter configuration** — all of these as typed fields, persisted so the run is reproducible and auditable.

> **PD-270:** *"The WasteHero system ensures that customer data can be processed and updated even during billing runs, allowing users to carry out their daily tasks without disruption. At the same time, the system implements safeguards to prevent changes to critical invoice-related data."*

The InvoiceRun knows which customers are in scope — so it can tell the rest of the system which billing addresses and billing groups are currently locked.

> **PD-273 (PJH):** *"PJH wants the billing data not to be sent automatically in the first phase. PJH wants the billing data to be sent by the billing user."*

The InvoiceRun has a **scheduledSendAt** field (nullable) — when null, send is manual; when set, a scheduler triggers the transmission.

---

## Customer / BillingProfile

> **PD-298:** *"The mandatory data required by the system includes customer identifiers, the invoice delivery method, and the billing address. A customer identifier refers to a CustomerID, which is a 6–9 digit numeric sequence."*

The Customer has a **BillingProfile** — a sub-structure (not a separate table entity, but an embedded value object) holding: CustomerID (6–9 digits), delivery method (E_INVOICE / EMAIL / PAPER / DIRECT_PAYMENT), billing address, business ID.

> **PD-298:** *"The application records how the invoice was sent (e.g., as an e-invoice, by email, or on paper)."*

The BillingProfile records the **delivery channel** — and this is what drives automatic surcharge assignment (PD-294) and FINVOICE routing.

> **PD-308:** *"A language code can be defined for a customer account (Finnish, Swedish, or English). Addresses can be stored in two languages."*

The BillingProfile has a **languageCode** and stores the billing address in **two language variants**.

> **PD-301:** *"Information on whether a customer is invoiced using a gross or net invoice is stored in the customer profile in WasteHero."*

The BillingProfile has a **invoicingMode** field: GROSS or NET.

> **PD-282:** *"Users can override or modify e-invoicing addresses and at the same time prevent future integration updates from automatically replacing them."*

The BillingProfile has an **EInvoiceAddress** sub-object with a **manuallyLocked** boolean. When locked, the integration (PD-107) will not overwrite it.

> **PD-281:** *"The ERP system acts as the master system for invoicing data, and all changes to the billing address made in the ERP system are synchronised with the invoicing system."*

Every change to BillingProfile fields triggers a sync to the external invoicing system. WasteHero is the master — the external system follows.

---

## InvoiceNumberSeries

> **PD-309:** *"During invoice creation, invoicing users can either define the invoice number series manually, or select the series to be used from pre-defined invoice number series (e.g., sludge invoice, credit invoice, public or private waste collection, annual invoice)."*

A dedicated entity that holds: series name, prefix, format pattern, the current sequence counter, and validity dates. When an invoice is assigned a number, the counter increments atomically. When an invoice is cancelled, the number is released back to a pool tracked on this entity.

> **PD-309 (PJH):** *"The same invoice number must not be reused."*

Released numbers are tracked but never re-issued. The entity maintains a list of released numbers as a separate set.

---

## Product / ProductTranslation

> **PD-308:** *"Products have separate names defined for each language."*

The Product entity has a collection of **ProductTranslation** records — one per language code — so that invoice line items display the product name in the customer's language.

> **PD-326 (from pricing requirements):** *"Pricing units ensure price corresponds to the nature of the product: €/piece, €/kg or €/ton, €/m³, €/liter, €/meter, €/hour."*

The Product carries a **pricingUnit** enum with those six values. This determines how quantity is interpreted on BillingEvents and how time-based pricing (€/hour) is handled.

> **PD-300:** *"The system automatically applies reverse charge VAT when a product or service marked as subject to reverse VAT is added to an invoice."*

The Product has a **reverseChargeVat** boolean. When true, the entire VAT logic on the invoice line changes.

---

## AccountingAccount / CostCenter / VatRate

These three are master data entities that underpin all financial allocation.

> **PD-296:** *"Price lists, cost centers, VAT classifications, and accounting accounts are predefined at the product and service level. This ensures that the correct pricing and financial data are automatically applied when services are used."*

> **PD-295:** *"Accounts for different products are defined in connection with product price lists. For example, VAT may be posted to a VAT account, waste treatment fees to their own account, and transport fees to a separate account."*

AccountingAccount holds: code, name, validity period (accounts expire when regulations change). Linked to products via allocation rules.

> **PD-295:** *"The cost center information for an event may consist of multiple components and data points. These may include, for example, the product's cost center, the reception point's cost center, and the service responsibility information defined in the contract."*

CostCenter is a composite — its final value for an event is assembled from multiple components at runtime, not stored as a flat code. The entity defines the segments; the service composes them.

> **PD-296:** *"The system supports multiple VAT rates, including rates that vary based on the event date, ensuring compliance with national regulations."*

VatRate carries: percentage, validFrom, validTo, and type (STANDARD / ZERO / REVERSE). **The applicable rate is resolved by event date** — not by today's date. If a January event is invoiced in March and VAT changed on February 1, the January rate still applies.

---

## PropertyGroup / SharedServiceParticipant

> **PD-280:** *"This functionality is implemented by defining the shared service as a property group within the system. The property group includes the relevant properties, and the desired service is shared among them. Usage shares for the service are then defined individually for each participant."*

PropertyGroup: the shared arrangement (e.g. four neighbours with one bio-waste container). Carries a list of SharedServiceParticipant records.

SharedServiceParticipant: one per household in the group. Carries: customerID, propertyID, sharePercentage, validFrom, validTo.

> **PD-280:** *"The sum of all percentage shares must equal exactly 100%, and the application must return an error message if the total is not exactly 100%."*

This is a hard constraint enforced on every write to the participant list.

> **PD-279:** *"A new participant can also be added to a shared service retroactively, in which case all events falling within the specified date range are redistributed based on the updated participation data."*

The validFrom / validTo dates on SharedServiceParticipant are what enable retroactive redistribution — the service logic queries participants by date range, not just the current state.

---

## SeasonalFeeConfig

> **PD-288:** *"Seasonal fees refer to payments such as monthly fees/update of seasonal fees, annual fees, block collection fees, rents."*

> **PD-288:** *"A certain basic fee has been defined for each property (e.g. permanent apartment, leisure apartment), which is billed once a year. The amount of the basic fee may vary annually."*

SeasonalFeeConfig: holds the product, customer/property scope, billing frequency, amount, and nextDueDate. The nightly scheduler reads this table and generates BillingEvents for each due config. No human triggers these — they are automatic.

---

## BillingCycle

> **PD-291:** *"Each service or event type is linked to a predefined billing cycle, such as monthly, quarterly, or annually. The system automatically sorts and processes events according to their assigned schedules, preventing conflicts between different billing cycles."*

BillingCycle: linked to a contract, service, or property. Holds: frequency (MONTHLY / QUARTERLY / ANNUAL), the entity it applies to, and the next billing date. The InvoiceRun service uses this to know which events fall into the current window.

> **PD-292:** *"A one-off service such as septic tank emptying should not be delayed by other recurring services in the invoicing process."*

BillingRestriction: an override configuration that marks certain service types as IMMEDIATE — they bypass the cycle grouping entirely and are invoiced right after the event.

---

## BundlingRule

> **PD-290:** *"When creating invoice data, users can define grouping rules based on product types, services, or other predefined criteria."*

> **PD-290:** *"For one customer, all waste bin emptyings are wanted on the same line, basic payments on their own line and all waste pallet imports on their own separate lines."*

BundlingRule: per customer, maps a product group code to either SINGLE_LINE (aggregate all into one) or SEPARATE (each event on its own line). Applied at invoice generation time. Never affects the underlying BillingEvent records.

---

## MinimumFeeConfig

> **PD-286:** *"The system ensures that the minimum charge complies with the contract dates, preventing overbilling in cases where customer contracts start or end mid-season."*

> **PD-286 (PJH):** *"If the previous owner has not met the minimum charge requirement by November, the system adjusts the final invoice accordingly. Additionally, the total amount used for comparison must be VAT 0% (net invoicing)."*

MinimumFeeConfig: holds the minimum net amount and the period type (ANNUAL / QUARTERLY). The comparison uses the net (VAT 0%) total of the billing period. If the contract started or ended mid-period, the minimum does not apply to the new owner for that period.

---

## ClassificationRule

> **PD-285:** *"The system automatically determines the correct classification based on predefined criteria such as customer type, product or service classification, or regional regulations."*

ClassificationRule: a priority-ordered list of rules, each with: customer type condition, product code condition, region condition, and the resulting classification (PUBLIC_LAW or PRIVATE_LAW). At event creation, the system evaluates rules in priority order and assigns the first match. No manual classification for standard cases.

---

## Project

> **PD-287:** *"Projects (or work sites) can be created in three ways: as separate properties linked to the customer, as separate contracts for a property or a customer, as a customer hierarchy, where projects are customers linked to a main customer, each with its own contracts."*

> **PD-287:** *"If desired, the WasteHero application can be configured so that different projects are invoiced on separate invoices and each of them can have its own references."*

Project: carries a mode flag (PROPERTY / CONTRACT / HIERARCHY), a reference to the parent customer or property, and a separateInvoice boolean. When separateInvoice is true, all BillingEvents tagged with this project ID produce their own dedicated invoice with its own reference.

---

## Audit entities

> **PD-277:** *"Every change is logged, including the user who made the change, a timestamp, and the reason for the correction. The system generates before-and-after records."*

> **PD-319:** *"Price adjustments are logged, including the reason for the change, the user who performed the update, and the events affected by the change."*

> **PD-364:** *"The system logs all retroactive changes and stores details such as the user who made the change, timestamps, and the events affected by the change."*

Three distinct audit log entity types are needed:
- **BillingEventAuditLog** — field-level change record for any edit to a BillingEvent
- **PriceAdjustmentRun** — records a retroactive price correction batch (PD-319)
- **ServiceResponsibilityChangeLog** — records a retroactive service responsibility correction (PD-364)
- **BillingEventTransferLog** — records every customer transfer of an event (PD-276, PD-344)

These are append-only. Nothing in the system deletes from them.

---

## ValidationRule

> **PD-271:** *"Validation rules are configurable. Companies can define their own error-checking rules based on their invoicing requirements."*

> **PD-278:** *"The ERP system must support the fact that users build things to be checked on a case-by-case basis with the conditions defined by the user."*

> **PD-278 (use case):** *"An employee of the waste company responds to this and builds an automation that triggers an alert whenever there are more than 30 bin emptying billing events."*

ValidationRule: company-configurable. Carries: rule type (MANDATORY_FIELD / PRICE_CONSISTENCY / QUANTITY_THRESHOLD / CLASSIFICATION), configuration as structured data (e.g. `{field: "costCenter", threshold: 30}`), whether it is blocking (invoice cannot proceed) or warning-only, and whether it is active.
