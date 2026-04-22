package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder
public class CostCentreAllocationEntry {
    private String costCentreCode;
    private String costCentreDescription;
    private int eventCount;
    private BigDecimal netAmount;
    private BigDecimal vatAmount;
}
