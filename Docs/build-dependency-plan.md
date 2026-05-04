# Invoicing Feature — Build Dependency Plan

This document maps every requirement to its dependencies so tickets can be handed to teams in the right order. A team picking up a requirement in Wave N will never be blocked by something that isn't ready yet — all their dependencies live in earlier waves.

**Note on PD-427:** This ticket ("Invoicing REST API") has no acceptance criteria and defines nothing. It is a label, not a requirement. It is excluded from this plan — the API surface is a natural output of building the features, not something that gets built in isolation first.

---

## The Critical Path at a Glance

```
Wave 0 — Four parallel foundations (no dependencies between them)
  PD-296  What you bill for    PD-289  Who you bill
  PD-309  How you number it    PD-291  When you bill it
                    │
Wave 1 — Configuration that builds on top of those four
                    │
Wave 2 — PD-299  The billing event (the central record)
                    │
Wave 3 — PD-290  Invoice generation (the core pipeline)
                    │
     ┌──────────────┼──────────────────┐
  Simulation    Display/PDF      Transfers & Credits
```

**PD-290 (invoice generation) is the single biggest blocker.** Nothing beyond basic event management can be fully exercised until invoices can actually be produced.

---

## Wave 0 — The Four Business Foundations
*(no dependencies between them; all four can be built in parallel)*

These are the reference data pillars that every other capability points to. Nothing can be correctly configured, recorded, or invoiced without these in place.

| Ticket | Capability | Why it is foundational |
|--------|-----------|------------------------|
| **PD-296** Cost centers and accounts | The financial classification framework: cost centers, accounting accounts, price lists, and VAT rates | Every billing event must carry a cost center, an account, and a VAT rate. Without this reference data existing, no event can be correctly recorded and no financial reporting is possible. |
| **PD-289** Accounts receivable data | The customer billing profile: billing address, delivery method (paper / e-invoice / direct payment), VAT ID, business ID, e-invoice address | Every billing event belongs to a customer. Without a billing profile there is no recipient, no delivery channel, and no invoice destination. |
| **PD-309** Invoice numbering sequence | Defining and managing invoice number series: format, counter, uniqueness | Every invoice that leaves the system needs a unique number. Template selection and billing runs both depend on a number series being defined first. |
| **PD-291** Billing cycles | How often each service type is invoiced — monthly, quarterly, semi-annually, annually — per property or contract | The invoice run uses billing cycles to decide which events fall due in a given period. Without cycles, the system has no basis for grouping or timing anything. |

---

## Wave 1 — Configuration Built on the Foundations
*(requires Wave 0; most are independent of each other within this wave)*

These are customer-level settings and system-level configuration rules that must exist before billing events can be correctly processed and invoiced.

| Ticket | Capability | Depends on | What breaks without it |
|--------|-----------|------------|------------------------|
| **PD-295** Account and cost center data | Linking products and services to their default accounts and cost centers | PD-296 | Products have no default financial classification; accounts and cost centers cannot be automatically resolved when events are created. |
| **PD-282** Editing e-invoice data | Managing the customer's e-invoice address and operator identifier | PD-289 | The e-invoice integration has no address to activate or transmit to. |
| **PD-301** Gross or net invoicing | Setting whether a customer is invoiced with VAT included (gross) or excluded (net) | PD-289 | Invoice totals always use the system default; individual customer preferences are never applied. |
| **PD-300** Reverse charge VAT | Flagging customers whose VAT is handled under reverse charge rules | PD-289, PD-296 | Customers subject to reverse charge receive invoices with standard VAT applied instead of the correct treatment. |
| **PD-308** Invoice language selection | Choosing the language in which each customer's invoices are rendered | PD-289 | All invoices render in the system default language regardless of customer preference. |
| **PD-307** Invoice template selection | Defining invoice templates and linking each template to a number series | PD-309 | Billing runs cannot select a template; the number series cannot be auto-resolved at run time. Deliver this early — two further requirements depend on it. |
| **PD-310** Electronic invoice format and delivery | The structure and transmission rules for electronic invoices: format, operator envelope, delivery | PD-309, PD-307 | Electronically delivered invoices are malformed and cannot be sent. Credit notes also cannot be transmitted electronically. |
| **PD-302** Custom and bulk invoice texts | Free-text fields on invoice templates: custom messages, batch-level text overrides | PD-307 | Back-office users cannot add contextual messages to invoices; all invoices carry only system-default text. |
| **PD-285** Private and public law invoice classification | Rules that classify each billable service as either public-law or private-law | PD-296, PD-289 | Every invoice line must carry a legal classification. Without classification rules, the invoice generation cannot determine how to treat any event and cannot produce a valid invoice. |
| **PD-284** Public and private law billing configuration | Configuring whether public-law and private-law charges appear on the same invoice or on separate invoices | PD-285 | The system has no rules for how to bundle or separate charges by legal classification. |

