# Beacon Domain — Business Description

## What this domain is about

The Beacon domain is a citizen-facing self-service portal where individual residents and property occupants can manage their waste collection services. Citizens authenticate via official government identity systems (MitID or NemID in Denmark, BankID in Norway), view their assigned containers and pickup schedules, submit service requests, and manage notification preferences. The portal reduces inbound support calls and gives citizens direct visibility and control over their waste services.

## Who uses this

**End-users are property occupants and residents** — homeowners, renters, and occupants of multi-unit dwellings. They access the portal to:
- Understand which waste containers they are responsible for.
- Know when their containers will be emptied.
- Request changes to their waste service (adding/removing containers, changing pickup frequency).
- Report service issues (missing collection, damaged container).
- Receive notifications about upcoming collections via SMS or email.

The portal is configured per waste management company (municipality or private operator) and supports multiple authentication methods and languages.

## What citizens/end-customers can do

1. **Log in** via government digital identity (MitID, NemID, or BankID) using personal or business credentials.
2. **Select their property** from an address search after authentication (if authorised for multiple properties).
3. **View their containers**: type, waste fraction, pickup frequency, and next scheduled emptying date.
4. **View their collection schedule**: upcoming and past pickup dates with status (emptied, not emptied, completed with issues).
5. **Download collection schedules** if available.
6. **Submit service requests (tickets)**:
   - Add a container (select type, waste fraction, pickup frequency, preferred start date).
   - Remove a container.
   - Change container type.
   - Change pickup setting (frequency).
   - Request an extra emptying.
   - Request bulk waste pickup.
   - Request container or area cleaning.
   - Report a damaged container needing repair.
   - Report a missing collection (scheduled collection did not occur).
7. **Manage notification preferences**:
   - Register and verify a phone number or email address.
   - Subscribe or unsubscribe from collection notifications per container.
   - View notification history.
8. **Switch portal language** (multi-language support).

## Business flows

### A resident logging in via government identity
1. Resident accesses the portal.
2. System prompts for login method (personal or business MitID/NemID/BankID).
3. Resident authenticates via the government identity system.
4. System verifies the resident is authorised for at least one property.
5. If one property: proceed directly to that property's portal view.
6. If multiple properties: resident selects their address from a searchable list.
7. Portal loads the resident's containers, collection schedule, and available service types.
8. Session is active for 60 minutes; inactivity triggers automatic logout.

### Viewing containers and collection schedule
1. Resident navigates to the Containers page.
2. System displays all assigned containers with type, waste fraction, pickup frequency, next emptying date, and price (if configured by the operator).
3. If open service requests exist on the property, an alert is displayed.
4. Resident navigates to the Collections page.
5. System displays a pickup calendar with upcoming and past collections and their statuses.
6. Resident can filter by status or download the schedule if the operator has enabled this.

### Submitting a service request
1. Resident navigates to Self-Service and selects a service type (e.g., "Add Container").
2. Multi-step form is presented:
   - **Container selection** (for container-level requests): select the relevant container.
   - **Service specification**: choose container type, waste fraction, pickup frequency, preferred start date (where applicable).
   - **Contact information**: name, phone, email (required fields vary by service type); CAPTCHA verification.
   - **Custom questions**: service-specific fields configured by the operator.
   - **Order review**: confirm selections and see pricing if applicable.
3. Resident submits the request.
4. System creates a ticket with status Open.
5. Resident receives an on-screen confirmation.
6. The ticket is visible to back-office staff for processing.

### Setting up collection notifications
1. Resident navigates to Notification Management.
2. Step 1 — Verify contact method: select SMS or Email, enter phone/email, complete CAPTCHA, receive a one-time verification code, enter the code to confirm.
3. Step 2 — Configure subscriptions: select which containers to receive notifications for and confirm.
4. System stores preferences; resident receives alerts before or after scheduled collections.

### Login error handling
1. Resident authenticates but is not authorised for any property in the system.
2. System displays a "No Access" message with the reason (e.g., property not found, not linked to this account).
3. Resident can select a different address, log out and try a different account, or contact support.

## Business rules

- **Authentication is mandatory**: All portal content requires successful government identity authentication. Unauthenticated users see only the login page.
- **Property authorisation**: A resident can only view and manage containers for properties they are authorised for. The authorisation is based on property ownership or occupancy data from the national property registry.
- **Login method configuration**: Each operator can require personal-only, business-only, or both identity types.
- **Session expiry**: Auth tokens are valid for 60 minutes. Expired sessions trigger automatic logout.
- **Service request availability**: The list of available service types is configured per operator. Not all service types are available to all residents.
- **Contact information requirements**: The fields required when submitting a service request (name, phone, email) are configurable per service type by the operator.
- **CAPTCHA on all submissions**: All service request submissions require CAPTCHA verification to prevent spam.
- **Notification verification**: Phone numbers and email addresses must be verified via a one-time code before notifications can be enabled.
- **Notification subscriptions are per-container**: Residents choose which of their containers to receive notifications for. They can modify preferences at any time.
- **Pricing display**: Container pricing is shown only if the operator has configured "show zero prices" or if a non-zero price is associated with the service.
- **Submitted tickets are not editable**: Once a service request is submitted, it is processed by back-office staff. The resident cannot modify or cancel a submitted ticket via the portal.

## Key business terminology

- **Property**: A residential or commercial address/location where waste service is provided.
- **Property Occupant / Citizen**: An individual authorised to access and manage waste services for one or more properties.
- **Container**: A physical waste receptacle assigned to a property for a specific waste stream.
- **Container Type**: Classification of the physical receptacle (e.g., 240L, 1100L wheelie bin).
- **Waste Fraction**: The category of waste a container is designated for (e.g., food waste, glass, cardboard, residual waste).
- **Pickup Setting**: The frequency or schedule of collections (e.g., weekly, bi-weekly, monthly, on-demand).
- **Ticket**: A service request submitted by a resident (e.g., "Add Container," "Repair Container," "Missing Collection").
- **Service Type**: A specific type of service request available in the portal (e.g., "Extra Emptying," "Change Container Type"). Configured per operator.
- **Collection / Pickup**: A scheduled emptying of one or more containers on a specific date.
- **Pickup Status**: The outcome of a scheduled collection (emptied, not emptied, completed with issues).
- **Verification Code**: A one-time code sent via SMS or email to validate the resident's contact method before enabling notifications.
- **Notification Subscription**: An opt-in preference to receive SMS or email alerts before or after a scheduled collection for a specific container.
- **MitID / NemID**: Danish government digital identity systems used for citizen authentication.
- **BankID**: Norwegian government digital identity system used for citizen authentication.

## What this domain does NOT handle

- **Back-office CRM operations**: Staff use a separate back-office CRM system to process tickets, manage customer records, and configure service rules.
- **Billing and payment**: No payment processing or invoice management. Billing is handled by the Ledger domain.
- **Pricing configuration**: Prices are configured in the back-office. The portal only displays pre-configured prices.
- **Route planning and dispatch**: Collection scheduling and driver routing are managed by the Compass domain.
- **Property ownership and tenancy administration**: The portal does not manage property ownership records or tenancy agreements. These come from the national property registry via the Bridge domain.
- **Waste disposal and environmental reporting**: No tracking of waste volumes after collection or environmental impact reporting.
- **Driver and contractor management**: Drivers and contractors use separate systems. The Beacon portal is exclusively citizen-facing.
- **Data synchronisation with external registries**: The portal consumes property and ownership data; syncing that data from government registries is handled by the Bridge domain.
