package com.example.invoicing.entity.allocation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllocationResolveResponse {
    private Long matchedRuleId;
    private Integer specificityScore;
    private String accountingAccountCode;
    private String accountingAccountName;
    private String matchReason;
}
