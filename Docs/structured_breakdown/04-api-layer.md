# 04 — API Layer
## What endpoints exist and why the requirements demand them

Every endpoint below is derived from a user story or acceptance criterion. The URL naming follows RESTful conventions. All endpoints are secured — see 06-cross-cutting.md for roles.

---

## Billing Events

### POST /billing-events
Creates a new billing event from an external source (route management, weighbridge, POS integration).
> **PD-299:** *"Event data can also be generated via a weighbridge integration or a cashier/POS system integration."*

Request body contains the full event schema. The service resolves VAT rate by event date, resolves legal classification by customer and product, and sets status = IN_PROGRESS.

### POST /billing-events/manual
Creates a manually entered billing event.
> **PD-283:** *"Users can manually enter invoicing events for services that do not originate from automated integrations."*

The user provides: customer ID, product ID, quantity/hours, date, and optional comment. The product ID drives all financial metadata — the user cannot set accounts or VAT manually.

### GET /billing-events
Returns a filtered list of billing events.
> **PD-318:** *"Billing events must be viewable and filterable according to the ongoing billing run, for example by date, products of a specific service, municipality, or service responsibility."*

Supports query parameters: customerId, status, municipality, dateFrom, dateTo, productId, serviceResponsibility, excluded, requiresReview.

### GET /billing-events/{id}
Returns full details of a single billing event including audit history.
> **PD-277:** *"The system generates before-and-after records, ensuring transparency and providing references for customer inquiries."*

### PATCH /billing-events/{id}
Edits a billing event's correctable fields.
> **PD-277:** *"Authorized users can update event data, including product quantities, event type, contractor fees, and customer price."*

Requires a `reason` field in the request body. Blocked if event status is SENT or COMPLETED.

### POST /billing-events/{id}/exclude
Excludes a single event from invoicing.
> **PD-318:** *"Users can exclude an individual event from invoicing."*

### POST /billing-events/{id}/reinstate
Reinstates a previously excluded event.
> **PD-318:** *"Users can update the status of these events later once the issue has been resolved."*

### POST /billing-events/bulk-exclude
Excludes multiple events at once.
> **PD-318:** *"Bulk exclusion: Office staff can select and exclude multiple events from invoicing in a single step."*

Request body: list of event IDs and a shared exclusion reason.

### POST /billing-events/{id}/transfer
Transfers a single unbilled event to a different customer.
> **PD-276:** *"Uninvoiced transactions must be able to be transferred and allocated from one customer/target to another."*

### POST /billing-events/bulk-transfer
Transfers multiple unbilled events to a different customer.
> **PD-344:** *"Billing events must be transferable from one customer to another, for example when a resident changes."*

### POST /billing-events/validate
Runs pre-billing validation checks on a list of events without starting a run.
> **PD-278:** *"An event must have mandatory data, such as accounts and cost centres, in order for it to be transferred to billing."*

Returns a report of which events have missing data and what is missing on each.

---

## Driver Events

### POST /driver/events
Driver submits a billing event from the mobile app.
> **PD-228:** *"Events such as waste collection, waste weighing at a sorting station, or additional services performed on site are recorded with the assigned driver and vehicle details."*

Mobile-facing endpoint. Authenticated with driver credentials. Request must include driverId and vehicleId. The service determines whether office review is required based on event type config.

### GET /billing-events/pending-review
Returns events waiting for office review.
> **PD-228:** *"Events must be able to be reviewed in bulk for error detection."*

### POST /billing-events/{id}/approve
Office user approves a driver-submitted event.
> **PD-228:** *"Additional tasks can be registered directly to the customer's invoice without requiring separate confirmation by the office user"* — and by implication, those that DO require confirmation need an approval action.

### POST /billing-events/{id}/reject
Office user rejects a driver-submitted event with a reason.

---

## Invoice Runs

### POST /invoice-runs
Starts a new invoice run (real or simulation).
> **PD-272:** *"Users can run a test run that generates invoice data without finalizing or sending invoices."*
> **PD-293:** *"It must be possible to define different limit conditions for printing the invoice."*

Request body includes all filter criteria (PD-293) and a `simulation: boolean` flag. If simulation = true, returns a SimulationReport immediately. If simulation = false, the run starts and its ID is returned for polling.

### POST /invoice-runs/simulate
Convenience endpoint for a simulation-only run — returns the full SimulationReport synchronously.
> **PD-272:** *"A summary report must be obtained from the simulation run."*

### GET /invoice-runs/{id}
Returns the current state and progress of a billing run.
> **PD-273:** *"Invoice datasets are available in a consolidated view, where both the status of the invoice data can be seen and the invoice data can be canceled."*

### POST /invoice-runs/{id}/cancel
Cancels a billing run before transmission.
> **PD-273:** *"The system must have a confirmation/cancellation option if the material has already been created, but has not yet been sent."*

Reverts all invoice statuses. Releases all invoice numbers. Removes all run locks.

