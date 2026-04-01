# Step 22 — VatCalculationService

## References to Original Requirements
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 9: "Event Date Drives VAT Rate — Not Today's Date" — "If an event happened in January when VAT was 24%, and the invoice is generated in March when VAT has changed to 25.5%, the January VAT rate (24%) must apply."
- `Docs/structured_breakdown/01-domain-model.md` → Section: Product — "reverseChargeVat boolean: When true, the entire VAT logic on the invoice line changes."
- `Docs/structured_breakdown/01-domain-model.md` → Section: Invoice — "PD-300: The system automatically applies reverse charge VAT when a product or service marked as subject to reverse VAT is added to an invoice."
- `Docs/structured_breakdown/03-business-logic.md` → Area 2: Invoice Generation — "InvoiceGenerationService step 8: Applies reverse charge VAT where products require it (PD-300)"
- `Docs/structured_breakdown/02-data-layer.md` → Section: VatRateRepository — "Query: productId = ? AND validFrom <= eventDate AND (validTo IS NULL OR validTo >= eventDate)"

---

## Goal

`VatCalculationService` is a focused internal service responsible for two things:

1. **Rate resolution** — given a `BillingEvent`, find the correct `VatRate` using the event's own date (not today). This enforces cross-cutting rule 9 from `06-cross-cutting.md`: an event that occurred in January is always taxed at the January VAT rate, even if it is invoiced in March after a rate change.

2. **Reverse charge handling** — when a product has `reverseChargeVat = true` AND the customer is B2B, the effective VAT rate is forced to 0%. The buyer self-accounts for VAT. The invoice must carry the buyer's VAT registration number and the Finnish statutory reverse charge notice text.

The service returns a `VatCalculationResult` value object containing the resolved rate, the gross amount, the net amount, and the reverse charge flag. `InvoiceGenerationService` and `FinvoiceBuilderService` consume this result — they never compute VAT directly.

---

## Backend

### 22.1 VatCalculationResult Value Object

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/vat/VatCalculationResult.java`

| Field | Java Type | Notes |
|---|---|---|
| `billingEventId` | `Long` | The event this result applies to |
| `vatRate` | `VatRate` | The resolved `VatRate` entity (carries code, percentage, validity period) |
| `effectiveRatePercent` | `BigDecimal` | The actual percentage used — may be 0.00 if reverse charge applies |
| `reverseCharge` | `boolean` | `true` when reverse charge overrides the standard rate |
| `amountNet` | `BigDecimal` | The taxable base (sum of all price components), scale 2 |
| `amountVat` | `BigDecimal` | VAT amount = `amountNet × effectiveRatePercent / 100`, scale 2, `HALF_UP` |
| `amountGross` | `BigDecimal` | `amountNet + amountVat` |
| `buyerVatNumber` | `String` | Populated when `reverseCharge = true`; taken from `customer.businessId` |
| `reverseChargeNoticeText` | `String` | Statutory Finnish reverse charge text; populated when `reverseCharge = true` |

```java
@Value
@Builder
public class VatCalculationResult {
    Long billingEventId;
    VatRate vatRate;
    BigDecimal effectiveRatePercent;
    boolean reverseCharge;
    BigDecimal amountNet;
    BigDecimal amountVat;
    BigDecimal amountGross;
    String buyerVatNumber;
    String reverseChargeNoticeText;
}
```

---

### 22.2 VatCalculationService

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/vat/VatCalculationService.java`

> **Requirement source:** `06-cross-cutting.md` Rule 9 — "The rate is fixed at the time the event occurred — not at the time the invoice is generated."
> **Requirement source:** `01-domain-model.md` Product — `reverseChargeVat` boolean.
> **Requirement source:** `03-business-logic.md` Area 2 — "Applies reverse charge VAT where products require it."

