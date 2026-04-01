# Step 20 — AccountingAllocationService

## References to Original Requirements
- `Docs/structured_breakdown/03-business-logic.md` → Area 6: Accounting and Financial Allocation — "AccountingAllocationService: Given a BillingEvent, finds the most specific AccountingAllocationRule (product + region + price list). Uses that rule to split the event's total amount into ledger-account-level sub-entries."
- `Docs/structured_breakdown/02-data-layer.md` → Section: AccountingAllocationRuleRepository — "Query … ORDER BY specificity DESC. Most-specific rule wins."
- `Docs/structured_breakdown/01-domain-model.md` → Section: InvoiceLineItem — "PD-295: The same product must be able to be routed to a different account and cost center based on e.g. price list or region."
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data — no new endpoints; this service is consumed internally by `InvoiceGenerationService`

---

## Goal

`AccountingAllocationService` is a pure internal service — it has no HTTP endpoints of its own. It takes a `BillingEvent` and returns one or more `LedgerEntry` objects that describe how the event's financial amounts must be split across different `AccountingAccount`s in the FINVOICE output.

The need for splitting arises from PD-295: on a single container emptying event, the waste treatment fee posts to account 4001 and the transport fee posts to account 4002. From the customer's perspective, it is one line item. From the accounting system's perspective, it is two ledger entries. This service performs that split.

The service delegates the rule lookup to `AccountingAllocationRuleRepository` (built in Step 19) and relies on the `specificityScore` ordering to guarantee that the most precise rule always wins without any application-level sorting logic.

The FE test panel (added to the Allocation Rules page in Step 19) calls the `/api/v1/allocation-rules/resolve` endpoint which internally exercises this service's rule-resolution path.

---

## Backend

### 20.1 LedgerEntry Value Object

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/LedgerEntry.java`

This is not a JPA entity — it is an in-memory value object assembled during invoice generation and written into the FINVOICE XML. It is never persisted to its own table.

| Field | Java Type | Notes |
|---|---|---|
| `billingEventId` | `Long` | The event this ledger entry was derived from |
| `accountingAccount` | `AccountingAccount` | The target ledger account resolved by the allocation rule |
| `amountNet` | `BigDecimal` | Net amount (VAT-excluded) in euros, scale 2 |
| `amountVat` | `BigDecimal` | VAT component in euros, scale 2 |
| `amountGross` | `BigDecimal` | `amountNet + amountVat` |
| `vatRate` | `BigDecimal` | The VAT percentage that produced `amountVat` |
| `priceComponent` | `PriceComponent` | Enum: `WASTE_FEE`, `TRANSPORT_FEE`, `ECO_FEE`, `SURCHARGE`, `ADJUSTMENT` |
| `description` | `String` | Human-readable label for this ledger line |
| `matchedRuleId` | `Long` | ID of the `AccountingAllocationRule` that was applied — for audit |

```java
@Value
@Builder
public class LedgerEntry {
    Long billingEventId;
    AccountingAccount accountingAccount;
    BigDecimal amountNet;
    BigDecimal amountVat;
    BigDecimal amountGross;
    BigDecimal vatRate;
    PriceComponent priceComponent;
    String description;
    Long matchedRuleId;
}
```

```java
public enum PriceComponent {
    WASTE_FEE,
    TRANSPORT_FEE,
    ECO_FEE,
    SURCHARGE,
    ADJUSTMENT
}
```

---

### 20.2 AccountingAllocationService

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/AccountingAllocationService.java`

> **Requirement source:** `03-business-logic.md` Area 6 — "Given a BillingEvent, finds the most specific AccountingAllocationRule … Uses that rule to split the event's total amount into ledger-account-level sub-entries. This split is what makes the FINVOICE accounting rows differ from the customer-visible invoice rows — one customer line may become three ledger entries."

| Method Signature | Description |
|---|---|
| `List<LedgerEntry> resolveEntries(BillingEvent event)` | Main entry point. Calls `splitByPriceComponent(event)` and resolves an `AccountingAccount` for each component using the allocation rules. Returns one `LedgerEntry` per applicable price component. |
| `AccountingAllocationRule findBestRule(Long productId, String region, String municipality)` | Calls `AllocationRuleRepository.findMostSpecificRules()`, returns the first result. Throws `AllocationRuleNotFoundException` if no active rule matches — this blocks invoice generation until the admin adds a rule. |
| `List<LedgerEntry> splitByPriceComponent(BillingEvent event)` | Decomposes the event into its price components. A container emptying event with `wastePrice > 0` and `transportPrice > 0` produces two components. An event with `ecoFee > 0` produces a third. |
| `BigDecimal computeVatAmount(BigDecimal netAmount, BigDecimal vatRatePercent)` | `netAmount.multiply(vatRatePercent).divide(BigDecimal.valueOf(100), 2, HALF_UP)` |

**Resolution logic for `resolveEntries`:**

```
For each active price component on the event:
  1. Call findBestRule(event.product.id, event.municipality, event.municipality)
  2. If no rule found → throw AllocationRuleNotFoundException with message including productId and region
  3. Build LedgerEntry with:
     - accountingAccount from the matched rule
     - amountNet = component amount (wastePrice / transportPrice / ecoFee)
     - vatRate from the event (already resolved by event date in BillingEventService)
     - amountVat = computeVatAmount(amountNet, vatRate)
     - amountGross = amountNet + amountVat
     - priceComponent = the component type
     - matchedRuleId = matched rule's ID
```

