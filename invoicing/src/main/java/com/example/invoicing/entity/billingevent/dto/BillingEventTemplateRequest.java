package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillingEventTemplateRequest {

    @NotBlank
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
}
