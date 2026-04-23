package com.example.invoicing.entity.bundling.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BundlingRuleAuditLogResponse {
    private Long id;
    private String productGroup;
    private String action;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private Instant changedAt;
}
