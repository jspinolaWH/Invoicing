package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.billingevent.BillingEventDirection;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillingEventUpdateRequest {

    @NotBlank(message = "A reason is required for every edit")
    private String reason;

    private LocalDate eventDate;
    private Long productId;
    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;
    private String vehicleId;
    private String driverId;
    private String customerNumber;
    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private BillingEventDirection direction;
    private String comments;
}
