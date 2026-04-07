package com.example.invoicing.billingcycle.dto;

import com.example.invoicing.billingcycle.BillingFrequency;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class BillingCycleResponse {
    private Long id;
    private String customerNumber;
    private BillingFrequency frequency;
    private LocalDate nextBillingDate;
    private String description;
    private String contractReference;
    private String propertyReference;
    private String serviceType;
    private boolean active;
    private Instant createdAt;
    private String createdBy;
}
