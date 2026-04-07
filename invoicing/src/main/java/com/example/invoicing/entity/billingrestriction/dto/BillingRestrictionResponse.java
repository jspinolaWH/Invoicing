package com.example.invoicing.entity.billingrestriction.dto;

import com.example.invoicing.entity.billingrestriction.BillingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BillingRestrictionResponse {
    private Long id;
    private String municipality;
    private String customerType;
    private String serviceType;
    private Long locationId;
    private BigDecimal minAmount;
    private String period;
    private String serviceResponsibility;
    private BillingType billingType;
    private String description;
    private boolean active;
    private Instant createdAt;
    private String createdBy;
}
