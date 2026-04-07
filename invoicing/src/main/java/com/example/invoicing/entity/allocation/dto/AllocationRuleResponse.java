package com.example.invoicing.entity.allocation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AllocationRuleResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String region;
    private String municipality;
    private Long accountingAccountId;
    private String accountingAccountCode;
    private String accountingAccountName;
    private Integer specificityScore;
    private String description;
    private boolean active;
    private Instant createdAt;
    private String createdBy;
}
