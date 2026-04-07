package com.example.invoicing.accounting.costcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CostCenterCompositionConfigRequest {

    @NotBlank
    private String separator;

    @NotBlank
    private String segmentOrder;

    private boolean productSegmentEnabled = true;
    private boolean receptionPointSegmentEnabled = true;
    private boolean serviceResponsibilitySegmentEnabled = true;

    private String publicLawCode = "PL";
    private String privateLawCode = "PR";
}
