package com.example.invoicing.sharedservice;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.invoice.InvoiceLineItem;
import com.example.invoicing.repository.BillingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedServiceInvoicingService {

    private final SharedServiceParticipantRepository participantRepository;
    private final SharedServiceValidationService validationService;
    private final BillingEventRepository billingEventRepository;

    /**
     * Generate one InvoiceLineItem per participant active on the event date.
     * Uses the event's computed net amount (fee prices * quantity).
     */
    @Transactional
    public List<InvoiceLineItem> distributeEvent(BillingEvent event) {
        Long groupId = resolveGroupId(event);

        validationService.validateTotalEquals100(groupId);

        List<SharedServiceParticipant> participants =
            participantRepository.findActiveAtDate(groupId, event.getEventDate());

        if (participants.isEmpty()) {
            throw new SharedServiceConfigException(
                "No active participants found for group " + event.getSharedServiceGroupId()
                + " on date " + event.getEventDate());
        }

        BigDecimal totalNet = computeNet(event);
        BigDecimal unitPrice = computeUnitPrice(event);
        BigDecimal vatRate = resolveVatRate(event);
        BigDecimal totalGross = totalNet.multiply(
            BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)))
            .setScale(4, RoundingMode.HALF_UP);

        List<InvoiceLineItem> lines = new ArrayList<>();
        BigDecimal allocatedNet = BigDecimal.ZERO;
        BigDecimal allocatedGross = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            SharedServiceParticipant participant = participants.get(i);
            boolean isLast = (i == participants.size() - 1);

            BigDecimal share = participant.getSharePercentage()
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

            BigDecimal lineNet;
            BigDecimal lineGross;

            if (isLast) {
                lineNet = totalNet.subtract(allocatedNet).setScale(4, RoundingMode.HALF_UP);
                lineGross = totalGross.subtract(allocatedGross).setScale(4, RoundingMode.HALF_UP);
            } else {
                lineNet = totalNet.multiply(share).setScale(4, RoundingMode.HALF_UP);
                lineGross = totalGross.multiply(share).setScale(4, RoundingMode.HALF_UP);
                allocatedNet = allocatedNet.add(lineNet);
                allocatedGross = allocatedGross.add(lineGross);
            }

            BigDecimal lineQty = event.getQuantity().multiply(share).setScale(4, RoundingMode.HALF_UP);

            InvoiceLineItem line = new InvoiceLineItem();
            line.setProduct(event.getProduct());
            line.setDescription((event.getProduct() != null ? event.getProduct().getCode() : "")
                + " (" + participant.getSharePercentage() + "% share)");
            line.setQuantity(lineQty);
            line.setUnitPrice(unitPrice);
            line.setVatRate(vatRate);
            line.setNetAmount(lineNet);
            line.setGrossAmount(lineGross);
            line.setLegalClassification(event.getLegalClassification() != null
                ? event.getLegalClassification()
                : com.example.invoicing.entity.classification.LegalClassification.PRIVATE_LAW);
            line.setAccountingAccount(event.getAccountingAccount());
            line.setCostCenter(event.getCostCenter());
            line.setBundled(false);
            line.setSourceEvent(event);
            lines.add(line);
        }

        return lines;
    }

    @Transactional
    public void redistributeRetroactive(Long groupId, LocalDate validFrom) {
        validationService.validateTotalEquals100(groupId);

        // Find the string groupId from participants (groupId here is PropertyGroup PK)
        // We need to query events by shared_service_group_id (which is a string on BillingEvent)
        // Use property group name as the key or find events another way
        List<BillingEvent> events = billingEventRepository.findBySharedServiceGroup(
            groupId.toString(), validFrom);

        for (BillingEvent event : events) {
            // Re-distribute with new participant set — results used by generation service
            distributeEvent(event);
        }
    }

    private Long resolveGroupId(BillingEvent event) {
        try {
            return Long.parseLong(event.getSharedServiceGroupId());
        } catch (NumberFormatException e) {
            throw new SharedServiceConfigException(
                "sharedServiceGroupId must be a numeric PropertyGroup ID, got: " + event.getSharedServiceGroupId());
        }
    }

    private BigDecimal computeUnitPrice(BillingEvent event) {
        return event.getWasteFeePrice()
            .add(event.getTransportFeePrice())
            .add(event.getEcoFeePrice())
            .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeNet(BillingEvent event) {
        return computeUnitPrice(event).multiply(event.getQuantity()).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveVatRate(BillingEvent event) {
        if (event.getVatRate24() != null && event.getVatRate24().compareTo(BigDecimal.ZERO) > 0) {
            return event.getVatRate24().setScale(2, RoundingMode.HALF_UP);
        }
        return event.getVatRate0() != null ? event.getVatRate0().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2);
    }
}
