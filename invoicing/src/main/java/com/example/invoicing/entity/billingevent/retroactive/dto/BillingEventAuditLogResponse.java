package com.example.invoicing.entity.billingevent.retroactive.dto;

import com.example.invoicing.entity.billingevent.audit.BillingEventAuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BillingEventAuditLogResponse {

    private Long id;
    private Long billingEventId;
    private String field;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private Instant changedAt;
    private String reason;

    public static BillingEventAuditLogResponse from(BillingEventAuditLog log) {
        return BillingEventAuditLogResponse.builder()
            .id(log.getId())
            .billingEventId(log.getBillingEventId())
            .field(log.getField())
            .oldValue(log.getOldValue())
            .newValue(log.getNewValue())
            .changedBy(log.getChangedBy())
            .changedAt(log.getChangedAt())
            .reason(log.getReason())
            .build();
    }
}
