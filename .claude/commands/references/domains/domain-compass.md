# Compass Domain — Business Description

## What this domain is about

The Compass domain manages real-time waste collection route execution and optimisation. It covers the complete lifecycle of routes: from planning and optimisation through driver field execution, with real-time GPS tracking and evidence collection at each collection point. Drivers use a mobile app to navigate routes, complete stops at containers or waste stations, and update the system with collection measurements and any deviations from the plan.

## Core business entities

**Route** — A planned sequence of stops (pickup locations) scheduled for a specific day and assigned to one or more vehicles and drivers. Routes have a defined start time, anticipated duration, and can be automatically optimised by the route solver or manually adjusted by dispatchers. Routes must respect vehicle capacity constraints and time windows for customer collections.

**Stop (Pickup Order)** — A location within a route where waste must be collected. Each stop corresponds to either a container at a property location or a waste station. Stops have a sequence number, geographical coordinates, and a status tracking completion. Each stop must be individually marked as complete by a driver, typically with evidence (photo, QR code) and measurements (weight or fill level).

**Vehicle** — A physical collection vehicle assigned to execute one or more routes. Vehicles have capacity limits (volume and weight), real-time location tracking via GPS telematics, an assigned primary driver, and an availability status (working, off-work, unavailable). A vehicle can only be on one active route at a time.

**Driver** — A person operating a vehicle on one or more routes. Drivers log in via the mobile app, select or are assigned a vehicle, and navigate through assigned routes. The system tracks which driver completed each stop and logs all field actions.

**Container** — A physical waste receptacle at a property. In the Compass context, containers are stops to be serviced: they have a type (volume/capacity), a waste fraction classification, a pickup setting (frequency and schedule), and a GPS location.

**Ticket** — A service request or task related to collection operations. Tickets can be created by dispatchers before a route (planned) or by drivers during a route (ad hoc). Types include: Add Container, Remove Container, Change Container Type, Extra Emptying, Missing Collection, Repair, Bulk Waste Pickup, and others. Tickets capture custom field responses (text, photos, signatures) and must be resolved before a route is considered fully complete.

**Waste Station (Depot)** — A facility where drivers deposit collected waste. Waste stations are stops on a route and typically require weight measurements. They also serve as route start and end points for driver shifts.

**Route Scheme** — A configuration template that governs how drivers interact with a route. It specifies:
- Whether drivers must record weight, fill level, or QR code scans at each stop.
- Whether drivers can add stops, edit containers, or reorder stops during execution.
- Whether stops must be completed strictly in sequence or drivers can skip ahead.
- Auto-complete distance and time thresholds.
- Whether collaborative (multi-driver) routes are permitted.

**Route Vehicle Session** — A record linking a specific driver and vehicle to a specific route during a specific time period. Used for collaborative routes where multiple drivers work the same route, and for tracking vehicle swaps mid-route.

**Telematics Record** — Real-time vehicle data ingested from GPS hardware: precise coordinates, altitude, speed, heading, engine status, battery voltage, fuel level, mileage, accelerometer readings, harsh braking events, and temperature sensor data.

## Business flows

### Planning and optimising a route
1. A dispatcher creates a route by selecting stops (containers and/or waste stations) for a scheduled date.
2. The dispatcher assigns a vehicle (or allows auto-assignment based on capacity and geography).
3. The VRP (Vehicle Routing Problem) solver optimises the stop sequence, considering:
   - Vehicle capacity constraints (weight and volume).
   - Time windows for collections (e.g., "collect between 09:00–12:00").
   - Geographical distance and travel time.
   - Driver shift hours and required breaks.
4. The optimised route is published to the mobile app and becomes visible to assigned drivers.

### A driver starting a route
1. The driver logs into the mobile app and selects their assigned vehicle.
2. The app displays all assigned routes for the day (or the next upcoming route).
3. The driver reviews the route: stops, estimated duration, vehicle assignment, and any special instructions.
4. The driver presses "Start Route," which:
   - Records the start time.
   - Activates GPS tracking (continuous position updates begin).
   - Enables turn-by-turn navigation.
   - Makes the route status visible to dispatchers in real time.

### A driver executing a route (completing stops)
1. The driver navigates to each stop following the app's navigation.
2. At each stop, the driver:
   - Confirms arrival (via GPS proximity or manual confirmation).
   - Views stop details: container info, waste type, last collection date, special instructions.
   - Performs the collection action (empties container, deposits at waste station, etc.).
   - Records required measurements per the route scheme:
     - **Weight**: estimated kilograms collected.
     - **Fill level**: percentage fill of the container before emptying (0–100%).
     - **QR code**: scans the container's QR code to verify identity.
   - Optionally attaches photos as proof.
   - Presses "Complete Stop."
3. The driver may also (if the route scheme allows):
   - Skip a stop and return to it later.
   - Reorder the remaining stops.
   - Add new containers encountered in the field.
   - Create tickets for issues (damaged container, access blocked, customer complaint).
4. The system records completion time, measurements, and driver identity for each stop immutably.

### Recording a deviation and creating a ticket
1. During execution, the driver encounters a situation outside the plan (e.g., a container not scheduled for collection is overflowing).
2. The driver creates a ticket from the mobile app, specifying:
   - Ticket type (e.g., "Extra Emptying," "Repair Container").
   - Which container or property it concerns.
   - Custom field responses (photos, signature, text notes).
   - Priority level.
