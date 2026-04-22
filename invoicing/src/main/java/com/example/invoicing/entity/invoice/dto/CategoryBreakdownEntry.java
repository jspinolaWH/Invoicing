package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder
public class CategoryBreakdownEntry {
    private String category;
    private int eventCount;
    private BigDecimal netAmount;
}
