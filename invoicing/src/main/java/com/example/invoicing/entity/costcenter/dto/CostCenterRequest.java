package com.example.invoicing.entity.costcenter.dto;

import lombok.Data;

@Data
public class CostCenterRequest {
    private String productSegment;
    private String receptionSegment;
    private String responsibilitySegment;
    private String description;
}
