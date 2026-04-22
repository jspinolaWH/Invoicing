package com.example.invoicing.service;
import com.example.invoicing.entity.billingevent.dto.AdjustmentType;
import com.example.invoicing.entity.billingevent.dto.AffectedEventEntry;
import com.example.invoicing.entity.billingevent.dto.PriceAdjustmentAuditLog;
import com.example.invoicing.entity.billingevent.dto.PriceAdjustmentResult;
import com.example.invoicing.entity.billingevent.dto.PriceAdjustmentPreview;
import com.example.invoicing.entity.billingevent.dto.PriceAdjustmentRequest;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.PriceAdjustmentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetroactivePriceAdjustmentService {

    private final BillingEventRepository billingEventRepository;
    private final PriceAdjustmentAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PriceAdjustmentPreview preview(PriceAdjustmentRequest request) {
        validateBase(request);
        List<BillingEvent> events = fetchEvents(request);

        List<AffectedEventEntry> entries = new ArrayList<>();
        BigDecimal totalCurrentNet = BigDecimal.ZERO;
        BigDecimal totalProjectedNet = BigDecimal.ZERO;
        int inProgressCount = 0;
        int billedCount = 0;

        BigDecimal multiplier = percentageMultiplier(request);

        for (BillingEvent e : events) {
            BigDecimal projWaste, projTransport, projEco;
            if (multiplier != null) {
                projWaste = orZero(e.getWasteFeePrice()).multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
                projTransport = orZero(e.getTransportFeePrice()).multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
                projEco = orZero(e.getEcoFeePrice()).multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
            } else {
                projWaste = request.getNewWasteFeePrice() != null
                    ? request.getNewWasteFeePrice() : orZero(e.getWasteFeePrice());
                projTransport = request.getNewTransportFeePrice() != null
                    ? request.getNewTransportFeePrice() : orZero(e.getTransportFeePrice());
                projEco = request.getNewEcoFeePrice() != null
                    ? request.getNewEcoFeePrice() : orZero(e.getEcoFeePrice());
            }

            BigDecimal qty = orZero(e.getQuantity());
            BigDecimal currentNet = qty.multiply(
                orZero(e.getWasteFeePrice()).add(orZero(e.getTransportFeePrice())).add(orZero(e.getEcoFeePrice()))
            );
            BigDecimal projectedNet = qty.multiply(projWaste.add(projTransport).add(projEco));

            totalCurrentNet = totalCurrentNet.add(currentNet);
            totalProjectedNet = totalProjectedNet.add(projectedNet);

            boolean isBilled = e.getStatus() == BillingEventStatus.SENT
                || e.getStatus() == BillingEventStatus.COMPLETED;
            if (isBilled) billedCount++;
            else inProgressCount++;

            entries.add(AffectedEventEntry.builder()
                .eventId(e.getId())
                .eventDate(e.getEventDate())
                .productCode(e.getProduct() != null ? e.getProduct().getCode() : null)
                .currentWasteFeePrice(e.getWasteFeePrice())
                .currentTransportFeePrice(e.getTransportFeePrice())
                .currentEcoFeePrice(e.getEcoFeePrice())
                .projectedWasteFeePrice(projWaste)
                .projectedTransportFeePrice(projTransport)
                .projectedEcoFeePrice(projEco)
                .quantity(qty)
                .currentNetAmount(currentNet)
                .projectedNetAmount(projectedNet)
                .delta(projectedNet.subtract(currentNet))
                .status(e.getStatus().name())
                .build());
        }

        return PriceAdjustmentPreview.builder()
            .customerNumber(request.getCustomerNumber())
            .totalEventCount(events.size())
            .inProgressCount(inProgressCount)
            .billedCount(billedCount)
            .totalCurrentNet(totalCurrentNet)
            .totalProjectedNet(totalProjectedNet)
            .totalDelta(totalProjectedNet.subtract(totalCurrentNet))
            .events(entries)
            .build();
    }

    @Transactional
    public PriceAdjustmentResult apply(PriceAdjustmentRequest request) {
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new IllegalArgumentException("internalComment is required for price adjustment");
        }
        validateBase(request);

        List<BillingEvent> events = fetchEvents(request);

        int updatedInProgress = 0;
        int correctionCopiesCreated = 0;
        int excludedCount = 0;
        BigDecimal totalDelta = BigDecimal.ZERO;
        List<Long> createdEventIds = new ArrayList<>();
        List<Long> affectedSourceIds = new ArrayList<>();

        for (BillingEvent e : events) {
            affectedSourceIds.add(e.getId());
            BigDecimal qty = orZero(e.getQuantity());
            BigDecimal currentNet = qty.multiply(
                orZero(e.getWasteFeePrice()).add(orZero(e.getTransportFeePrice())).add(orZero(e.getEcoFeePrice()))
            );

            boolean isBilled = e.getStatus() == BillingEventStatus.SENT
                || e.getStatus() == BillingEventStatus.COMPLETED;

            if (!isBilled) {
                applyPrices(e, request);
                billingEventRepository.save(e);
                updatedInProgress++;
                BigDecimal newNet = qty.multiply(
                    orZero(e.getWasteFeePrice()).add(orZero(e.getTransportFeePrice())).add(orZero(e.getEcoFeePrice()))
                );
                totalDelta = totalDelta.add(newNet.subtract(currentNet));
            } else {
                e.setExcluded(true);
                e.setExclusionReason("RETROACTIVE_PRICE_ADJUSTMENT: " + request.getReason());
                e.setExcludedBy("SYSTEM");
                e.setExcludedAt(Instant.now());
                billingEventRepository.save(e);
                excludedCount++;

                BillingEvent correction = copyEvent(e);
                applyPrices(correction, request);
                correction.setCorrectedFromEventId(e.getId());
                BillingEvent saved = billingEventRepository.save(correction);
                createdEventIds.add(saved.getId());
                correctionCopiesCreated++;

                BigDecimal newNet = qty.multiply(
                    orZero(correction.getWasteFeePrice()).add(orZero(correction.getTransportFeePrice())).add(orZero(correction.getEcoFeePrice()))
                );
                totalDelta = totalDelta.add(newNet.subtract(currentNet));
            }
        }

        auditLogRepository.save(PriceAdjustmentAuditLog.builder()
            .customerNumber(request.getCustomerNumber())
            .performedBy(request.getPerformedBy())
            .reason(request.getReason())
            .internalComment(request.getInternalComment())
            .adjustmentType(request.getAdjustmentType() != null ? request.getAdjustmentType() : AdjustmentType.FIXED)
            .adjustmentValue(request.getAdjustmentValue())
            .appliedAt(Instant.now())
            .affectedEventIds(affectedSourceIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .totalDelta(totalDelta)
            .build());

        log.info("Retroactive price adjustment applied for customer {}: {} in-progress updated, {} correction copies created",
            request.getCustomerNumber(), updatedInProgress, correctionCopiesCreated);

        return PriceAdjustmentResult.builder()
            .customerNumber(request.getCustomerNumber())
            .updatedInProgressCount(updatedInProgress)
            .correctionCopiesCreated(correctionCopiesCreated)
            .excludedCount(excludedCount)
            .totalDelta(totalDelta)
            .createdEventIds(createdEventIds)
            .message(String.format(
                "Price adjustment applied: %d events updated in-place, %d correction copies created for re-invoicing.",
                updatedInProgress, correctionCopiesCreated))
            .build();
    }

    private List<BillingEvent> fetchEvents(PriceAdjustmentRequest request) {
        if (request.getEventIds() != null && !request.getEventIds().isEmpty()) {
            return billingEventRepository.findAllById(request.getEventIds());
        }
        return billingEventRepository.findByCustomerAndPeriod(
            request.getCustomerNumber(),
            request.getEventDateFrom(),
            request.getEventDateTo(),
            request.getProductId(),
            request.getServiceResponsibility()
        );
    }

    private void validateBase(PriceAdjustmentRequest request) {
        if (request.getCustomerNumber() == null || request.getCustomerNumber().isBlank())
            throw new IllegalArgumentException("customerNumber is required");
        boolean hasEventIds = request.getEventIds() != null && !request.getEventIds().isEmpty();
        if (!hasEventIds) {
            if (request.getEventDateFrom() == null || request.getEventDateTo() == null)
                throw new IllegalArgumentException("eventDateFrom and eventDateTo are required when eventIds not provided");
            if (request.getEventDateFrom().isAfter(request.getEventDateTo()))
                throw new IllegalArgumentException("eventDateFrom must not be after eventDateTo");
        }
    }

    private void applyPrices(BillingEvent e, PriceAdjustmentRequest req) {
        BigDecimal multiplier = percentageMultiplier(req);
        if (multiplier != null) {
            if (e.getWasteFeePrice() != null)
                e.setWasteFeePrice(e.getWasteFeePrice().multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
            if (e.getTransportFeePrice() != null)
                e.setTransportFeePrice(e.getTransportFeePrice().multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
            if (e.getEcoFeePrice() != null)
                e.setEcoFeePrice(e.getEcoFeePrice().multiply(multiplier).setScale(4, RoundingMode.HALF_UP));
        } else {
            if (req.getNewWasteFeePrice() != null) e.setWasteFeePrice(req.getNewWasteFeePrice());
            if (req.getNewTransportFeePrice() != null) e.setTransportFeePrice(req.getNewTransportFeePrice());
            if (req.getNewEcoFeePrice() != null) e.setEcoFeePrice(req.getNewEcoFeePrice());
        }
    }

    private BigDecimal percentageMultiplier(PriceAdjustmentRequest req) {
        if (req.getAdjustmentType() == AdjustmentType.PERCENTAGE && req.getAdjustmentValue() != null) {
            return BigDecimal.ONE.add(
                req.getAdjustmentValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
            );
        }
        return null;
    }

    private BillingEvent copyEvent(BillingEvent original) {
        BillingEvent copy = new BillingEvent();
        copy.setEventDate(original.getEventDate());
        copy.setProduct(original.getProduct());
        copy.setQuantity(original.getQuantity());
        copy.setWeight(original.getWeight());
        copy.setWasteFeePrice(original.getWasteFeePrice());
        copy.setTransportFeePrice(original.getTransportFeePrice());
        copy.setEcoFeePrice(original.getEcoFeePrice());
        copy.setVatRate0(original.getVatRate0());
        copy.setVatRate24(original.getVatRate24());
        copy.setLegalClassification(original.getLegalClassification());
        copy.setAccountingAccount(original.getAccountingAccount());
        copy.setCostCenter(original.getCostCenter());
        copy.setMunicipalityId(original.getMunicipalityId());
        copy.setLocationId(original.getLocationId());
        copy.setContractor(original.getContractor());
        copy.setVehicleId(original.getVehicleId());
        copy.setDriverId(original.getDriverId());
        copy.setProjectId(original.getProjectId());
        copy.setOrigin(original.getOrigin());
        copy.setCustomerNumber(original.getCustomerNumber());
        copy.setStatus(BillingEventStatus.IN_PROGRESS);
        copy.setExcluded(false);
        copy.setOfficeReviewRequired(false);
        return copy;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
