package com.example.invoicing.entity.billingevent.credit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreditTransferRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6,9}", message = "Target customer number must be 6–9 digits")
    private String targetCustomerNumber;

    private String targetPropertyId;

    @NotBlank(message = "A reason is required for every credit & transfer")
    private String reason;
}
