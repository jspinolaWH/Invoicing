package com.example.invoicing.entity.invoice.dto;

import lombok.*;

@Data @Builder
public class ValidationFailureEntry {
    private Long customerId;
    private String customerName;
    private String ruleType;
    private String severity;
    private String description;
}
