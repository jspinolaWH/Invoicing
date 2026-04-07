package com.example.invoicing.driver;

import com.example.invoicing.driver.dto.DriverEventRequest;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.dto.BillingEventManualCreateRequest;
import com.example.invoicing.entity.billingevent.dto.BillingEventResponse;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.service.BillingEventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverEventService {

    private final BillingEventService billingEventService;
    private final BillingEventRepository billingEventRepository;
    private final EventTypeConfigRepository eventTypeConfigRepository;

    public BillingEventResponse submitDriverEvent(DriverEventRequest request, String driverId) {
        EventTypeConfig config = eventTypeConfigRepository
            .findByEventTypeCode(request.getEventTypeCode())
            .orElseThrow(() -> new EntityNotFoundException(
                "Unknown event type code: " + request.getEventTypeCode()));

        BillingEventManualCreateRequest createReq = mapToCreateRequest(request, driverId);
        BillingEventResponse created = billingEventService.createManual(createReq);

        boolean reviewRequired = evaluateReviewRequired(config, request);

        BillingEvent event = billingEventRepository.findById(created.getId()).orElseThrow();
        event.setDriverId(driverId);
        event.setOfficeReviewRequired(reviewRequired);
        event.setOrigin("DRIVER");
        billingEventRepository.save(event);

        return billingEventService.toResponse(event);
    }

    public BillingEventResponse approveReview(Long eventId, String reviewerUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (!event.isOfficeReviewRequired()) {
            throw new IllegalStateException("Event " + eventId + " does not require office review.");
        }
        if (event.getReviewedAt() != null) {
            throw new IllegalStateException("Event " + eventId + " has already been reviewed.");
        }

        event.setOfficeReviewRequired(false);
        event.setReviewedBy(reviewerUser);
        event.setReviewedAt(Instant.now());

        billingEventRepository.save(event);
        return billingEventService.toResponse(event);
    }

    public BillingEventResponse rejectReview(Long eventId, String rejectionReason, String reviewerUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (!event.isOfficeReviewRequired()) {
            throw new IllegalStateException("Event " + eventId + " does not require office review.");
        }
        if (event.getReviewedAt() != null) {
            throw new IllegalStateException("Event " + eventId + " has already been reviewed.");
        }

        event.setExcluded(true);
        event.setExclusionReason("REJECTED: " + rejectionReason);
        event.setExcludedBy(reviewerUser);
        event.setExcludedAt(Instant.now());
        event.setRejectionReason(rejectionReason);
        event.setReviewedBy(reviewerUser);
        event.setReviewedAt(Instant.now());

        billingEventRepository.save(event);
        return billingEventService.toResponse(event);
    }

    private boolean evaluateReviewRequired(EventTypeConfig config, DriverEventRequest request) {
        if (config.isRequiresOfficeReview()) return true;

        if (config.getUnusualQuantityThreshold() != null
                && request.getQuantity().compareTo(config.getUnusualQuantityThreshold()) > 0) {
            return true;
        }
        if (config.getUnusualWeightThreshold() != null
                && request.getWeight().compareTo(config.getUnusualWeightThreshold()) > 0) {
            return true;
        }

        BigDecimal highestPrice = List.of(
                request.getWasteFeePrice(), request.getTransportFeePrice(), request.getEcoFeePrice())
            .stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        if (config.getUnusualPriceThreshold() != null
                && highestPrice.compareTo(config.getUnusualPriceThreshold()) > 0) {
            return true;
        }

        if (config.isReviewIfUnknownLocation()
                && (request.getLocationId() == null || request.getLocationId().isBlank())) {
            return true;
        }

        return false;
    }

    private BillingEventManualCreateRequest mapToCreateRequest(DriverEventRequest req, String driverId) {
        BillingEventManualCreateRequest r = new BillingEventManualCreateRequest();
        r.setEventDate(req.getEventDate());
        r.setProductId(req.getProductId());
        r.setWasteFeePrice(req.getWasteFeePrice());
        r.setTransportFeePrice(req.getTransportFeePrice());
        r.setEcoFeePrice(req.getEcoFeePrice());
        r.setQuantity(req.getQuantity());
        r.setWeight(req.getWeight());
        r.setCustomerNumber(req.getCustomerNumber());
        r.setVehicleId(req.getVehicleId());
        r.setDriverId(driverId);
        r.setLocationId(req.getLocationId());
        r.setMunicipalityId(req.getMunicipalityId());
        r.setComments(req.getComments());
        return r;
    }
}