### POST /invoice-runs/{id}/schedule-send
Schedules the run to transmit at a future time.
> **PD-273 (PJH):** *"PJH wants the billing data not to be sent automatically in the first phase. PJH wants the billing data to be sent by the billing user."*

### POST /invoice-runs/{id}/send
Manually triggers the transmission of a ready run.
> **PD-273 (PJH):** *"PJH wants the billing data to be sent by the billing user."*

### PUT /invoice-runs/{id}/batch-attachment
Attaches a single PDF to all invoices in a run.
> **PD-304:** *"A pdf attachment must be attached to the entire batch of invoices."*

Request body: the attachment identifier (pre-uploaded to the invoicing service). The identifier is written into every FINVOICE message for this run.

---

## Invoices

### GET /invoices/{id}
Returns full invoice details.
> **PD-274:** *"Users can generate a draft invoice that replicates the final invoice."*

### POST /invoices/preview
Generates a draft invoice preview for a customer without persisting anything.
> **PD-274:** *"Users can simulate an invoice so that the simulated invoice contains all the information that the corresponding invoice would contain."*

### PATCH /invoices/{id}/text
Sets or updates the custom text on an invoice.
> **PD-302:** *"Users can add custom text to invoices on a per-customer basis or across multiple invoices at once."*

### POST /invoices/bulk-text
Sets the same custom text on multiple invoices.
> **PD-302:** *"I want to be able to include customised text content in invoices for individual or multiple invoices."*

### GET /invoices/{id}/image
Retrieves the invoice image from the external invoicing system.
> **PD-306:** *"The system sends a request to the external invoicing system to retrieve the corresponding invoice image. The image is then displayed in a dedicated viewing window within WasteHero ERP."*

Returns: binary image data (PDF) with appropriate Content-Type header.

### GET /invoices/{id}/attachments
Lists the attachment metadata for an invoice.
> **PD-303:** *"When the sent invoice is opened for review, the attachments sent with the invoice are also seen."*

### GET /invoices/{id}/attachments/{attachmentId}
Retrieves a specific attachment binary from the external system.
> **PD-303:** *"WasteHero ERP retrieves the attachment dynamically and displays it alongside the invoice."*

### POST /invoices/{id}/attachments
Uploads a PDF attachment to an invoice.
> **PD-305:** *"Users can attach up to ten PDF documents to an invoice in the WasteHero application."*

Multipart upload. Validated against: count ≤ 10, total size < 1 MB before encoding, format must be PDF/A, JPEG, or PNG.

### POST /invoices/{id}/remove-surcharge
Removes the invoicing surcharge from a specific invoice.
> **PD-294:** *"It must be possible to remove the invoicing surcharge, if necessary, so that I can account for exceptional circumstances."*

### POST /invoices/{id}/correct
Creates a credit note and copies the events to a new invoice for correction.
> **PD-275:** *"The user can copy or transfer invoiced events to the same or a different customer or property."*

Request body: which line items to credit, and the target customer ID (may be same customer).

---

## Credit Notes

### POST /invoices/{id}/credit
Issues a full or partial credit note against an invoice.
> **PD-269:** *"It must be possible to create a refund invoice from the desired invoice."*

Request body: `type` (FULL or PARTIAL), `lineItemIds` (for partial), `customText`, `internalComment`. The `internalComment` field is mandatory.

### POST /invoices/batch-credit
Issues credit notes against multiple invoices in one operation.
> **PD-269:** *"A credit note can be created as a separate batch run if, for example, credit notes are to be created for several different invoices at the same time."*

### GET /customers/{id}/credit-history
Returns all credit notes issued for a customer.
> **PD-269:** *"Information about reimbursed transactions must remain in the customer's transaction data."*

---

## Billing Profiles and Customer Data

### GET /customers/{id}/billing-profile
Returns the customer's billing profile.

### PUT /customers/{id}/billing-profile
Updates the customer's billing profile.
> **PD-298:** *"Waste management companies can define additional mandatory fields to meet their own needs."*
> **PD-281:** *"Any changes I make to customer invoicing information in the WasteHero application to be automatically synchronised with the external invoicing system."*

Triggers BillingProfileSyncService on save.

### PUT /customers/{id}/einvoice-address
Manually sets or updates the e-invoice address.
> **PD-282:** *"It must also be possible to edit e-invoicing addresses manually, and it must be possible to add new e-invoicing addresses, even if the information is not available from the integration."*

Includes a `lock` flag: when `lock: true`, the address is protected from integration overwrites.

---

## Billing Cycles and Restrictions

### GET /billing-cycles
Lists all configured billing cycles.

### POST /billing-cycles
Creates a new billing cycle for a contract, service, or property.
> **PD-291:** *"Users can define and adjust billing intervals through an intuitive user interface."*

### PUT /billing-cycles/{id}
Updates a billing cycle (e.g. changing from quarterly to annual).
> **PD-291:** *"Changes in customer preferences or contracts are reflected without affecting other services."*

### GET /billing-restrictions
Lists billing restrictions (which service types are invoiced immediately).

