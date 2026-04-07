package com.example.invoicing.minimumfee.dto;

import com.example.invoicing.minimumfee.PeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MinimumFeeConfigRequest {
    @NotBlank
    private String customerType;
    @NotNull
    private BigDecimal netAmountThreshold;
    @NotNull
    private PeriodType periodType;
    private boolean contractStartAdjustment = true;
    private boolean contractEndAdjustment = true;
    @NotBlank
    private String adjustmentProductCode;
    private String description;
    private boolean active = true;
}
