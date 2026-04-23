package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.billingevent.BillingEventDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillingEventDraftRequest {

    @NotNull
    private LocalDate eventDate;

    private Long productId;

    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Customer number must be 6-9 digits")
    private String customerNumber;

    private String vehicleId;
    private String driverId;
    private String locationId;
    private String municipalityId;
    private String comments;
    private String internalComments;
    private String registrationNumber;
    private String contractor;
    private BillingEventDirection direction;
    private BigDecimal sharedServiceGroupPercentage;
    private String wasteType;
    private String receivingSite;
}
