package com.example.invoicing.entity.billingevent.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TransferEventRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Target customer number must be 6–9 digits")
    private String targetCustomerNumber;

    private String targetPropertyId;

    @NotBlank(message = "A reason is required for every transfer")
    private String reason;
}