---

## Wave 2 — The Billing Event
*(hard synchronisation point — all of Wave 3 onwards depends on this)*

The billing event is the central record of the system. Everything downstream reads, validates, transfers, or invoices events. This wave must be agreed and stable before Wave 3 begins — changes to the event data model after invoice generation is built ripple through every downstream requirement.

| Ticket | Capability | Depends on | What breaks without it |
|--------|-----------|------------|------------------------|
| **PD-299** Billing event details | The complete data model for a billing event: date, product, prices, quantities, weight, VAT amounts, cost center, account, customer reference, registration number, comments, and status | PD-296, PD-289 | There is no event to validate, edit, transfer, or invoice. This is the central record of the entire system. |
| **PD-297** Billing event status information | The status lifecycle of an event: newly created → awaiting correction → invoiced → completed, with rules on what actions are permitted at each status | PD-299 | Editing, transfer, and validation all need to know the current status before acting. Without a defined lifecycle, status transitions are undefined and downstream rules cannot be enforced. |
| **PD-283** Manual creation of billing events | Back-office users creating billing events by hand | PD-299 | Teams building editing and validation have no data to work against. This is the primary entry point for all billing data. |

**Assign these three to one team.** They define the same record and its lifecycle. Splitting them risks inconsistent field definitions that propagate through every downstream requirement.

---

## Wave 3 — Event Enrichment
*(requires Wave 2; most are independent of each other within this wave)*

| Ticket | Capability | Depends on | What breaks without it |
|--------|-----------|------------|------------------------|
| **PD-277** Manual editing of events | Back-office users correcting event fields before invoicing — quantities, prices, product, contractor fees — with a full audit trail of who changed what and when | PD-299, PD-297 | Events can be created but never corrected. The "awaiting correction" status exists in the lifecycle but nothing triggers it or resolves it. |
| **PD-318** Editing billing events | The full user-facing workflow for reviewing and editing billing events | PD-277, PD-297 | Users have no way to perform corrections through the interface. |
| **PD-278** Error listing of events | Configurable validation rules — mandatory fields, price consistency against the price list, quantity thresholds — that flag events with errors before they reach invoicing | PD-277, PD-299, PD-296 | No event is ever validated. Bad events flow through to invoices unchecked, and the simulation has no error data to surface. |
| **PD-288** Seasonal fees | Configuring recurring time-based fees that the system automatically generates as billing events on a schedule | PD-289, PD-291, PD-299 | Seasonal charges are never created. The automated scheduling has nothing to produce. |
| **PD-286** Minimum fee | Enforcing a minimum billable amount per invoice | PD-296, PD-299 | Invoices go out below the minimum threshold with no enforcement. |
| **PD-294** Billing surcharge | Automatically adding a delivery surcharge to invoices based on the customer's invoicing method | PD-296, PD-299 | Surcharges are never added; paper-invoice customers are not charged the handling fee. |
| **PD-287** Projects | Linking events to projects — defined as properties, contracts, or a customer hierarchy — and generating separate invoices per project | PD-289, PD-299 | Customers configured for per-project invoicing receive one combined invoice for all projects. |

---

## Wave 4 — Invoice Generation Pipeline
*(the single biggest blocker)*

**This is the most strategically important capability in the system.** Every display, simulation, credit, transfer, and reporting feature depends on invoices actually being produced. Do not begin Wave 5 until this is stable.