### POST /billing-restrictions / PUT /billing-restrictions/{id}
Configures which service types bypass the billing cycle and go to immediate invoicing.
> **PD-292:** *"A one-off service such as septic tank emptying should not be delayed by other recurring services in the invoicing process."*

---

## Bundling Rules

### GET /customers/{id}/bundling-rules
Returns the bundling configuration for a customer.

### PUT /customers/{id}/bundling-rules
Updates the bundling rules for a customer.
> **PD-290:** *"When creating invoice data, users can define grouping rules based on product types, services, or other predefined criteria."*

---

## Shared Services

### GET /property-groups
Lists all property groups (shared services).

### POST /property-groups
Creates a new shared service arrangement.
> **PD-280:** *"This functionality is implemented by defining the shared service as a property group within the system."*

### GET /property-groups/{id}
Returns the group with all participants and their current percentages.

### PUT /property-groups/{id}/participants
Updates the participant list and percentages. Validates that total = 100%.
> **PD-280:** *"The sum of all percentage shares must equal exactly 100%."*

### POST /property-groups/{id}/add-participant-retroactive
Adds a participant retroactively and redistributes past events.
> **PD-279:** *"A new participant can also be added to a shared service retroactively, in which case all events falling within the specified date range are redistributed."*

### GET /property-groups/{id}/validate
Checks that all shares currently sum to 100%.
> **PD-280:** *"The application issues a warning if the total exceeds or falls below 100%."*

---

## Projects

### GET /customers/{id}/projects / POST /customers/{id}/projects
List and create projects for a customer.
> **PD-287:** *"The transactions that form the basis of the invoicing have the feature with which the customer has the opportunity to establish several projects."*

### PUT /customers/{id}/projects/{projectId}
Updates a project, including its separateInvoice flag.

---

## Retroactive Operations

### POST /price-adjustments/preview
Returns a preview of which events would be affected by a retroactive price change.
> **PD-319:** *"The solution also includes tools for validating and previewing the impact of retroactive price changes before they are finalised."*

### POST /price-adjustments/{previewId}/apply
Commits the previewed price changes.
> **PD-319:** *"Once the new prices have been applied, the system recalculates the affected events."*

### POST /service-responsibilities/retroactive-change/preview
Previews the events that would be affected by a service responsibility change.
> **PD-364:** *"The system identifies and updates all related uninvoiced events accordingly."*

### POST /service-responsibilities/retroactive-change/{previewId}/apply
Applies the service responsibility change to all matching unbilled events.

---

## Accounting Master Data

### GET /accounting-accounts / POST / PUT /{id}
CRUD for accounting accounts.
> **PD-296:** *"The system allows office users to manually add, update, and maintain cost centers, VAT rates, and accounting accounts."*

### GET /cost-centers / POST / PUT /{id}
CRUD for cost centres.

### GET /vat-rates / POST / PUT /{id}
CRUD for VAT rates with validity periods.
> **PD-296:** *"The system supports multiple VAT rates, including rates that vary based on the event date."*

### GET /allocation-rules / POST / PUT /{id}
CRUD for accounting allocation rules (product → account/cost centre mapping).
> **PD-295:** *"The same product must be able to be routed to a different account and cost center based on e.g. price list or region."*

---

## Invoice Number Series

### GET /invoice-number-series / POST / PUT /{id}
CRUD for invoice number series.
> **PD-309:** *"Waste management companies have full control over defining and managing invoice number series."*

---

## Validation Rules

### GET /validation-rules / POST / PUT /{id}
CRUD for company-configurable validation rules.
> **PD-271:** *"Companies can define their own error-checking rules based on their invoicing requirements."*

---

## Invoicing Surcharge Configuration

### GET /invoicing-surcharge-config / PUT
Manage the global surcharge configuration.
> **PD-294:** *"However, it must be possible to globally toggle the invoicing-method surcharge on and off."*

---

## Authority Access (read-only, restricted role)

### GET /authority/customers/{id}/invoices
Returns all sent/completed invoices for a customer in the authority view.
> **PD-171:** *"Authorities must have the ability to view invoices sent to customers and open the invoice view independently."*

### GET /authority/invoices/{id}/image
Returns the invoice image exactly as sent to the customer.
> **PD-171:** *"In the viewing view, the image of the invoice must appear as it did when it was sent to the customer."*

These two endpoints are the entire authority interface. No write operations. No draft invoice access. Only sent and completed invoices.

---

## Seasonal Fees

### GET /seasonal-fee-configs / POST / PUT /{id}
CRUD for seasonal fee configurations.
> **PD-288:** *"The system must also be able to create invoicing material for time-bound products/services that do not have an event-specific component."*

---

## Minimum Fee

### GET /minimum-fee-configs / POST / PUT /{id}
CRUD for minimum fee configurations per customer or globally.
> **PD-286:** *"The system must have a function that can be used to calculate the amount in euros, if the amount of invoicing transactions is calculated to be below the minimum payment level."*
