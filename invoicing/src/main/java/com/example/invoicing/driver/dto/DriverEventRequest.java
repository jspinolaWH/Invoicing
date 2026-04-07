package com.example.invoicing.driver.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DriverEventRequest {

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private Long productId;

    @NotBlank
    private String eventTypeCode;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal wasteFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal transportFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal ecoFeePrice;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal weight;

    @NotBlank
    private String vehicleId;

    @NotBlank
    @Pattern(regexp = "\\d{6,9}")
    private String customerNumber;

    private String locationId;
    private String municipalityId;
    private String comments;
}