| Ticket | Capability | Depends on | What breaks without it |
|--------|-----------|------------|------------------------|
| **PD-290** Billing data generation and bundling | The full invoicing pipeline: grouping events by billing cycle and customer, applying bundling rules (combine or keep separate by product type and legal classification), calculating VAT and cost centre allocations, producing line items, assigning invoice numbers, and transmitting invoices to the delivery channel | PD-299, PD-296, PD-291, PD-285, PD-286, PD-294, PD-301, PD-307, PD-289 | **Everything in Wave 5 and beyond.** Without this, there are no invoices to display, simulate, credit, transfer, or report on. |
| **PD-292** Billing restrictions | Rules controlling how invoicing is triggered — immediate invoicing for one-off events versus scheduled batch invoicing for recurring services | PD-290, PD-291 | One-off events (e.g. on-demand pickups) sit in the queue until the next scheduled run instead of being invoiced straight away. |

**Staffing note:** PD-290 has 41 acceptance criteria and depends on 9 prior requirements. It is the highest-risk ticket in the project. Protect it from scope changes, and consider breaking it into internally reviewable stages: (1) event grouping and bundling, (2) VAT and cost centre calculations, (3) invoice number assignment and transmission.

---

## Wave 5 — Parallel Tracks
*(all require PD-290; tracks are independent of each other)*

### Track A — Validation & Simulation
| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-293** Invoice batch filtering | Filtering which customers and events are included in a billing run, by municipality, period, location, or service type | PD-290 |
| **PD-271** Automatic checks on billing data | Automated pre-invoicing checks — missing data, VAT accuracy, cost centre completeness — blocking the run when critical errors are found | PD-278, PD-290 |
| **PD-272** Simulation run of billing data | A dry run that processes all invoicing logic and produces a full summary report without issuing any real invoices | PD-271, PD-290, PD-293 |
| **PD-274** Invoice simulation | Per-customer preview of what their invoice will look like before the run is committed | PD-272 |
| **PD-273** Cancellation option for billing data | Cancelling a billing batch that has been prepared but not yet finalised | PD-290 |

Sequence within this track: PD-293 → PD-271 → PD-272 → PD-274. PD-273 can run alongside PD-272.

### Track B — Invoice Display & PDF
| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-306** Displaying invoice image | The basic invoice view — header, line items, and totals | PD-290, PD-307 |
| **PD-303** Displaying invoice image with attachments | The full invoice review screen, including attachments sent with the invoice | PD-290, PD-307, PD-308 |
| **PD-305** PDF attachment to invoice | Generating and attaching a PDF to a single invoice | PD-303, PD-307 |
| **PD-304** PDF attachment to invoice batch | Generating PDFs for an entire billing run in bulk | PD-305 |

Sequence within this track: PD-306 → PD-303 → PD-305 → PD-304.

### Track C — Billing Run Management
| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-270** System behavior during billing run | Restricting changes to billing address and billing groups while a run is active; clear messaging to users explaining what is locked and why | PD-290 |
| **PD-298** Billing data details | A detailed view of what data has been collected for a given billing period — which events are included and how they are classified | PD-290 |

Both can be worked in parallel.

### Track D — Event Transfers
| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-344** Transfer of billing events | Moving billing events from one customer to another — the core transfer capability | PD-299, PD-297 |
| **PD-275** Transfer and copy of billed events | Transferring or copying events that have already been invoiced, creating correction entries on the originals | PD-344 |
| **PD-276** Transfer of unbilled events | Moving events that have not yet been invoiced to a different customer | PD-344 |

Sequence within this track: PD-344 first, then PD-275 and PD-276 in parallel.

### Track E — Shared Services
| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-280** Shared service events on the invoice | Property groups where multiple customers share a service and are each invoiced for their percentage share; warnings when shares do not total 100% | PD-299, PD-290 |
| **PD-279** Dynamic updates to shared service events | Keeping shared service invoicing accurate as participants or percentage shares change over time | PD-280 |

Sequence: PD-280 → PD-279. PD-280 requires the data store for property groups and participant share records to be defined and populated as part of its own delivery.

---

## Wave 6 — Advanced Features
*(require specific Wave 5 tracks)*

| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-319** Price changes for unbilled events | Applying retroactive price corrections to events that have been transferred but not yet invoiced | PD-277, PD-276 |
| **PD-269** Credit invoices | Issuing full or partial credit notes against existing invoices, with electronic delivery | PD-290, PD-310, PD-303 |
| **PD-281** Updating billing information via API | External systems pushing billing profile updates into the system programmatically | PD-289, PD-282 |
| **PD-107** E-invoice integration | Receiving electronic invoice activation and termination orders from an e-invoice operator and automatically updating the customer's delivery channel and address | PD-310, PD-282, PD-289 |

