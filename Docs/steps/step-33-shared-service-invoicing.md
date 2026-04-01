# Step 33 — Shared Service Invoicing Service

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: SharedServiceParticipant, InvoiceLineItem
- `Docs/structured_breakdown/03-business-logic.md` → Section: SharedServiceInvoicingService
- `Docs/structured_breakdown/06-cross-cutting.md` → Rule 8: "The Shared Service 100% Rule", Rule 3: "Simulation Mode Guard"

---

## Goal
Implement `SharedServiceInvoicingService`, which takes a shared-service `BillingEvent` and generates one `InvoiceLineItem` per active participant. Each participant's line amount equals `totalEventAmount × (participantSharePercentage / 100)`. The 100% rule is checked before any line item is generated. Retroactive participant addition triggers redistribution across historical events. This service is called internally by `InvoiceGenerationService`; no new controller endpoint is introduced.

---

## Backend

### 33.1 SharedServiceInvoicingService

**File:** `invoicing/src/main/java/com/example/invoicing/sharedservice/SharedServiceInvoicingService.java`

> **Requirement source:** `03-business-logic.md` — SharedServiceInvoicingService
> **Requirement source:** `06-cross-cutting.md` — Rule 8: exact BigDecimal arithmetic

**Core distribution algorithm:**

```java
@Service
@Transactional
public class SharedServiceInvoicingService {

    /**
     * Generate one InvoiceLineItem per participant active on the event date.
     * Throws SharedServicePercentageException if shares do not sum to 100.00%.
     */
    public List<InvoiceLineItem> distributeEvent(BillingEvent event) {
        Long groupId = event.getSharedServiceGroupId();

        // 1. Validate 100% rule BEFORE generating any lines
        validationService.validateTotalEquals100(groupId);

        // 2. Find participants active on the event date (not today)
        List<SharedServiceParticipant> participants =
            participantRepository.findActiveAtDate(groupId, event.getEventDate());

        if (participants.isEmpty()) {
            throw new SharedServiceConfigException(
                "No active participants found for group " + groupId
                + " on date " + event.getEventDate());
        }

        // 3. Generate one line per participant
        List<InvoiceLineItem> lines = new ArrayList<>();
        BigDecimal totalNet = event.getNetAmount().setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalGross = event.getGrossAmount().setScale(4, RoundingMode.HALF_UP);

        for (SharedServiceParticipant participant : participants) {
            BigDecimal share = participant.getSharePercentage()
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

            BigDecimal lineNet = totalNet.multiply(share)
                .setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineGross = totalGross.multiply(share)
                .setScale(4, RoundingMode.HALF_UP);

            InvoiceLineItem line = new InvoiceLineItem();
            line.setProduct(event.getProduct());
            // Description includes share percentage for transparency to customer
            line.setDescription(event.getProduct().getName()
                + " (" + participant.getSharePercentage() + "% share)");
            line.setQuantity(event.getQuantity().multiply(share)
                .setScale(4, RoundingMode.HALF_UP));
            line.setUnitPrice(event.getUnitPrice());
            line.setVatRate(event.getVatRate());
            line.setNetAmount(lineNet);
            line.setGrossAmount(lineGross);
            line.setLegalClassification(event.getLegalClassification());
            line.setAccountingAccount(event.getAccountingAccount());
            line.setCostCenter(event.getCostCenter());
            line.setBundled(false);
            lines.add(line);
        }
        return lines;
    }

    /**
     * Retroactive redistribution — called when a new participant is added with validFrom in the past.
     * Finds all IN_PROGRESS events for the group from validFrom onwards and re-runs distribution.
     */
    public void redistributeRetroactive(Long groupId, LocalDate validFrom) {
        // 1. Validate the new 100% total (caller has already added the new participant)
        validationService.validateTotalEquals100(groupId);

        // 2. Find all IN_PROGRESS billing events for this group from validFrom
        List<BillingEvent> events = billingEventRepository
            .findBySharedServiceGroupIdAndStatusAndEventDateGreaterThanEqual(
                groupId, BillingEventStatus.IN_PROGRESS, validFrom);

        // 3. For each event, delete existing participant line items and regenerate
        for (BillingEvent event : events) {
            // Remove old shared-service lines for this event (if on a draft invoice)
            lineItemRepository.deleteBySourceBillingEventIdAndBundled(event.getId(), false);
            // Re-distribute with new participant set
            List<InvoiceLineItem> newLines = distributeEvent(event);
            lineItemRepository.saveAll(newLines);
        }
    }
}
```

