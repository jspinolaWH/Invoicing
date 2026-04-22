package com.example.invoicing.entity.weighbridge.dto;

import com.example.invoicing.entity.weighbridge.WeighbridgeIntegrationConfig;
import lombok.*;

@Data @Builder
public class WeighbridgeConfigResponse {
    private Long id;
    private String customerNumber;
    private String externalSystemId;
    private String defaultProductCode;
    private String siteReference;
    private boolean active;

    public static WeighbridgeConfigResponse from(WeighbridgeIntegrationConfig c) {
        return WeighbridgeConfigResponse.builder()
            .id(c.getId())
            .customerNumber(c.getCustomerNumber())
            .externalSystemId(c.getExternalSystemId())
            .defaultProductCode(c.getDefaultProductCode())
            .siteReference(c.getSiteReference())
            .active(c.isActive())
            .build();
    }
}
