package com.example.invoicing.entity.allocation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocationRuleRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String region;
    private String municipality;

    @NotNull(message = "Accounting account ID is required")
    private Long accountingAccountId;

    private String description;
}
