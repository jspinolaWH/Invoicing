package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.audit.BillingEventAuditLog;
import com.example.invoicing.entity.billingevent.transfer.BillingEventTransferLog;
import com.example.invoicing.entity.billingevent.transfer.BillingEventTransferLogRepository;
import com.example.invoicing.entity.billingevent.transfer.dto.*;
import com.example.invoicing.repository.BillingEventAuditLogRepository;
import com.example.invoicing.repository.BillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventTransferService {

    private final BillingEventRepository billingEventRepository;
    private final BillingEventTransferLogRepository transferLogRepository;
    private final BillingEventAuditLogRepository auditLogRepository;
    private final BillingEventStatusService statusService;

    // ── Initiate transfer (moves to PENDING_TRANSFER) ──────────────────────

    public TransferResult transfer(Long eventId, TransferEventRequest request, String currentUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        statusService.assertMutable(event);

        String previousCustomer = event.getCustomerNumber();
        String previousProperty = event.getLocationId();

        event.setPriorCustomerNumber(previousCustomer);
        event.setPriorLocationId(previousProperty);
        event.setPendingTransferCustomerNumber(request.getTargetCustomerNumber());
        event.setPendingTransferLocationId(request.getTargetPropertyId());

        statusService.transitionTo(event, BillingEventStatus.PENDING_TRANSFER, null);

        transferLogRepository.save(BillingEventTransferLog.builder()
            .billingEventId(eventId)
            .sourceCustomerNumber(previousCustomer)
            .targetCustomerNumber(request.getTargetCustomerNumber())
            .sourcePropertyId(previousProperty)
            .targetPropertyId(request.getTargetPropertyId())
            .transferredBy(currentUser)
            .transferredAt(Instant.now())
            .reason(request.getReason())
            .build());

        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(eventId).field("transferInitiated")
            .oldValue(previousCustomer).newValue(request.getTargetCustomerNumber())
            .changedBy(currentUser).changedAt(Instant.now())
            .reason(request.getReason()).build());

        return new TransferResult(eventId, previousCustomer, request.getTargetCustomerNumber(), true, null);
    }

    public BulkTransferResult bulkTransfer(BulkTransferRequest request, String currentUser) {
        List<BillingEvent> events = billingEventRepository.findAllById(request.getEventIds());
        List<Long> succeeded = new ArrayList<>();
        List<BulkTransferFailure> failed = new ArrayList<>();

        List<BillingEventTransferLog> transferLogs = new ArrayList<>();
        List<BillingEventAuditLog> auditEntries = new ArrayList<>();

        for (BillingEvent event : events) {
            try {
                statusService.assertMutable(event);

                String previousCustomer = event.getCustomerNumber();
                String previousProperty = event.getLocationId();

                event.setPriorCustomerNumber(previousCustomer);
                event.setPriorLocationId(previousProperty);
                event.setPendingTransferCustomerNumber(request.getTargetCustomerNumber());
                event.setPendingTransferLocationId(request.getTargetPropertyId());
                event.setStatus(BillingEventStatus.PENDING_TRANSFER);
                succeeded.add(event.getId());

                transferLogs.add(BillingEventTransferLog.builder()
                    .billingEventId(event.getId())
                    .sourceCustomerNumber(previousCustomer)
                    .targetCustomerNumber(request.getTargetCustomerNumber())
                    .sourcePropertyId(previousProperty)
                    .targetPropertyId(request.getTargetPropertyId())
                    .transferredBy(currentUser)
                    .transferredAt(Instant.now())
                    .reason(request.getReason())
                    .build());

                auditEntries.add(BillingEventAuditLog.builder()
                    .billingEventId(event.getId()).field("transferInitiated")
                    .oldValue(previousCustomer).newValue(request.getTargetCustomerNumber())
                    .changedBy(currentUser).changedAt(Instant.now())
                    .reason(request.getReason()).build());

            } catch (IllegalStateException ex) {
                failed.add(new BulkTransferFailure(event.getId(), ex.getMessage()));
            }
        }

        billingEventRepository.saveAll(events.stream()
            .filter(e -> succeeded.contains(e.getId()))
            .toList());
        transferLogRepository.saveAll(transferLogs);
        auditLogRepository.saveAll(auditEntries);

        return new BulkTransferResult(succeeded, failed);
    }

    // ── Confirm transfer (applies the pending change) ──────────────────────

    public TransferResult confirmTransfer(Long eventId, String confirmedBy) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (event.getStatus() != BillingEventStatus.PENDING_TRANSFER) {
            throw new IllegalStateException(
                "BillingEvent " + eventId + " is not in PENDING_TRANSFER state.");
        }

        String initiator = transferLogRepository
            .findByBillingEventIdOrderByTransferredAtDesc(eventId)
            .stream().findFirst()
            .map(BillingEventTransferLog::getTransferredBy)
            .orElse("unknown");

        String newCustomer = event.getPendingTransferCustomerNumber();
        String newLocation = event.getPendingTransferLocationId();

        event.setCustomerNumber(newCustomer);
        if (newLocation != null) {
            event.setLocationId(newLocation);
        }
        event.setPendingTransferCustomerNumber(null);
        event.setPendingTransferLocationId(null);

        statusService.transitionTo(event, BillingEventStatus.IN_PROGRESS, null);

        List<BillingEventAuditLog> auditEntries = new ArrayList<>();
        auditEntries.add(BillingEventAuditLog.builder()
            .billingEventId(eventId).field("customerNumber")
            .oldValue(event.getPriorCustomerNumber()).newValue(newCustomer)
            .changedBy(initiator).confirmedBy(confirmedBy).changedAt(Instant.now())
            .reason("Transfer confirmed").build());
        if (newLocation != null) {
            auditEntries.add(BillingEventAuditLog.builder()
                .billingEventId(eventId).field("locationId")
                .oldValue(event.getPriorLocationId()).newValue(newLocation)
                .changedBy(initiator).confirmedBy(confirmedBy).changedAt(Instant.now())
                .reason("Transfer confirmed").build());
        }
        auditLogRepository.saveAll(auditEntries);

        return new TransferResult(eventId, event.getPriorCustomerNumber(), newCustomer, true, null);
    }

    public BulkTransferResult bulkConfirmTransfer(List<Long> eventIds, String confirmedBy) {
        List<Long> succeeded = new ArrayList<>();
        List<BulkTransferFailure> failed = new ArrayList<>();
        for (Long id : eventIds) {
            try {
                confirmTransfer(id, confirmedBy);
                succeeded.add(id);
            } catch (Exception ex) {
                failed.add(new BulkTransferFailure(id, ex.getMessage()));
            }
        }
        return new BulkTransferResult(succeeded, failed);
    }

    // ── Cancel transfer (reverts to IN_PROGRESS) ───────────────────────────

    public TransferResult cancelTransfer(Long eventId, String cancelledBy) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        if (event.getStatus() != BillingEventStatus.PENDING_TRANSFER) {
            throw new IllegalStateException(
                "BillingEvent " + eventId + " is not in PENDING_TRANSFER state.");
        }

        String pendingTarget = event.getPendingTransferCustomerNumber();

        event.setPendingTransferCustomerNumber(null);
        event.setPendingTransferLocationId(null);
        event.setPriorCustomerNumber(null);
        event.setPriorLocationId(null);

        statusService.transitionTo(event, BillingEventStatus.IN_PROGRESS, null);

        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(eventId).field("transferCancelled")
            .oldValue(pendingTarget).newValue(event.getCustomerNumber())
            .changedBy(cancelledBy).changedAt(Instant.now())
            .reason("Transfer cancelled").build());

        return new TransferResult(eventId, event.getCustomerNumber(), event.getCustomerNumber(), false, "Transfer cancelled");
    }

    public BulkTransferResult bulkCancelTransfer(List<Long> eventIds, String cancelledBy) {
        List<Long> succeeded = new ArrayList<>();
        List<BulkTransferFailure> failed = new ArrayList<>();
        for (Long id : eventIds) {
            try {
                cancelTransfer(id, cancelledBy);
                succeeded.add(id);
            } catch (Exception ex) {
                failed.add(new BulkTransferFailure(id, ex.getMessage()));
            }
        }
        return new BulkTransferResult(succeeded, failed);
    }
}
