package com.example.invoicing.service;
import com.example.invoicing.entity.billingevent.dto.AffectedEventSummary;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeResult;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangePreview;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeRequest;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.repository.BillingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceResponsibilityChangeService {

    private final BillingEventRepository billingEventRepository;

    @Transactional(readOnly = true)
    public ResponsibilityChangePreview preview(ResponsibilityChangeRequest request) {
        validateBase(request);
        List<BillingEvent> events = resolveEvents(request);

        List<AffectedEventSummary> summaries = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int inProgressCount = 0;
        int billedCount = 0;

        for (BillingEvent e : events) {
            BigDecimal amt = orZero(e.getQuantity()).multiply(
                orZero(e.getWasteFeePrice()).add(orZero(e.getTransportFeePrice())).add(orZero(e.getEcoFeePrice()))
            );
            total = total.add(amt);

            boolean isBilled = e.getStatus() == BillingEventStatus.SENT
                || e.getStatus() == BillingEventStatus.COMPLETED;
            if (isBilled) billedCount++;
            else inProgressCount++;

            summaries.add(AffectedEventSummary.builder()
                .eventId(e.getId())
                .eventDate(e.getEventDate())
                .productCode(e.getProduct() != null ? e.getProduct().getCode() : null)
                .quantity(e.getQuantity())
                .totalFeeAmount(amt)
                .status(e.getStatus().name())
                .build());
        }

        return ResponsibilityChangePreview.builder()
            .fromCustomerNumber(request.getFromCustomerNumber())
            .toCustomerNumber(request.getToCustomerNumber())
            .totalEventCount(events.size())
            .inProgressCount(inProgressCount)
            .billedCount(billedCount)
            .totalTransferAmount(total)
            .events(summaries)
            .build();
    }

    @Transactional
    public ResponsibilityChangeResult apply(ResponsibilityChangeRequest request) {
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new IllegalArgumentException("internalComment is required");
        }
        validateBase(request);
        List<BillingEvent> events = resolveEvents(request);

        int movedInProgress = 0;
        int correctionCopies = 0;
        int excludedCount = 0;
        List<Long> createdEventIds = new ArrayList<>();

        for (BillingEvent e : events) {
            boolean isBilled = e.getStatus() == BillingEventStatus.SENT
                || e.getStatus() == BillingEventStatus.COMPLETED;

            if (!isBilled) {
                e.setCustomerNumber(request.getToCustomerNumber());
                billingEventRepository.save(e);
                movedInProgress++;
            } else {
                e.setExcluded(true);
                e.setExclusionReason("SERVICE_RESPONSIBILITY_CHANGE: " + request.getReason());
                e.setExcludedBy("SYSTEM");
                e.setExcludedAt(Instant.now());
                billingEventRepository.save(e);
                excludedCount++;

                BillingEvent copy = copyEvent(e, request.getToCustomerNumber());
                copy.setCorrectedFromEventId(e.getId());
                BillingEvent saved = billingEventRepository.save(copy);
                createdEventIds.add(saved.getId());
                correctionCopies++;
            }
        }

        log.info("Service responsibility change applied: {} moved, {} copies created for {} → {}",
            movedInProgress, correctionCopies,
            request.getFromCustomerNumber(), request.getToCustomerNumber());

        return ResponsibilityChangeResult.builder()
            .fromCustomerNumber(request.getFromCustomerNumber())
            .toCustomerNumber(request.getToCustomerNumber())
            .movedInProgressCount(movedInProgress)
            .correctionCopiesCreated(correctionCopies)
            .excludedCount(excludedCount)
            .createdEventIds(createdEventIds)
            .message(String.format(
                "Responsibility change applied: %d events moved, %d correction copies created for re-invoicing.",
                movedInProgress, correctionCopies))
            .build();
    }

    private List<BillingEvent> resolveEvents(ResponsibilityChangeRequest request) {
        if (request.getSpecificEventIds() != null && !request.getSpecificEventIds().isEmpty()) {
            List<BillingEvent> events = billingEventRepository.findAllById(request.getSpecificEventIds());
            return events.stream()
                .filter(e -> request.getFromCustomerNumber().equals(e.getCustomerNumber()) && !e.isExcluded())
                .toList();
        }
        return billingEventRepository.findByCustomerAndPeriod(
            request.getFromCustomerNumber(),
            request.getEventDateFrom(),
            request.getEventDateTo(),
            request.getProductId()
        );
    }

    private void validateBase(ResponsibilityChangeRequest request) {
        if (request.getFromCustomerNumber() == null || request.getFromCustomerNumber().isBlank())
            throw new IllegalArgumentException("fromCustomerNumber is required");
        if (request.getToCustomerNumber() == null || request.getToCustomerNumber().isBlank())
            throw new IllegalArgumentException("toCustomerNumber is required");
        if (request.getFromCustomerNumber().equals(request.getToCustomerNumber()))
            throw new IllegalArgumentException("fromCustomerNumber and toCustomerNumber must be different");
        if (request.getSpecificEventIds() == null || request.getSpecificEventIds().isEmpty()) {
            if (request.getEventDateFrom() == null || request.getEventDateTo() == null)
                throw new IllegalArgumentException("eventDateFrom and eventDateTo are required when specificEventIds not provided");
        }
    }

    private BillingEvent copyEvent(BillingEvent original, String targetCustomerNumber) {
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
        copy.setCustomerNumber(targetCustomerNumber);
        copy.setStatus(BillingEventStatus.IN_PROGRESS);
        copy.setExcluded(false);
        copy.setOfficeReviewRequired(false);
        return copy;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
