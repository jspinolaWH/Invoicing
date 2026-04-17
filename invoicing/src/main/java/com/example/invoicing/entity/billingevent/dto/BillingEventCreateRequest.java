package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.classification.LegalClassification;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillingEventCreateRequest {

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long productId;

    @NotNull @DecimalMin("0.00")
    private BigDecimal wasteFeePrice;

    @NotNull @DecimalMin("0.00")
    private BigDecimal transportFeePrice;

    @NotNull @DecimalMin("0.00")
    private BigDecimal ecoFeePrice;

    @NotNull @DecimalMin("0.00")
    private BigDecimal quantity;

    @NotNull @DecimalMin("0.00")
    private BigDecimal weight;

    private BigDecimal vatRate0;
    private BigDecimal vatRate24;

    private String vehicleId;
    private String driverId;

    private Long costCenterId;
    private Long accountingAccountId;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Customer number must be 6-9 digits")
    private String customerNumber;

    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private String comments;
    private String projectId;
    private LegalClassification legalClassification;
    private String wasteType;
    private String receivingSite;
}