**Key arithmetic rules enforced:**
- All `BigDecimal` operations use `RoundingMode.HALF_UP`.
- `sharePercentage` divided by 100 uses scale 10 internally before multiplying amounts, then rounded to scale 4 for storage.
- The sum of all participant `netAmount` values equals the event's `netAmount` (verified by test — rounding differences absorbed in the last participant).

---

### 33.2 Rounding Remainder Handling

When distributing amounts across N participants, rounding can cause the sum of individual lines to differ from the total by 1 cent. Fix: after calculating lines for N-1 participants, compute the last participant's amount as `total - sum(otherLines)`.

```java
// Last participant gets the remainder to guarantee exact total
if (isLastParticipant) {
    BigDecimal allocated = lines.stream()
        .map(InvoiceLineItem::getNetAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    lineNet = totalNet.subtract(allocated);
    // Same for grossAmount
}
```

---

### 33.3 Integration into InvoiceGenerationService

`InvoiceGenerationService` (step 34) calls `SharedServiceInvoicingService.distributeEvent()` for every `BillingEvent` that has a non-null `sharedServiceGroupId`. The returned `List<InvoiceLineItem>` replaces what would have been a single line item from the bundling step.

**No new controller endpoint is created in this step.** The service is internal only.

---

## Frontend

### 33.4 Shared Service Line Display

The invoice detail view (`InvoiceLineItemsTable` from step 29) already handles multiple line items. For shared service lines, the `description` field includes the percentage suffix `(25.00% share)`, making it self-explanatory to the billing user.

No additional component is required specifically for this step. The existing table renders shared service lines correctly.

**Tooltip enhancement** (optional, add to `InvoiceLineItemsTable.jsx`): when a line item's description contains `% share`, show a tooltip "This line represents one participant's share of a shared service arrangement."

---

## Verification Checklist

1. Create a property group with 4 participants at 25% each; create a shared-service `BillingEvent` with net = €100.00.
2. Call `SharedServiceInvoicingService.distributeEvent()` → 4 line items, each with `netAmount = 25.0000`.
3. Verify the sum of all 4 `netAmount` values equals the event's `netAmount` exactly (€100.00).
4. Test rounding: create an event with net = €10.00 split 3 ways (33.33%, 33.33%, 33.34%) — sum must still equal €10.00. Last participant absorbs remainder.
5. Attempt distribution with shares summing to 99.00% → `SharedServicePercentageException` is thrown before any lines are generated.
6. Call `redistributeRetroactive(groupId, validFrom)` after adding a new participant → all IN_PROGRESS events from `validFrom` onward are redistributed; SENT/COMPLETED events are untouched.
7. Retroactive redistribution: verify that old participant lines (from draft invoices) are removed and new lines are created.
8. Verify `BigDecimal` scale: all `netAmount` values stored at scale 4 (e.g. `25.0000`, not `25`).
9. In invoice preview (step 34), shared service lines appear with customer-visible percentage descriptions.
10. Events with `sharedServiceGroupId = null` pass through `InvoiceGenerationService` unchanged (standard bundling path).

---

## File Checklist

### Backend
- [ ] `sharedservice/SharedServiceInvoicingService.java`
- [ ] `sharedservice/SharedServiceConfigException.java`
- [ ] `sharedservice/SharedServicePercentageException.java` (may already exist from step 32)

### Frontend
- [ ] `src/pages/invoices/components/InvoiceLineItemsTable.jsx` — add shared-service tooltip (minor enhancement to step-29 file)
