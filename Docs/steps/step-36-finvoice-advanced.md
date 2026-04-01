# Step 36 — FINVOICE Advanced: Multi-Account Splits, Mixed VAT, Reverse Charge

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → "Single invoice line split across different ledger accounts (PD-295)", "Multiple VAT rates on one invoice (PD-310)", "Reverse charge VAT — buyer's VAT number + legal reference (PD-300)"
- `Docs/structured_breakdown/01-domain-model.md` → Invoice (reverseChargeVat boolean), Product (reverseChargeVat boolean)
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 9: "Event Date Drives VAT Rate"

---

## Goal
Extend `FinvoiceBuilderService` from step 35 with three advanced capabilities: (1) multi-account splits where one customer-visible line generates multiple `<InvoiceRow>` ledger entries for different accounting accounts; (2) mixed VAT rates on the same invoice (e.g. 24% for standard services, 0% for VAT-exempt items), each with its own VAT summary block; (3) reverse charge VAT handling with the buyer's VAT number and Finnish legal reference text in the FINVOICE. All outputs are tested against the FINVOICE XSD validator.

---

## Backend

### 36.1 Multi-Account Splits

> **Requirement source:** `05-integration-layer.md` — "A single invoice line visible to the customer may be split in the FINVOICE data across different ledger accounts for accounting purposes (PD-295)"

**Design:** `InvoiceLineItem` can have multiple `AccountingLedgerEntry` sub-records, one per ledger account that the line amount is split across. The `FinvoiceBuilderService` generates one `<InvoiceRow>` per `AccountingLedgerEntry` — not one per `InvoiceLineItem`.

**AccountingLedgerEntry** (embedded value type, not a top-level entity):
```java
@Embeddable
public class AccountingLedgerEntry {
    private String ledgerCode;              // e.g. "3200" — transport revenue
    private String ledgerDescription;       // e.g. "Transport revenue"
    private BigDecimal amount;              // the portion of the line for this account
    private BigDecimal vatAmount;
}
```

`InvoiceLineItem` holds a `@ElementCollection` of `AccountingLedgerEntry`:
```java
@ElementCollection
@CollectionTable(name = "invoice_line_ledger_entries",
    joinColumns = @JoinColumn(name = "line_item_id"))
private List<AccountingLedgerEntry> ledgerEntries = new ArrayList<>();
```

**FinvoiceBuilderService — multi-account invoice row generation:**
```java
private void buildInvoiceRows(Invoice invoice, Element parent) {
    int rowIndex = 1;
    for (InvoiceLineItem line : invoice.getLineItems()) {
        if (!line.getLedgerEntries().isEmpty()) {
            // Multiple rows — one per ledger account split (accounting view)
            for (AccountingLedgerEntry entry : line.getLedgerEntries()) {
                Element row = buildInvoiceRow(line, entry, rowIndex++);
                parent.appendChild(row);
            }
        } else {
            // Single row — standard case
            Element row = buildInvoiceRowSingle(line, rowIndex++);
            parent.appendChild(row);
        }
    }
}
```

Each split row carries the sub-amount for that ledger account; the `<RowAmountWithoutVat>` = the entry's `amount`. The customer-visible total is the sum of all split rows for the same line.

---

### 36.2 Mixed VAT Rates

> **Requirement source:** `05-integration-layer.md` — "FINVOICE 3.0 supports decimal percentages. Must handle correctly — rounding errors in VAT calculation will cause the accounting to not balance."

**Algorithm:**
1. Group line items by `vatRate` value (using `BigDecimal.compareTo` — not `.equals()` which is scale-sensitive).
2. For each unique rate group: compute `vatBaseAmount` (sum of `netAmount`) and `vatRateAmount` (sum of `vatAmount`).
3. Generate one `<VatSpecificationDetails>` block per group.

**Finnish VAT rate examples handled:**
- `24.00` — standard rate (pre-2024)
- `25.50` — standard rate (2024 onwards; note the decimal)
- `14.00` — reduced rate for food/restaurant
- `10.00` — reduced rate for cultural services
- `0.00` — zero rated (e.g. reverse charge, export)

**VAT calculation guard — no floating-point:**
```java
BigDecimal vatAmount = netAmount
    .multiply(vatRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
    .setScale(4, RoundingMode.HALF_UP);
```

**Validation after grouping:** sum of all `<VatSpecificationDetails>/<VatBaseAmount>` must equal `<InvoiceTotalVatExcludedAmount>`. If not equal (due to rounding), add the rounding difference to the largest VAT group. This is the standard Finnish practice.

---

### 36.3 Reverse Charge VAT

> **Requirement source:** `01-domain-model.md` — Invoice (reverseChargeVat boolean), Product (reverseChargeVat boolean)
> **Requirement source:** `05-integration-layer.md` — "The invoice includes required legal references, the buyer's VAT number, and a clear indication that reverse charge VAT applies. FINVOICE 3.0 specification defines exactly where these appear."

