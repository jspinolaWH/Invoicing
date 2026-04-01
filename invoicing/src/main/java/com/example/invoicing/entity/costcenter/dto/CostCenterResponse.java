package com.example.invoicing.entity.costcenter.dto;

import com.example.invoicing.entity.costcenter.CostCenter;
import lombok.Data;

import java.time.Instant;

@Data
public class CostCenterResponse {
    private Long id;
    private String productSegment;
    private String receptionSegment;
    private String responsibilitySegment;
    private String compositeCode;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static CostCenterResponse from(CostCenter c) {
        CostCenterResponse r = new CostCenterResponse();
        r.id = c.getId();
        r.productSegment = c.getProductSegment();
        r.receptionSegment = c.getReceptionSegment();
        r.responsibilitySegment = c.getResponsibilitySegment();
        r.compositeCode = c.getCompositeCode();
        r.description = c.getDescription();
        r.createdBy = c.getCreatedBy();
        r.createdAt = c.getCreatedAt();
        r.lastModifiedBy = c.getLastModifiedBy();
        r.lastModifiedAt = c.getLastModifiedAt();
        return r;
    }
}
