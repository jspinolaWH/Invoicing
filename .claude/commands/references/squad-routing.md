# WasteHero Squad Routing Reference

Use this table to route feature requests, bug reports, and tickets to the correct squad(s).

## Quick Reference

| Squad | Trigger words / concepts |
|---|---|
| **Nexus** | customer, contact, property, container, asset, CRM, IoT sensor, fill level, MQTT, RFID, property group |
| **Ledger** | product, pricing, price list, waste fraction, zone, service level, agreement, contract, invoice, billing, skip hire, work order |
| **Forge** | auth, login, user, role, permission, MFA, OAuth, JWT, DevOps, Kubernetes, CI/CD, infra, email notification, SMS, address, geocoding, shared library, platform |
| **Bridge** | integration, Visma, SvarUt, ACOS, Matrikkel, Enhetsregisteret, F24, YTJ, TIEKE, Norwegian registry, Finnish business data, external system |
| **Access** | migration, data import, ETL, onboarding, customer data transfer |
| **Compass** | route, route planning, VRP, driver, navigation, telematics, GPS, vehicle, field operations, mobile app |
| **Beacon** | citizen portal, self-service, BankID, end-customer, resident, property contact, portal login |

## Cross-Squad Routing Notes

| Scenario | Primary squad | Supporting squad |
|---|---|---|
| Customer notification after invoice generated | Ledger (triggers event) | Forge (delivers email/SMS) |
| Citizen portal shows billing agreement | Beacon (UI) | Ledger (data source) |
| Driver app auth / role management | Forge (IAM) | Compass (app integration) |
| Norwegian property data in platform | Bridge (sync from Matrikkel) | Nexus (property entity owner) |
| New client going live | Access (migration) | All squads (feature gaps) |
| Container fill-level to route optimization | Nexus (sensor/IoT) | Compass (VRP consumes fill data) |

## Routing Rules

1. **Match trigger words first** — scan the request for keywords in the quick reference table
2. **Check ownership boundaries** — read the detailed squad file to confirm the matched squad actually owns the capability
3. **Identify cross-squad dependencies** — if the request spans squads, use the cross-squad notes and squad detail files to determine primary vs. supporting
4. **When ambiguous** — ask the user to clarify which aspect is the core deliverable. The squad that owns that aspect is primary.
