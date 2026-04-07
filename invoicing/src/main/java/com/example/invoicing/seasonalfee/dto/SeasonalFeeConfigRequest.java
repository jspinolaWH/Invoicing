package com.example.invoicing.seasonalfee.dto;

import com.example.invoicing.billingcycle.BillingFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SeasonalFeeConfigRequest {
    @NotBlank
    private String customerNumber;
    @NotNull
    private Long productId;
    @NotNull
    private BillingFrequency billingFrequency;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDate nextDueDate;
    private String propertyReference;
    private String description;
    private boolean active = true;
}
