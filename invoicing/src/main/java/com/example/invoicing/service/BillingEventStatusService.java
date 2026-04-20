package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.repository.BillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventStatusService {

    private final BillingEventRepository billingEventRepository;

    static final Map<BillingEventStatus, Set<BillingEventStatus>> ALLOWED_TRANSITIONS = Map.of(
        BillingEventStatus.DRAFT,            Set.of(BillingEventStatus.IN_PROGRESS),
        BillingEventStatus.IN_PROGRESS,      Set.of(BillingEventStatus.SENT, BillingEventStatus.PENDING_TRANSFER),
        BillingEventStatus.PENDING_TRANSFER, Set.of(BillingEventStatus.IN_PROGRESS),
        BillingEventStatus.SENT,             Set.of(BillingEventStatus.COMPLETED),
        BillingEventStatus.ERROR,            Set.of(BillingEventStatus.SENT),
        BillingEventStatus.COMPLETED,        Set.of()
    );

    public BillingEvent transitionTo(Long eventId, BillingEventStatus targetStatus) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));
        return applyTransition(event, targetStatus, null);
    }

    public BillingEvent transitionTo(BillingEvent event, BillingEventStatus targetStatus, String reason) {
        return applyTransition(event, targetStatus, reason);
    }

    private BillingEvent applyTransition(BillingEvent event, BillingEventStatus targetStatus, String reason) {
        assertValidTransition(event.getStatus(), targetStatus, event.getId());
        event.setStatus(targetStatus);
        if (targetStatus == BillingEventStatus.ERROR) {
            event.setTransmissionErrorReason(reason);
        } else {
            event.setTransmissionErrorReason(null);
        }
        return billingEventRepository.save(event);
    }

    public void assertMutable(BillingEvent event) {
        if (event.getStatus() == BillingEventStatus.SENT
                || event.getStatus() == BillingEventStatus.COMPLETED
                || event.getStatus() == BillingEventStatus.PENDING_TRANSFER) {
            throw new IllegalStateException(
                "BillingEvent " + event.getId() + " is " + event.getStatus()
                + " and cannot be modified. Use the credit-and-re-invoice flow for corrections.");
        }
    }

    private void assertValidTransition(BillingEventStatus from, BillingEventStatus to, Long id) {
        Set<BillingEventStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "Invalid status transition for BillingEvent " + id
                + ": " + from + " \u2192 " + to + ". Allowed: " + allowed);
        }
    }
}
