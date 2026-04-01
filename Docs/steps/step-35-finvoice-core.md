# Step 35 — FINVOICE Core Builder

## References to Original Requirements
- `Docs/structured_breakdown/05-integration-layer.md` → Section 1: External Invoicing System — FINVOICE 3.0 XML, FINVOICE specification requirements
- `Docs/structured_breakdown/03-business-logic.md` → Section: InvoiceGenerationService (step 9 — build FINVOICE XML)
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 9: "Event Date Drives VAT Rate — Not Today's Date"

---

## Goal
Implement `FinvoiceBuilderService`, which generates a valid FINVOICE 3.0 XML document from a set of `InvoiceLineItem` records and customer/seller data. The output must pass XSD validation against the official FINVOICE 3.0 schema. Seller details, buyer details in the correct language, per-line invoice rows with ledger codes, a VAT summary section grouped by rate, template code reference, and payment details are all included. This service is called internally — no new controller endpoint.

---

## Backend

### 35.1 FinvoiceBuilderService

**File:** `invoicing/src/main/java/com/example/invoicing/finvoice/FinvoiceBuilderService.java`

> **Requirement source:** `05-integration-layer.md` — "Seller party details, buyer party details (in customer language), invoice rows, VAT specification details, template code, language code, custom text, attachment references"

```java
@Service
public class FinvoiceBuilderService {

    /**
     * Build a FINVOICE 3.0 XML string from the invoice's line items and metadata.
     * @param invoice      The Invoice entity with all line items populated
     * @param seller       The waste company (WasteHero client) seller details
     * @throws FinvoiceValidationException if the generated XML fails XSD validation
     */
    public String build(Invoice invoice, SellerDetails seller) {
        // 1. Create XML document root <Finvoice Version="3.0">
        // 2. Populate <MessageTransmissionDetails>
        // 3. Populate <SellerPartyDetails> from seller config
        // 4. Populate <BuyerPartyDetails> from invoice.customer.billingProfile
        //    — use invoice.language to select correct address/name variant
        // 5. Populate <InvoiceDetails> (invoiceNumber, invoiceDate, dueDate, templateCode)
        // 6. Populate <InvoiceRow> for each line item
        // 7. Populate <VatSpecificationDetails> — grouped by rate
        // 8. Populate <InvoiceAmountDetails>
        // 9. If invoice.customText != null → <FreeTextDetails>
        // 10. Validate against FINVOICE 3.0 XSD
        // 11. Return XML string
    }

    private void validate(String xml) throws FinvoiceValidationException {
        // Load FINVOICE 3.0 XSD from classpath (resources/finvoice/Finvoice3.0.xsd)
        // Validate the XML string against it
        // Collect all schema violations and throw FinvoiceValidationException with full list
    }
}
```

---

### 35.2 FINVOICE XML Structure Details

**Seller section (`<SellerPartyDetails>`):**
```xml
<SellerPartyDetails>
  <SellerPartyIdentifier IdentifierType="VATNumber">FI12345678</SellerPartyIdentifier>
  <SellerOrganisationName>Waste Company Oy</SellerOrganisationName>
  <SellerStreetName>Jätetie 1</SellerStreetName>
  <SellerTownName>Helsinki</SellerTownName>
  <SellerPostCodeIdentifier>00100</SellerPostCodeIdentifier>
  <CountryCode>FI</CountryCode>
</SellerPartyDetails>
```

**Buyer section (`<BuyerPartyDetails>`)** — language-aware:
- When `invoice.language = FI` → use Finnish billing address fields from `BillingProfile`.
- When `invoice.language = SV` → use Swedish address variant.
- When `invoice.language = EN` → use English address variant (or fall back to FI if not defined).

```xml
<BuyerPartyDetails>
  <BuyerPartyIdentifier IdentifierType="CustomerNumber">123456</BuyerPartyIdentifier>
  <BuyerOrganisationName>Virtanen Oy</BuyerOrganisationName>
  <BuyerStreetName>Mäkitie 5</BuyerStreetName>
  <BuyerTownName>Tampere</BuyerTownName>
  <BuyerPostCodeIdentifier>33100</BuyerPostCodeIdentifier>
  <CountryCode>FI</CountryCode>
</BuyerPartyDetails>
```

**Invoice rows (`<InvoiceRow>`)** — one per `InvoiceLineItem`:
```xml
<InvoiceRow>
  <ArticleIdentifier>WASTE-001</ArticleIdentifier>
  <ArticleName>Waste container emptying</ArticleName>
  <DeliveredQuantity QuantityUnitCode="pcs">4</DeliveredQuantity>
  <UnitPriceAmount AmountCurrencyIdentifier="EUR">20.00</UnitPriceAmount>
  <RowVatRatePercent>24.00</RowVatRatePercent>
  <RowVatAmount AmountCurrencyIdentifier="EUR">19.20</RowVatAmount>
  <RowAmountWithoutVat AmountCurrencyIdentifier="EUR">80.00</RowAmountWithoutVat>
  <RowAmountWithVat AmountCurrencyIdentifier="EUR">99.20</RowAmountWithVat>
  <RowIdentifier>1</RowIdentifier>
  <!-- Accounting ledger code — used by the external accounting system -->
  <RowIdentifier IdentifierType="LedgerCode">3200</RowIdentifier>
</InvoiceRow>
```