**Price component extraction from `BillingEvent`:**

```java
private List<ComponentAmount> splitByPriceComponent(BillingEvent event) {
    List<ComponentAmount> components = new ArrayList<>();
    if (event.getWastePrice() != null && event.getWastePrice().compareTo(BigDecimal.ZERO) > 0) {
        components.add(new ComponentAmount(WASTE_FEE, event.getWastePrice()));
    }
    if (event.getTransportPrice() != null && event.getTransportPrice().compareTo(BigDecimal.ZERO) > 0) {
        components.add(new ComponentAmount(TRANSPORT_FEE, event.getTransportPrice()));
    }
    if (event.getEcoFee() != null && event.getEcoFee().compareTo(BigDecimal.ZERO) > 0) {
        components.add(new ComponentAmount(ECO_FEE, event.getEcoFee()));
    }
    return components;
}
```

---

### 20.3 AllocationRuleNotFoundException

**File:** `invoicing/src/main/java/com/example/invoicing/accounting/AllocationRuleNotFoundException.java`

```java
public class AllocationRuleNotFoundException extends RuntimeException {
    public AllocationRuleNotFoundException(Long productId, String region) {
        super("No active allocation rule found for productId=" + productId
              + ", region=" + region
              + ". Add a rule at /api/v1/allocation-rules before running invoices.");
    }
}
```

This exception bubbles up through `InvoiceGenerationService` and is caught by the validation engine, adding a BLOCKING failure to the invoice's `ValidationReport`. The invoice run continues for other customers.

---

### 20.4 Integration with InvoiceGenerationService

**File:** `invoicing/src/main/java/com/example/invoicing/invoice/InvoiceGenerationService.java` (modification)

`AccountingAllocationService` is injected into `InvoiceGenerationService` and called after bundling rules are applied (Step 6 of the generation pipeline from `03-business-logic.md`):

```java
// Inside InvoiceGenerationService.generateForCustomer(...)
List<LedgerEntry> ledgerEntries = events.stream()
    .flatMap(event -> accountingAllocationService.resolveEntries(event).stream())
    .collect(toList());
// ledgerEntries are attached to the Invoice for use by FinvoiceBuilderService
invoice.setLedgerEntries(ledgerEntries);
```

No new REST endpoints are added in this step.

---

## Frontend

### 20.5 Allocation Test Panel Enhancement

The Resolve Test Panel built in Step 19 (`ResolveTestPanel.jsx`) already covers the visual testing of the rule resolution. In this step, extend it to also show the **full split** — i.e., which price components would be allocated to which accounts.

**Extended panel response display** (rendered in `ResolveTestPanel.jsx`):

When the user clicks "Test":
1. Call `GET /api/v1/allocation-rules/resolve?productId=&region=&municipality=`
2. Display a table with columns: Price Component, Account Code, Account Name, Specificity Score, Matched Rule ID

This gives the admin a clear view of "if I process an event for this product in this region, here is where each price component will land in the accounting system."

**No new API file or page is created** — the enhancement is entirely within the existing `ResolveTestPanel.jsx` and the existing `resolveAllocationRule` API call from `allocationRules.js`.

---

## Verification Checklist

1. With at least two allocation rules for the same product (one with region null, one with region "Uusimaa"), call `accountingAllocationService.findBestRule(productId, "Uusimaa", null)` — confirm the region-specific rule (score 2) is returned, not the fallback (score 1).
2. Create a `BillingEvent` with `wastePrice=50.00`, `transportPrice=20.00`, `ecoFee=0` — `resolveEntries()` returns exactly 2 `LedgerEntry` objects, one for `WASTE_FEE` and one for `TRANSPORT_FEE`.
3. Create a `BillingEvent` with `wastePrice=50.00`, `transportPrice=20.00`, `ecoFee=5.00` — `resolveEntries()` returns 3 `LedgerEntry` objects.
4. Verify `LedgerEntry.amountGross = amountNet + amountVat` for each entry — cross-check: `50.00 * 0.24 = 12.00 VAT → gross = 62.00`.
5. Delete all allocation rules for a product. Call `resolveEntries()` for an event with that product — confirm `AllocationRuleNotFoundException` is thrown with a message containing `productId` and `region`.
6. When `AllocationRuleNotFoundException` is thrown during an invoice run, confirm the affected invoice is moved to ERROR state and the error message appears in the run's `ValidationReport`, but other customers' invoices in the same run are not affected.
7. Open the Resolve Test Panel in the FE — confirm the extended table shows Price Component, Account Code, and Account Name columns.
8. Run a full invoice generation for a customer with a multi-component product — inspect the generated invoice entity and confirm `ledgerEntries` contains the correct split entries with `matchedRuleId` populated.

---

## File Checklist

### Backend
- [ ] `accounting/LedgerEntry.java` (value object)
- [ ] `accounting/PriceComponent.java` (enum)
- [ ] `accounting/AccountingAllocationService.java`
- [ ] `accounting/AllocationRuleNotFoundException.java`
- [ ] `invoice/InvoiceGenerationService.java` (modification — inject and call `AccountingAllocationService`)

### Frontend
- [ ] `src/components/allocation/ResolveTestPanel.jsx` (enhancement — show price component split table)
