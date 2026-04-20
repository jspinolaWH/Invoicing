package com.example.invoicing.entity.billingevent.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditLogEntryResponse {
    private Long id;
    private Long billingEventId;
    private String field;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private String confirmedBy;
    private Instant changedAt;
    private String reason;
}
