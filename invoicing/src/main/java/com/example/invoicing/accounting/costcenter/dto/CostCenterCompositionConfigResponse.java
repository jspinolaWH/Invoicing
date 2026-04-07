package com.example.invoicing.accounting.costcenter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CostCenterCompositionConfigResponse {
    private Long id;
    private String separator;
    private String segmentOrder;
    private boolean productSegmentEnabled;
    private boolean receptionPointSegmentEnabled;
    private boolean serviceResponsibilitySegmentEnabled;
    private String publicLawCode;
    private String privateLawCode;
}