| Method Signature | Description |
|---|---|
| `VatCalculationResult calculate(BillingEvent event)` | Main entry point. Resolves rate by event date, checks reverse charge flag, computes all amounts. |
| `VatRate resolveRateByEventDate(BillingEvent event)` | Calls `VatRateRepository.findByEventDate(event.getEventDate())`. If multiple rates are returned (should not happen if data is clean), uses the one with the highest `specificityScore` or throws if truly ambiguous. Throws `VatRateNotFoundException` if no rate applies to the event date. |
| `boolean isReverseChargeApplicable(BillingEvent event)` | Returns `true` when `event.getProduct().isReverseChargeVat() == true` AND the customer is B2B (i.e. `customer.getBillingProfile().getBusinessId()` is not null/blank). |
| `BigDecimal computeNetAmount(BillingEvent event)` | Sums all price components: `wastePrice + transportPrice + ecoFee`. All must use `BigDecimal.ZERO` as null-safe default. |
| `String buildReverseChargeNoticeText(BillingEvent event)` | Returns the statutory Finnish text. The standard text is: `"VAT 0 % (Käännetty verovelvollisuus / Omvänd skattskyldighet / Reverse charge, Article 196 VAT Directive)"`. This is a constant in the service. |

**Core calculation logic:**

```java
public VatCalculationResult calculate(BillingEvent event) {
    VatRate resolvedRate = resolveRateByEventDate(event);
    boolean reverseCharge = isReverseChargeApplicable(event);

    BigDecimal effectiveRate = reverseCharge
        ? BigDecimal.ZERO
        : resolvedRate.getRate();

    BigDecimal amountNet = computeNetAmount(event);
    BigDecimal amountVat = amountNet
        .multiply(effectiveRate)
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal amountGross = amountNet.add(amountVat);

    return VatCalculationResult.builder()
        .billingEventId(event.getId())
        .vatRate(resolvedRate)
        .effectiveRatePercent(effectiveRate)
        .reverseCharge(reverseCharge)
        .amountNet(amountNet)
        .amountVat(amountVat)
        .amountGross(amountGross)
        .buyerVatNumber(reverseCharge ? event.getCustomer().getBillingProfile().getBusinessId() : null)
        .reverseChargeNoticeText(reverseCharge ? buildReverseChargeNoticeText(event) : null)
        .build();
}
```

**Important:** `BigDecimal` arithmetic only. No `double` or `float` anywhere in this service. Division uses `scale=2, HALF_UP`. See `06-cross-cutting.md` Rule 8 for the precision requirement (applied here by analogy).

---

