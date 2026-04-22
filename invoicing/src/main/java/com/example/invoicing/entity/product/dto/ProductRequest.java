package com.example.invoicing.entity.product.dto;

import com.example.invoicing.entity.product.PricingUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    private String code;
    private PricingUnit pricingUnit;
    private boolean reverseChargeVat;
    private Long defaultAccountingAccountId;
    private Long defaultCostCenterId;
    private Long priceListId;
}
