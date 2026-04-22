package com.example.invoicing.entity.weighbridge.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class WeighbridgeConfigRequest {
    @NotBlank @Pattern(regexp = "\\d{6,9}", message = "customerNumber must be 6-9 digits")
    private String customerNumber;

    @Size(max = 100)
    private String externalSystemId;

    @Size(max = 100)
    private String defaultProductCode;

    @Size(max = 200)
    private String siteReference;
}
