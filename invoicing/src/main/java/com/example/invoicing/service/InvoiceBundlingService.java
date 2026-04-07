package com.example.invoicing.service;
import com.example.invoicing.repository.BundlingRuleRepository;
import com.example.invoicing.entity.bundling.BundlingType;
import com.example.invoicing.entity.bundling.BundlingRule;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceBundlingService {

    private final BundlingRuleRepository bundlingRuleRepository;

    /**
     * Bundle billing events into invoice line items according to per-customer bundling rules.
     * Events with a sharedServiceGroupId are left for SharedServiceInvoicingService (caller handles them).
     */
    public List<InvoiceLineItem> bundle(List<BillingEvent> events, String customerNumber) {
        return doBundling(events, customerNumber);
    }

    /** Same as bundle() but returns detached (non-persisted) line items with id = null. */
    public List<InvoiceLineItem> bundleForPreview(List<BillingEvent> events, String customerNumber) {
        return doBundling(events, customerNumber);
    }

    private List<InvoiceLineItem> doBundling(List<BillingEvent> events, String customerNumber) {
        List<InvoiceLineItem> result = new ArrayList<>();
        int order = 1;

        // Separate shared-service events — they are handled by SharedServiceInvoicingService
        List<BillingEvent> regularEvents = events.stream()
            .filter(e -> e.getSharedServiceGroupId() == null)
            .collect(Collectors.toList());

        // Group by product group code (fall back to product.code if productGroupCode is null)
        Map<String, List<BillingEvent>> byGroup = regularEvents.stream()
            .collect(Collectors.groupingBy(e ->
                e.getProduct() != null && e.getProduct().getProductGroupCode() != null
                    ? e.getProduct().getProductGroupCode()
                    : (e.getProduct() != null ? e.getProduct().getCode() : "UNKNOWN"),
                LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<BillingEvent>> entry : byGroup.entrySet()) {
            String groupCode = entry.getKey();
            List<BillingEvent> groupEvents = entry.getValue();

            BundlingType bundlingType = bundlingRuleRepository
                .findByCustomerNumberAndProductGroup(customerNumber, groupCode)
                .map(BundlingRule::getBundlingType)
                .orElse(BundlingType.SEPARATE);

            if (bundlingType == BundlingType.SINGLE_LINE) {
                // All events in group must share the same classification — if they differ, split by classification
                Map<LegalClassification, List<BillingEvent>> byClassification = groupEvents.stream()
                    .collect(Collectors.groupingBy(
                        e -> e.getLegalClassification() != null ? e.getLegalClassification() : LegalClassification.PRIVATE_LAW,
                        LinkedHashMap::new, Collectors.toList()));

                for (Map.Entry<LegalClassification, List<BillingEvent>> classEntry : byClassification.entrySet()) {
                    List<BillingEvent> sameClassEvents = classEntry.getValue();
                    result.add(buildBundledLine(sameClassEvents, classEntry.getKey(), order++));
                }
            } else {
                for (BillingEvent event : groupEvents) {
                    result.add(buildSeparateLine(event, order++));
                }
            }
        }

        return result;
    }

    private InvoiceLineItem buildBundledLine(List<BillingEvent> events, LegalClassification classification, int order) {
        BillingEvent first = events.get(0);
        BigDecimal totalQuantity = events.stream()
            .map(BillingEvent::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalNet = events.stream()
            .map(this::computeNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal unitPrice = computeUnitPrice(first);
        BigDecimal vatRate = computeVatRate(first);
        BigDecimal totalGross = totalNet.multiply(BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
            .setScale(4, RoundingMode.HALF_UP);

        InvoiceLineItem line = new InvoiceLineItem();
        line.setProduct(first.getProduct());
        line.setDescription(first.getProduct() != null ? first.getProduct().getCode() : "");
        line.setQuantity(totalQuantity);
        line.setUnitPrice(unitPrice);
        line.setVatRate(vatRate);
        line.setNetAmount(totalNet);
        line.setGrossAmount(totalGross);
        line.setLegalClassification(classification);
        line.setAccountingAccount(first.getAccountingAccount());
        line.setCostCenter(first.getCostCenter());
        line.setBundled(events.size() > 1);
        line.setLineOrder(order);
        line.setSourceEvent(first);
        return line;
    }

    private InvoiceLineItem buildSeparateLine(BillingEvent event, int order) {
        BigDecimal unitPrice = computeUnitPrice(event);
        BigDecimal vatRate = computeVatRate(event);
        BigDecimal net = computeNetAmount(event);
        BigDecimal gross = net.multiply(BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
            .setScale(4, RoundingMode.HALF_UP);

        InvoiceLineItem line = new InvoiceLineItem();
        line.setProduct(event.getProduct());
        line.setDescription(event.getProduct() != null ? event.getProduct().getCode() : "");
        line.setQuantity(event.getQuantity().setScale(4, RoundingMode.HALF_UP));
        line.setUnitPrice(unitPrice);
        line.setVatRate(vatRate);
        line.setNetAmount(net);
        line.setGrossAmount(gross);
        line.setLegalClassification(event.getLegalClassification() != null ? event.getLegalClassification() : LegalClassification.PRIVATE_LAW);
        line.setAccountingAccount(event.getAccountingAccount());
        line.setCostCenter(event.getCostCenter());
        line.setBundled(false);
        line.setLineOrder(order);
        line.setSourceEvent(event);
        return line;
    }

    private BigDecimal computeUnitPrice(BillingEvent event) {
        return event.getWasteFeePrice()
            .add(event.getTransportFeePrice())
            .add(event.getEcoFeePrice())
            .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeNetAmount(BillingEvent event) {
        return computeUnitPrice(event)
            .multiply(event.getQuantity())
            .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeVatRate(BillingEvent event) {
        // Use vatRate24 if non-zero, otherwise vatRate0
        if (event.getVatRate24() != null && event.getVatRate24().compareTo(BigDecimal.ZERO) > 0) {
            return event.getVatRate24().setScale(2, RoundingMode.HALF_UP);
        }
        return event.getVatRate0() != null ? event.getVatRate0().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}
