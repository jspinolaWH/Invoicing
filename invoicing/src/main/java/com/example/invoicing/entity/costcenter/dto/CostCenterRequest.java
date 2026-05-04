package com.example.invoicing.entity.costcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CostCenterRequest {
    @NotBlank private String productSegment;
    @NotBlank private String receptionSegment;
    @NotBlank private String responsibilitySegment;
    private String description;
}