**When `invoice.reverseChargeVat = true`:**

1. All line items for reverse-charge products must have `vatRate = 0.00` in the FINVOICE (the buyer accounts for the VAT, not the seller).
2. Add the buyer's VAT number to `<BuyerPartyDetails>`:
```xml
<BuyerPartyIdentifier IdentifierType="VATNumber">FI98765432</BuyerPartyIdentifier>
```
3. Add the legal reference text to `<InvoiceDetails>` (Finnish VAT Act §8c reference):
```xml
<InvoiceFreeText>Käännetty verovelvollisuus / Omvänd skattskyldighet (AVL 8c §)</InvoiceFreeText>
```
4. The `<VatSpecificationDetails>` block for reverse-charge lines must show `<VatRatePercent>0.00</VatRatePercent>` and a note:
```xml
<VatCode>RC</VatCode>
```

**Validation:** if `invoice.reverseChargeVat = true` but `customer.billingProfile.vatNumber` is null → throw `FinvoiceValidationException("Reverse charge requires buyer VAT number")`. This is blocking.

---

### 36.4 Updated FinvoiceBuilderService Method Signature

```java
@Service
public class FinvoiceBuilderService {

    // Main entry — handles all three advanced cases
    public String build(Invoice invoice, SellerDetails seller);

    // Internal helpers (package-private for testing):
    String buildInvoiceRowXml(InvoiceLineItem line, AccountingLedgerEntry entry, int rowIndex);
    String buildVatSummaryXml(Map<BigDecimal, VatGroup> vatGroups);
    String buildReverseChargeSection(Invoice invoice, BillingProfile buyerProfile);
    void validateReverseChargePrerequisites(Invoice invoice);
    Map<BigDecimal, VatGroup> groupLinesByVatRate(List<InvoiceLineItem> lines);
}
```

**VatGroup** (internal value type):
```java
class VatGroup {
    BigDecimal vatRate;
    BigDecimal baseAmount;     // sum of net amounts at this rate
    BigDecimal vatAmount;      // sum of VAT amounts at this rate
}
```

---

## Frontend

No new FE components in this step. The advanced FINVOICE structures are transparent to the frontend — the invoice detail page already shows all line items and amounts. The FE does not render XML directly.

**Testing tool** (optional, dev mode only): add a button "Download FINVOICE XML" on the invoice detail page visible only in dev environment:
```js
export const downloadFinvoiceXml = (id) =>
  axios.get(`/api/v1/invoices/${id}/finvoice-xml`, { responseType: 'blob' })
    .then(res => {
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/xml' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `invoice-${id}.xml`
      a.click()
    })
```

---

## Verification Checklist

1. Generate a FINVOICE for an invoice with one line split across 3 ledger accounts (transport, treatment, VAT recovery) — verify 3 `<InvoiceRow>` elements are produced with amounts summing to the original line amount.
2. Generate a FINVOICE with 2 lines at 24% VAT and 1 line at 0% VAT — verify exactly 2 `<VatSpecificationDetails>` blocks (one per unique rate).
3. Verify VAT arithmetic: for lines at 25.50%, compute VAT using `BigDecimal` division — check against expected values manually.
4. Sum of `<VatSpecificationDetails>/<VatBaseAmount>` must equal `<InvoiceTotalVatExcludedAmount>`. Run the rounding adjustment if needed.
5. Generate a FINVOICE for a reverse-charge invoice — verify `<VatRatePercent>0.00</VatRatePercent>` for reverse-charge lines, buyer VAT number present, legal reference text present.
6. Attempt to generate a reverse-charge FINVOICE for a customer without a VAT number → `FinvoiceValidationException` thrown before XML is built.
7. Validate all three test XMLs against the official FINVOICE 3.0 XSD — no violations in any case.
8. Run the single-account path (no ledger split) to confirm it still works after the multi-account refactor.
9. Mixed-rate VAT group: `BigDecimal.compareTo` used for grouping — verify `24.00` and `24.000` are treated as the same rate group.
10. Invoice with both reverse-charge and non-reverse-charge lines: reverse-charge lines at 0%, other lines at their normal rate — both appear in the VAT summary.

---

## File Checklist

### Backend
- [ ] `finvoice/FinvoiceBuilderService.java` — extend from step 35
- [ ] `invoice/AccountingLedgerEntry.java` (Embeddable)
- [ ] `invoice/InvoiceLineItem.java` — add `ledgerEntries` collection (extend from step 29)
- [ ] `finvoice/VatGroup.java` (internal value type)
- [ ] `finvoice/FinvoiceValidationException.java` — extend to include reverse-charge error messages

### Frontend
- [ ] `src/pages/invoices/InvoiceDetailPage.jsx` — add dev-mode FINVOICE download button (optional)
