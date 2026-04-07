package com.example.invoicing.bundling.dto;

import com.example.invoicing.bundling.BundlingType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BundlingRuleResponse {
    private Long id;
    private String customerNumber;
    private String productGroup;
    private BundlingType bundlingType;
    private String description;
}
