package com.example.invoicing.entity.product.dto;

import com.example.invoicing.entity.product.PricingUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    @NotBlank private String code;
    @NotNull private PricingUnit pricingUnit;
    private boolean reverseChargeVat;
    private Long defaultAccountingAccountId;
    private Long defaultCostCenterId;
    private Long priceListId;
}
