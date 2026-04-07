package com.example.invoicing.billingcycle.dto;

import com.example.invoicing.billingcycle.BillingFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BillingCycleRequest {
    @NotBlank
    private String customerNumber;
    @NotNull
    private BillingFrequency frequency;
    @NotNull
    private LocalDate nextBillingDate;
    private String description;
    private String contractReference;
    private String propertyReference;
    private String serviceType;
    private boolean active = true;
}
