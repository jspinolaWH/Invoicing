package com.example.invoicing.entity.seasonalfee.dto;

import com.example.invoicing.entity.billingcycle.BillingFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class SeasonalFeeConfigResponse {
    private Long id;
    private String customerNumber;
    private Long productId;
    private String productName;
    private BillingFrequency billingFrequency;
    private BigDecimal amount;
    private LocalDate nextDueDate;
    private String propertyReference;
    private String description;
    private boolean active;
    private Instant createdAt;
    private String createdBy;
}
