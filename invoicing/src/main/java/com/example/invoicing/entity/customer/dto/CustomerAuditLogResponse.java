package com.example.invoicing.entity.customer.dto;

import com.example.invoicing.entity.customer.CustomerAuditLog;
import lombok.*;

import java.time.Instant;

@Getter @Builder
public class CustomerAuditLogResponse {
    private Long id;
    private Long customerId;
    private String field;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private Instant changedAt;

    public static CustomerAuditLogResponse from(CustomerAuditLog log) {
        return CustomerAuditLogResponse.builder()
            .id(log.getId())
            .customerId(log.getCustomerId())
            .field(log.getField())
            .oldValue(log.getOldValue())
            .newValue(log.getNewValue())
            .changedBy(log.getChangedBy())
            .changedAt(log.getChangedAt())
            .build();
    }
}
