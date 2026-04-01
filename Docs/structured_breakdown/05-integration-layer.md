# 05 — Integration Layer
## What external systems exist, what WasteHero sends and receives, and why

There are three distinct external system relationships in the invoicing module. Each has different data flow direction, frequency, and failure handling requirements.

---

## 1. External Invoicing System (Ropo, Netvisor, or similar)

This is the system that actually sends invoices to customers and manages the accounts receivable ledger. WasteHero generates the data; the external system handles delivery, payment tracking, and archiving.

> **PD-298 (implementation note):** *"The basic FINVOICE data is defined as common for all companies, and if deviations are needed, this is possible company-specifically as a change request."*

WasteHero does not manage the invoicing system — it feeds it. The relationship is one-way for invoice transmission, but bidirectional for status feedback and image retrieval.

### What WasteHero sends to the external system

**FINVOICE 3.0 XML files — one per invoice**
> **PD-310:** *"The FINVOICE material is to be generated during the invoicing run. The current latest standard is Finvoice 3."*

Each FINVOICE file is a complete invoice document in the Finnish standard e-invoice format. The file includes:

- Seller party details (the waste company)
- Buyer party details — from BillingProfile in the customer's language (PD-308)
- Invoice rows — one per line item, with product name in the customer's language
- Each invoice row may have multiple sub-entries for ledger account splits (PD-295)
- VAT specification details — broken down per VAT rate (PD-301, PD-300)
- Public/private law classification metadata per line (PD-284)
- Invoice template code (PD-307)
- Language code (PD-308)
- Custom free text if set (PD-302)
- Attachment references (PD-305, PD-304)

> **PD-310:** *"A single invoice line visible to the customer may be split in the FINVOICE data across different ledger accounts for accounting purposes."*

This means the customer sees one line (e.g. "Waste container collection") but the FINVOICE file contains three sub-entries (transport revenue account, treatment revenue account, VAT account) for the same amount. The external accounting system uses the sub-entries for its ledger allocation.

> **PD-300:** *"The invoice includes required legal references, the buyer's VAT number, and a clear indication that reverse charge VAT applies."*

When reverse charge VAT applies, the FINVOICE must contain the legal basis text and the buyer's VAT registration number. The Finnish Finvoice 3.0 specification defines exactly where these appear in the XML schema.

**Billing address updates**
> **PD-281:** *"When a customer's billing address changes, the ERP system updates the correct billing address via the integration interface."*

When a billing address is updated in WasteHero, a call goes to the external system's customer update API. This is not part of the FINVOICE flow — it is a separate customer master data sync. The external system must apply the new address to all open invoices for that customer, including any already transmitted.

**E-invoice address updates**
> **PD-281:** *"Any changes I make to customer invoicing information in the WasteHero application to be automatically applied to all open invoices for the respective customers."*

Same sync pattern as billing address — when the e-invoice address changes in WasteHero (whether from manual edit or from the operator integration), WasteHero pushes it to the external system.

### What WasteHero receives from the external system

**Transmission confirmation**
> **PD-297:** *"Completed: an invoice has been created from the event and the invoice data has been successfully sent to and confirmed by the invoicing system."*

The external system confirms receipt of the FINVOICE file. WasteHero updates the invoice status from SENT to COMPLETED, and the BillingEvent status to COMPLETED. If the external system reports an error, the invoice moves to ERROR status.

**Invoice images (on demand)**
> **PD-306:** *"When a user selects an invoice in the ERP system, the system sends a request to the external invoicing system to retrieve the corresponding invoice image."*

WasteHero does not store invoice images locally. When a user wants to view an invoice, WasteHero calls the external system's image retrieval API with the invoice number. The image is streamed directly to the user — not cached in WasteHero.

**Attachment files (on demand)**
> **PD-303:** *"The external system must provide an API endpoint through which WasteHero ERP can request and retrieve invoice attachments."*

> **PD-303:** *"WasteHero must be able to request attachments dynamically via the API rather than relying on preloaded storage."*

The external system must support:
- An API endpoint accepting invoice number + attachment identifier
- Response: the attachment binary in its original format (PDF/A, JPEG, or PNG)
- A structured error response if the attachment is missing

This is a requirement WasteHero places on the external system — the integration spec must include it.

### Invoice recall

> **PD-273:** *"I want to be able to cancel the sending of invoices from a third-party invoicing system after they have been sent from the WasteHero application to that system, if supported by the integration between the two systems."*

Recall is only possible if the external system's API supports it. WasteHero calls the recall endpoint with the invoice number. The external system responds with success or failure. WasteHero updates the status accordingly. If the external system does not support recall (e.g. the invoice has already been sent to the customer), WasteHero reports this clearly to the user.

---

## 2. Invoice Operator (Ropo, Maventa, or similar)

The invoice operator sits between WasteHero and the customers' banks. It routes e-invoices and handles the e-invoice address registration flow.

> **PD-107:** *"WasteHero receives information via integration from an invoice intermediary or a billing system about a customer's e-invoice and direct debit orders."*

This relationship is inbound — the operator pushes messages to WasteHero (or WasteHero polls for them). WasteHero does not push anything to the operator directly for the e-invoice address registration flow. That happens between the customer, their bank, and the operator.

### What WasteHero receives from the operator

**E-invoice order messages — delivered once per day**

> **PD-107:** *"The integration transmits information indicating either the start or the termination of an order. The data content includes information that allows the order to be matched to the correct customer."*