---

## Wave 7 — Retroactive, Alerting & Reporting

| Ticket | Capability | Depends on |
|--------|-----------|------------|
| **PD-364** Retroactive changes to service responsibilities | Applying a change in service responsibility backwards in time, updating all affected events, reports, and pricing accordingly | PD-344, PD-276, PD-277 |
| **PD-363** Alert when euro limit exceeded | Monitoring annual billed amounts per customer by responsibility classification and triggering an alert when a configured threshold is crossed | PD-290, PD-269 |
| **PD-177** Billing event data for reporting | Revenue, VAT, and cost centre breakdown reports; data export to accounting and business intelligence systems | PD-290, PD-296 |
| **PD-171** Right of authorities to view invoices | Read-only access for regulatory authorities to view invoice records | PD-290 |
| **PD-163** Billing list | A list view of all billing activity across customers and periods | PD-290 |

---

## Dependency Matrix — Quick Reference

| Ticket | Must be done first |
|--------|--------------------|
| PD-296 | — |
| PD-289 | — |
| PD-309 | — |
| PD-291 | — |
| PD-295 | PD-296 |
| PD-282 | PD-289 |
| PD-301 | PD-289 |
| PD-300 | PD-289, PD-296 |
| PD-308 | PD-289 |
| PD-307 | PD-309 |
| PD-310 | PD-309, PD-307 |
| PD-302 | PD-307 |
| PD-285 | PD-296, PD-289 |
| PD-284 | PD-285 |
| PD-299 | PD-296, PD-289 |
| PD-297 | PD-299 |
| PD-283 | PD-299 |
| PD-277 | PD-299, PD-297 |
| PD-318 | PD-277, PD-297 |
| PD-278 | PD-277, PD-299, PD-296 |
| PD-288 | PD-289, PD-291, PD-299 |
| PD-286 | PD-296, PD-299 |
| PD-294 | PD-296, PD-299 |
| PD-287 | PD-289, PD-299 |
| **PD-290** | PD-299, PD-296, PD-291, PD-285, PD-286, PD-294, PD-301, PD-307, PD-289 |
| PD-292 | PD-290, PD-291 |
| PD-293 | PD-290 |
| PD-271 | PD-278, PD-290 |
| PD-272 | PD-271, PD-290, PD-293 |
| PD-274 | PD-272 |
| PD-273 | PD-290 |
| PD-306 | PD-290, PD-307 |
| PD-303 | PD-290, PD-307, PD-308 |
| PD-305 | PD-303, PD-307 |
| PD-304 | PD-305 |
| PD-270 | PD-290 |
| PD-298 | PD-290 |
| PD-344 | PD-299, PD-297 |
| PD-275 | PD-344 |
| PD-276 | PD-344 |
| PD-280 | PD-299, PD-290 |
| PD-279 | PD-280 |
| PD-319 | PD-277, PD-276 |
| PD-269 | PD-290, PD-310, PD-303 |
| PD-281 | PD-289, PD-282 |
| PD-107 | PD-310, PD-282, PD-289 |
| PD-364 | PD-344, PD-276, PD-277 |
| PD-363 | PD-290, PD-269 |
| PD-177 | PD-290, PD-296 |
| PD-171 | PD-290 |
| PD-163 | PD-290 |

---

## Key Risks

1. **PD-290 is the highest-risk ticket in the project.** It has 41 acceptance criteria, depends on 9 prior requirements, and blocks more than 30 further tickets. Any delay here delays everything downstream. Treat it as the critical path item and protect it from scope changes.

2. **Agree the billing event data model (PD-299) before starting PD-290.** The billing event is the central record that every downstream requirement reads or writes. If its structure changes after invoice generation is built, the impact reaches every requirement in Waves 5, 6, and 7.

3. **PD-285 (legal classification) must be delivered before PD-290 begins.** Invoice generation assigns a legal classification to every event during processing. Without classification rules in place, the system cannot determine how to treat any event.

4. **PD-280 (shared services) must include the data model for property groups and participant shares as part of its own delivery.** This is not a separate infrastructure task — it is part of what PD-280 requires to function at all.

5. **PD-107 (e-invoice integration) includes a daily import process** that pulls activation and termination orders from the e-invoice operator. This scheduled process must be built as part of PD-107, not assumed to exist from another ticket.
