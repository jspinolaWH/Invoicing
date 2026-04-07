package com.example.invoicing.entity.billingrestriction.dto;

import com.example.invoicing.entity.billingrestriction.BillingType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingRestrictionRequest {
    private String municipality;
    private String customerType;
    private String serviceType;
    private Long locationId;
    private BigDecimal minAmount;
    private String period;
    private String serviceResponsibility;
    @NotNull
    private BillingType billingType;
    private String description;
    private boolean active = true;
}
