package com.example.invoicing.sharedservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder
public class ValidationResultResponse {
    private Long groupId;
    private BigDecimal totalSharePercentage;
    private boolean valid;
    private String message;
}
