package com.example.invoicing.entity.reporting.dto;

import lombok.Data;

@Data
public class ReportingFieldConfigDto {
    private Long id;
    private Long companyId;
    private String fieldName;
    private String labelOverride;
    private boolean enabled;
    private int displayOrder;
}