### 22.3 VatRateNotFoundException

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/vat/VatRateNotFoundException.java`

```java
public class VatRateNotFoundException extends RuntimeException {
    public VatRateNotFoundException(LocalDate eventDate) {
        super("No VAT rate configured for event date: " + eventDate
              + ". Add a VatRate with a validFrom <= " + eventDate
              + " at /api/v1/vat-rates before processing events from this date.");
    }
}
```

This exception is caught in `InvoiceGenerationService` and recorded as a BLOCKING failure in the `ValidationReport`. The affected invoice is not generated; other invoices in the same run continue.

---

### 22.4 BillingEvent Detail Response — VAT Fields

**File:** `invoicing/src/main/java/com/example/invoicing/billing/dto/BillingEventDetailResponse.java` (modification)

Add the following fields to the existing detail response DTO:

```java
private String resolvedVatRateCode;       // e.g. "VAT_24"
private BigDecimal resolvedVatRatePercent; // e.g. 24.00
private boolean reverseCharge;             // true/false
private BigDecimal calculatedAmountNet;
private BigDecimal calculatedAmountVat;
private BigDecimal calculatedAmountGross;
private String buyerVatNumber;             // null unless reverseCharge = true
```

**File:** `invoicing/src/main/java/com/example/invoicing/billing/BillingEventService.java` (modification)

In `getById(Long id)`, after fetching the event, call `vatCalculationService.calculate(event)` and populate the VAT fields on the response DTO:

```java
VatCalculationResult vatResult = vatCalculationService.calculate(event);
response.setResolvedVatRateCode(vatResult.getVatRate().getCode());
response.setResolvedVatRatePercent(vatResult.getEffectiveRatePercent());
response.setReverseCharge(vatResult.isReverseCharge());
response.setCalculatedAmountNet(vatResult.getAmountNet());
response.setCalculatedAmountVat(vatResult.getAmountVat());
response.setCalculatedAmountGross(vatResult.getAmountGross());
response.setBuyerVatNumber(vatResult.getBuyerVatNumber());
```

---

## Frontend

### 22.5 VAT Details on Billing Event Detail Page

**File:** `invoicing-fe/src/pages/billing/BillingEventDetailPage.jsx` (modification)

Add a "VAT Details" section to the billing event detail view with the following read-only fields:

| Label | Value source |
|---|---|
| VAT Rate Code | `event.resolvedVatRateCode` |
| VAT Rate % | `event.resolvedVatRatePercent` |
| Reverse Charge | Badge: green "No" or orange "Yes — Buyer accounts for VAT" |
| Net Amount | `event.calculatedAmountNet` formatted as EUR |
| VAT Amount | `event.calculatedAmountVat` formatted as EUR |
| Gross Amount | `event.calculatedAmountGross` formatted as EUR |
| Buyer VAT Number | `event.buyerVatNumber` — shown only when `reverseCharge = true` |

When `reverseCharge = true`, display an info box: "This product uses reverse charge VAT. The customer self-accounts for VAT. VAT rate shown on invoice will be 0%."

**No new API file or endpoint is needed** — all values come from the existing `GET /api/v1/billing-events/{id}` response, enhanced in section 22.4.

---

## Verification Checklist

1. Create a `VatRate` with `rate=24.00`, `validFrom=2024-01-01`, `validTo=null`.
2. Create a `BillingEvent` with `eventDate=2024-06-15`, `wastePrice=100.00`, `transportPrice=0`, `ecoFee=0`. Call `vatCalculationService.calculate(event)` — confirm `effectiveRatePercent=24.00`, `amountNet=100.00`, `amountVat=24.00`, `amountGross=124.00`.
3. Create a second `VatRate` with `rate=25.50`, `validFrom=2025-01-01`, `validTo=null`.
4. Create a `BillingEvent` with `eventDate=2024-12-01`. Call `calculate()` — confirm `effectiveRatePercent=24.00` (not 25.50), verifying event-date resolution.
5. Create a `BillingEvent` with `eventDate=2025-02-01`. Call `calculate()` — confirm `effectiveRatePercent=25.50`.
6. Set `product.reverseChargeVat=true`. Set customer `businessId="FI12345678"`. Create a `BillingEvent` for this product and customer. Call `calculate()` — confirm `reverseCharge=true`, `effectiveRatePercent=0.00`, `amountVat=0.00`, `amountGross=amountNet`, `buyerVatNumber="FI12345678"`.
7. Same product with `reverseChargeVat=true` but a consumer customer (no `businessId`). Call `calculate()` — confirm `reverseCharge=false`, standard VAT rate applies.
8. Create a `BillingEvent` with `eventDate=2010-01-01` (no VAT rate covers this date). Call `calculate()` — confirm `VatRateNotFoundException` is thrown with a message containing the event date.
9. `GET /api/v1/billing-events/{id}` — response includes `resolvedVatRateCode`, `resolvedVatRatePercent`, `calculatedAmountNet`, `calculatedAmountVat`, `calculatedAmountGross`.
10. Open Billing Event detail page in FE — confirm the "VAT Details" section renders all fields correctly, reverse charge badge appears when applicable.
11. Verify all arithmetic uses `BigDecimal` — no floating-point rounding errors (e.g. `33.33 * 24% = 7.99`, not `8.00` due to float truncation).

---

## File Checklist

### Backend
- [ ] `accounting/vat/VatCalculationResult.java` (value object)
- [ ] `accounting/vat/VatCalculationService.java`
- [ ] `accounting/vat/VatRateNotFoundException.java`
- [ ] `billing/dto/BillingEventDetailResponse.java` (modification — add VAT fields)
- [ ] `billing/BillingEventService.java` (modification — call `VatCalculationService.calculate()` in `getById()`)

### Frontend
- [ ] `src/pages/billing/BillingEventDetailPage.jsx` (modification — add VAT Details section)
