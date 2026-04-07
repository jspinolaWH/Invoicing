package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillingEventManualCreateRequest {

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

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Customer number must be 6-9 digits")
    private String customerNumber;

    private String vehicleId;
    private String driverId;
    private String locationId;
    private String municipalityId;
    private String comments;
}
