package com.example.invoicing.entity.minimumfee.dto;

import com.example.invoicing.entity.minimumfee.PeriodType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class MinimumFeeConfigResponse {
    private Long id;
    private String customerType;
    private BigDecimal netAmountThreshold;
    private PeriodType periodType;
    private boolean contractStartAdjustment;
    private boolean contractEndAdjustment;
    private String adjustmentProductCode;
    private String description;
    private boolean active;
    private Instant createdAt;
    private String createdBy;
}
