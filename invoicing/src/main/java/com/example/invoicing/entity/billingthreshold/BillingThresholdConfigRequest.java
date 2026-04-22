package com.example.invoicing.entity.billingthreshold;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingThresholdConfigRequest {
    private String serviceResponsibility;
    private BigDecimal annualEuroLimit;
    private String description;
    private boolean active = true;
}