Each message contains:
- Order type: START or TERMINATE
- One or two customer identifiers (e.g. invoice reference + customer number) — the specific identifiers are agreed between the waste company, the operator, and the bank
- E-invoice address (for START orders)
- Operator code

WasteHero matches each message to a customer record using the agreed identifiers. Two-identifier matching is supported:

> **PD-107:** *"Two different identifiers can also be used for matching. An agreement on which identifiers are used for matching is made between the companies and the bank."*

On START: the customer's delivery method changes to E_INVOICE, the e-invoice address is set, the change is logged.
On TERMINATE: delivery method reverts to PAPER, e-invoice address is removed, the change is logged.

Before updating, the lock flag is checked:
> **PD-282:** *"A setting can be added to an e-invoicing address indicating that the recorded e-invoicing address must not be overwritten via integration."*

If `manuallyLocked = true`, the integration message is logged but the address is NOT updated. The office user has explicitly said their manual entry is correct.

**The Kiertokapula flow (concrete example from the requirements)**
> **PD-107:** *"Kiertokapula has an e-invoicing contract with Nordea. All messages related to e-invoicing go through Ropo to Nordea and from there to the recipient's bank. Messages from the bank follow the same route from the end customer's bank through Nordea to Ropo and from Ropo to Kiertokapula."*

The full path: customer action at their bank → Nordea → Ropo → WasteHero. WasteHero is the final destination of the message. The daily batch from Ropo is what WasteHero processes.

---

## 3. FINVOICE — the data format itself

FINVOICE is not a system — it is the XML standard that defines the content of Finnish invoices. Understanding it is essential for building the FinvoiceBuilderService correctly.

> **PD-310:** *"The FINVOICE specification/definition is to be agreed during implementation. The current latest standard is Finvoice 3."*

The official specification is at finanssiala.fi. For the implementation, the key elements that WasteHero must populate are:

**Attachment handling in FINVOICE**
> **PD-305:** *"The details of individual attachments are given in the AttachmentDetails section. There can be several attachments, in which case the AttachmentDetails part is repeated according to the number of attachments. There may be no more than ten attachments and their total size may not exceed one megabyte before encoding."*

> **PD-305:** *"The ID of the individual attachment is given in the AttachmentIdentifier field and is formed from the ID of the attachment message and the SHA1 check calculated from the contents of the attachment."*

> **PD-305:** *"The actual attachment file is given base64-encoded in the AttachmentContent field."*

> **PD-305:** *"If the sender has marked the attachment as confidential, it is indicated in the AttachmentSecurityClass field, so-called With SEI code, e.g. SEI01."*

> **PD-305:** *"For PDF documents, the only type allowed is PDF/A."*

The FinvoiceBuilderService must validate all of these constraints before generating the FINVOICE XML. Any violation must prevent the FINVOICE from being generated — not just logged as a warning.

**Batch attachment (reference only)**
> **PD-304:** *"The FINVOICE message contains the information that there will be an ATTACHMENT to the invoice and the identification information used to identify the attachment. The self-added attachment is ready in the invoicing service and is not transmitted in FINVOICE format."*

For batch attachments, the FINVOICE file does NOT contain the attachment binary. It contains only the AttachmentIdentifier — a reference to a file that has been pre-uploaded to the invoicing service by the user. The invoicing service uses the identifier to associate the correct file with each invoice.

**Multiple VAT rates on one invoice**
> **PD-310:** *"The solution will support multiple VAT rates, apply the correct VAT rate, and allow VAT rates with decimals to ensure compliance with tax regulations."*

The Finnish VAT Act uses rates like 25.5% (the 2024 rate). FINVOICE 3.0 supports decimal percentages. The FinvoiceBuilderService must handle them correctly — rounding errors in VAT calculation will cause the accounting to not balance.

---

## 4. Integration failure handling

No integration is guaranteed to succeed. Each external call needs defined failure behaviour:

**FINVOICE transmission failure**
> **PD-297:** *"Error: events that contain incorrect data or where errors occurred during the transfer."*

If transmission fails, the invoice moves to ERROR status. The BillingEvent remains in SENT status. The user sees the error in the invoice run view. After correcting any data issues, the user can re-send — which regenerates the FINVOICE and retransmits.

**Invoice image retrieval failure**
> **PD-306:** *"If an invoice image cannot be retrieved, the system provides clear error messages and troubleshooting guidance."*

The endpoint returns an appropriate HTTP error (404 if the image is not found in the external system, 503 if the external system is unavailable). The user sees a clear message in the UI — not a generic error.

**Attachment retrieval failure**
> **PD-303:** *"If an attachment is missing or cannot be retrieved, the external system must return a structured error response. WasteHero ERP must be able to display an appropriate error message."*

WasteHero treats this as a non-blocking error — the invoice itself is displayed even if an attachment cannot be retrieved. The missing attachment is shown with an error indicator rather than causing the whole view to fail.

**Billing address sync failure**
> **PD-281:** *"When the billing address changes, it must be possible to update the new information via the interface to any open invoices that have already been transferred to the invoice broker."*

If the sync call to the external system fails, the new address is still saved in WasteHero (WasteHero is the master). The failed sync is logged and retried. The user is notified that the external system update is pending. This is important — the billing address in WasteHero is authoritative; the external system must eventually reflect it.

**Operator message processing failure**
If a message from the invoice operator cannot be matched to a customer record, it is logged with the raw message content and flagged for manual review. It is not discarded. An unmatched message could mean a customer number mismatch or a new customer not yet in WasteHero — a human needs to investigate.
