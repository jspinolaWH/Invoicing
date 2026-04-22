package com.example.invoicing.entity.billingthreshold;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BillingThresholdConfigResponse {
    private Long id;
    private String serviceResponsibility;
    private BigDecimal annualEuroLimit;
    private String description;
    private boolean active;
    private String createdBy;
    private Instant createdAt;
}
