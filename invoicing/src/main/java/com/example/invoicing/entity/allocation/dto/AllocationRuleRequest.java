package com.example.invoicing.entity.allocation.dto;

import com.example.invoicing.entity.account.PriceComponent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocationRuleRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String region;
    private String municipality;

    private PriceComponent priceComponent;

    @NotNull(message = "Accounting account ID is required")
    private Long accountingAccountId;

    private String description;
}
