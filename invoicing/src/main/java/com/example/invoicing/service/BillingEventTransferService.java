package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEvent;
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

    public TransferResult transfer(Long eventId, TransferEventRequest request, String currentUser) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        statusService.assertMutable(event);

        String previousCustomer = event.getCustomerNumber();
        String previousProperty = event.getLocationId();

        event.setCustomerNumber(request.getTargetCustomerNumber());
        if (request.getTargetPropertyId() != null) {
            event.setLocationId(request.getTargetPropertyId());
        }
        billingEventRepository.save(event);

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

        List<BillingEventAuditLog> auditEntries = new ArrayList<>();
        auditEntries.add(BillingEventAuditLog.builder()
            .billingEventId(eventId).field("customerNumber")
            .oldValue(previousCustomer).newValue(request.getTargetCustomerNumber())
            .changedBy(currentUser).changedAt(Instant.now())
            .reason(request.getReason()).build());
        if (request.getTargetPropertyId() != null) {
            auditEntries.add(BillingEventAuditLog.builder()
                .billingEventId(eventId).field("locationId")
                .oldValue(previousProperty).newValue(request.getTargetPropertyId())
                .changedBy(currentUser).changedAt(Instant.now())
                .reason(request.getReason()).build());
        }
        auditLogRepository.saveAll(auditEntries);

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

                event.setCustomerNumber(request.getTargetCustomerNumber());
                if (request.getTargetPropertyId() != null) {
                    event.setLocationId(request.getTargetPropertyId());
                }
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
                    .billingEventId(event.getId()).field("customerNumber")
                    .oldValue(previousCustomer).newValue(request.getTargetCustomerNumber())
                    .changedBy(currentUser).changedAt(Instant.now())
                    .reason(request.getReason()).build());
                if (request.getTargetPropertyId() != null) {
                    auditEntries.add(BillingEventAuditLog.builder()
                        .billingEventId(event.getId()).field("locationId")
                        .oldValue(previousProperty).newValue(request.getTargetPropertyId())
                        .changedBy(currentUser).changedAt(Instant.now())
                        .reason(request.getReason()).build());
                }

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
}
