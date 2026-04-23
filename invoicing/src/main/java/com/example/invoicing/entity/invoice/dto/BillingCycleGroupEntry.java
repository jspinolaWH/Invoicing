package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class BillingCycleGroupEntry {
    private String frequency;
    private String serviceType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int customerCount;
    private int eventCount;
    private BigDecimal netAmount;
}
