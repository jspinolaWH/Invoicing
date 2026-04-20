package com.example.invoicing.entity.billingevent.audit;

import com.example.invoicing.entity.billingevent.audit.dto.AuditLogEntryResponse;
import com.example.invoicing.repository.BillingEventAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final BillingEventAuditLogRepository auditLogRepository;

    public List<AuditLogEntryResponse> getChangeHistoryForEvent(Long billingEventId) {
        return auditLogRepository
            .findByBillingEventIdOrderByChangedAtDesc(billingEventId)
            .stream().map(this::toResponse).toList();
    }

    public List<AuditLogEntryResponse> getUserActivity(String userId, Instant from, Instant to) {
        return auditLogRepository
            .findByUserAndTimeRange(userId, from, to)
            .stream().map(this::toResponse).toList();
    }

    public List<AuditLogEntryResponse> getChangesByField(String fieldName) {
        return auditLogRepository
            .findByFieldOrderByChangedAtDesc(fieldName)
            .stream().map(this::toResponse).toList();
    }

    private AuditLogEntryResponse toResponse(BillingEventAuditLog log) {
        return AuditLogEntryResponse.builder()
            .id(log.getId())
            .billingEventId(log.getBillingEventId())
            .field(log.getField())
            .oldValue(log.getOldValue())
            .newValue(log.getNewValue())
            .changedBy(log.getChangedBy())
            .confirmedBy(log.getConfirmedBy())
            .changedAt(log.getChangedAt())
            .reason(log.getReason())
            .build();
    }
}
