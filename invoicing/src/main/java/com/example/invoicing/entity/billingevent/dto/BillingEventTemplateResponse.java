package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class BillingEventTemplateResponse {

    private Long id;
    private String name;
    private String customerNumber;
    private Long contractId;
    private Long productId;
    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;
    private String vehicleId;
    private String driverId;
    private String locationId;
    private String municipalityId;
    private String comments;
    private String contractor;
    private String direction;
    private BigDecimal sharedServiceGroupPercentage;
    private String wasteType;
    private String receivingSite;
    private String createdBy;
    private Instant createdAt;
}