3. The ticket is logged and visible to dispatchers immediately.
4. Some ticket types require specific completion actions before the ticket can be closed (e.g., RFID tag scanned for "Add Container," photo required for "Repair").

### Real-time GPS tracking of a fleet
1. Once a route is started, the driver's vehicle continuously transmits position and telematics data.
2. The telematics service ingests and stores:
   - Precise GPS coordinates, altitude, speed, heading.
   - Vehicle diagnostics: engine status, battery voltage, fuel level, mileage.
   - Motion data: accelerometer readings, harsh braking events.
   - Environmental sensor data (temperature, etc.) if available.
3. Dispatchers view vehicle locations on a live map.
4. GPS data is used to infer stop arrival and departure times if drivers do not manually confirm.

### Completing a route
1. The driver completes all stops (or the dispatcher marks the route as complete).
2. The system records:
   - Total distance travelled.
   - Total time (including breaks).
   - Total weight/volume collected.
   - Any stops marked "not completed" (access blocked, customer refused, etc.).
3. Route status changes to Completed and is no longer active in the driver's app.
4. Any incomplete tickets remain open for dispatcher follow-up.

## Business rules

### Route constraints
- A route must have at least one assigned vehicle.
- A vehicle must be available (Working or Off-Work status) to be assigned to a route.
- A route must respect vehicle capacity: the sum of estimated waste weights must not exceed the vehicle's weight limit.
- A vehicle can only be on one active route at a time.

### Stop completion rules
- A stop cannot be marked complete until all required fields per the route scheme are filled:
  - If weight is required, weight must be a positive number.
  - If fill level is required, fill level must be 0–100%.
  - If QR code is required, a valid QR must be scanned.
- A stop can only be completed once, unless explicitly reset by a dispatcher.
- If the route scheme specifies "complete in sequence only," stops must be done in order; skipping is blocked.

### Ticket constraints
- A ticket must have a type and be associated with a container, property, or route.
- Tickets with attachment requirements (photo, signature) cannot be marked complete without the attachment.
- Certain ticket types require specific completion actions (e.g., RFID pairing requires an RFID tag to be scanned; "Add Container" requires a barcode scan).

### Time windows
- Stops may have time windows. The app warns the driver if they arrive outside the window.
- Routes have a scheduled start time; drivers are not expected to start before the scheduled date.

### Data integrity
- All stop completions, measurements, and timestamps are logged with the driver ID and vehicle ID.
- Completion records cannot be edited; resetting a stop is the only way to correct an error.
- All measurements (weight, fill level, QR) are immutable once recorded.

## Key business terminology

- **Route**: A planned sequence of waste collection stops for a specific date, assigned to a vehicle and driver.
- **Stop**: A single collection point within a route (a container at a property or a waste station).
- **Route Scheme**: A configuration controlling driver behaviour: what must be recorded at each stop, what actions are allowed, whether stops must be done in sequence.
- **Pickup Setting**: The collection frequency and rules for a specific container (e.g., weekly, bi-weekly, on-demand).
- **Waste Fraction**: The type of waste material a container is designated for (organic, paper, plastic, glass, residual, etc.).
- **Fill Level**: The percentage of the container's capacity that is full at the time of collection, recorded by the driver.
- **Waste Estimate (Weight)**: The estimated or measured kilograms of waste collected at a stop.
- **Ticket**: A service request or field task, created before a route (planned) or during a route (ad hoc).
- **Deviation**: An unplanned event during route execution captured via a ticket (e.g., overflowing container, access blocked, extra collection requested).
- **Route Vehicle Session**: A record linking a driver and vehicle to a route for a specific time period. Supports multi-driver collaborative routes and vehicle swaps.
- **Collaborative Route**: A route worked by multiple drivers and vehicles, with shared stop visibility.
- **Auto-Complete**: A feature where stops are automatically marked complete when the driver arrives within a configured distance and time threshold, reducing manual interaction.
- **RFID Pairing**: The process of associating a new RFID tag with a container record in the system, required before the container can be routinely serviced.
- **Telematics**: Real-time vehicle data (GPS position, speed, engine status, diagnostics) ingested from on-board GPS hardware.
- **VRP (Vehicle Routing Problem)**: The optimisation problem solved when assigning and sequencing stops to vehicles and drivers to minimise distance and time.

## What this domain does NOT handle

- **Billing and invoicing**: Compass collects route execution data used for billing, but does not calculate or generate invoices. That belongs to the Ledger domain.
- **Customer relationship management**: Customer contact details, service agreements, and complaint handling belong to the Nexus domain.
- **Container asset management**: Container records, types, and property assignments are managed by the Nexus domain.
- **Waste processing after collection**: What happens to waste after the vehicle arrives at a waste station (sorting, disposal, recycling) is outside Compass.
- **Vehicle maintenance scheduling**: While telematics captures engine diagnostics, vehicle maintenance belongs to fleet management systems outside Compass.
- **Driver authentication and access control**: Login, roles, and permissions are managed by the Forge domain.
- **Historical analytics and reporting**: Compass collects execution data; aggregation, trend analysis, and dashboards belong to analytics and reporting tools.
- **Citizen-facing services**: Residents do not interact with Compass; citizen self-service belongs to the Beacon domain.
