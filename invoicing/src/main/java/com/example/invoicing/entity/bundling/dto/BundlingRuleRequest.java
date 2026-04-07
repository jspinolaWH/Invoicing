package com.example.invoicing.entity.bundling.dto;

import com.example.invoicing.entity.bundling.BundlingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BundlingRuleRequest {
    @NotBlank
    private String productGroup;
    @NotNull
    private BundlingType bundlingType;
    private String description;
}