**VAT summary section (`<VatSpecificationDetails>`)** — one block per unique VAT rate:
```xml
<VatSpecificationDetails>
  <VatBaseAmount AmountCurrencyIdentifier="EUR">80.00</VatBaseAmount>
  <VatRatePercent>24.00</VatRatePercent>
  <VatRateAmount AmountCurrencyIdentifier="EUR">19.20</VatRateAmount>
</VatSpecificationDetails>
<VatSpecificationDetails>
  <VatBaseAmount AmountCurrencyIdentifier="EUR">40.00</VatBaseAmount>
  <VatRatePercent>0.00</VatRatePercent>
  <VatRateAmount AmountCurrencyIdentifier="EUR">0.00</VatRateAmount>
</VatSpecificationDetails>
```

**Invoice amount details:**
```xml
<InvoiceAmountDetails>
  <InvoiceTotalVatExcludedAmount AmountCurrencyIdentifier="EUR">120.00</InvoiceTotalVatExcludedAmount>
  <InvoiceTotalVatAmount AmountCurrencyIdentifier="EUR">28.80</InvoiceTotalVatAmount>
  <InvoiceTotalVatIncludedAmount AmountCurrencyIdentifier="EUR">148.80</InvoiceTotalVatIncludedAmount>
</InvoiceAmountDetails>
```

**Template code reference:**
```xml
<InvoiceDetails>
  <InvoiceTypeText>INVOICE</InvoiceTypeText>
  <InvoiceTypeCode>INV01</InvoiceTypeCode>
  <!-- Template code sent to external system for invoice layout selection (PD-307) -->
  <InvoiceTypeCode IdentifierType="TemplateCode">WASTE_STANDARD</InvoiceTypeCode>
  <InvoiceNumber>PL2024000042</InvoiceNumber>
  <InvoiceDate Format="CCYYMMDD">20240301</InvoiceDate>
  <InvoiceDueDate Format="CCYYMMDD">20240315</InvoiceDueDate>
</InvoiceDetails>
```

---

### 35.3 SellerDetails Configuration

**File:** `invoicing/src/main/java/com/example/invoicing/finvoice/SellerDetails.java`

Populated from application configuration (not from a database entity — seller details are per-installation):

```java
@ConfigurationProperties(prefix = "app.seller")
@Component
public class SellerDetails {
    private String vatNumber;
    private String organisationName;
    private String streetName;
    private String townName;
    private String postCode;
    private String countryCode;
    private String bankAccountIban;
    private String bicCode;
}
```

**`application.yml` example:**
```yaml
app:
  seller:
    vat-number: FI12345678
    organisation-name: "Waste Company Oy"
    street-name: "Jätetie 1"
    town-name: Helsinki
    post-code: "00100"
    country-code: FI
    bank-account-iban: "FI1234567890123456"
    bic-code: NDEAFIHH
```

---

### 35.4 XSD Validation

**File:** `invoicing/src/main/resources/finvoice/Finvoice3.0.xsd`

Place the official FINVOICE 3.0 XSD file (downloadable from finanssiala.fi) in the classpath. Validate every generated XML before returning it from `FinvoiceBuilderService.build()`.

```java
private Schema loadFinvoiceSchema() {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    InputStream xsd = getClass().getResourceAsStream("/finvoice/Finvoice3.0.xsd");
    return factory.newSchema(new StreamSource(xsd));
}
```

**`FinvoiceValidationException`:**
```java
public class FinvoiceValidationException extends RuntimeException {
    private final List<String> schemaViolations;
}
```

---

## Frontend

No new FE components in this step. `FinvoiceBuilderService` is internal to the BE generation pipeline. The FE already sees the invoice through the invoice detail page (step 29) and the preview (step 34).

**Optional display enhancement:** add a "View FINVOICE XML" link on the invoice detail page (INVOICING_USER only) that calls a new endpoint:

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/v1/invoices/{id}/finvoice-xml` | Returns the raw FINVOICE XML for inspection | INVOICING_USER |

Response: `Content-Type: application/xml`, body = stored FINVOICE XML string.

**API call:**
```js
export const getFinvoiceXml = (id) =>
  axios.get(`/api/v1/invoices/${id}/finvoice-xml`, { responseType: 'text' })
```

---

## Verification Checklist

1. Generate a FINVOICE XML for a test invoice with 2 line items at different VAT rates. Validate against the official FINVOICE 3.0 XSD — no violations.
2. Verify `<BuyerPartyDetails>` uses the customer's FI address when `invoice.language = FI`, and the SV address when `invoice.language = SV`.
3. Verify the VAT summary section contains one `<VatSpecificationDetails>` block per unique VAT rate, not one per line.
4. Verify the sum of all `<VatSpecificationDetails>/<VatBaseAmount>` values equals `<InvoiceTotalVatExcludedAmount>`.
5. Verify the sum of all `<VatSpecificationDetails>/<VatRateAmount>` values equals `<InvoiceTotalVatAmount>`.
6. Verify `<InvoiceTypeCode IdentifierType="TemplateCode">` contains the correct value from `invoice.templateCode`.
7. Attempt to build a FINVOICE with a missing required field (e.g. null invoice number) → `FinvoiceValidationException` thrown with schema violation details.
8. Retrieve the stored XML via `GET /api/v1/invoices/{id}/finvoice-xml` — returns valid XML.
9. `SellerDetails` loads from `application.yml` configuration — verify correct VAT number and bank details appear in generated XML.
10. VAT amounts use `BigDecimal` arithmetic and match the values on the invoice line items (no floating-point drift).

---

## File Checklist

### Backend
- [ ] `finvoice/FinvoiceBuilderService.java`
- [ ] `finvoice/FinvoiceValidationException.java`
- [ ] `finvoice/SellerDetails.java`
- [ ] `finvoice/FinvoiceXmlController.java` (optional inspection endpoint)
- [ ] `src/main/resources/finvoice/Finvoice3.0.xsd`

### Frontend
- [ ] `src/api/invoices.js` — add `getFinvoiceXml(id)` (optional)
