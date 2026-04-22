package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractorPaymentRequest {

    @NotBlank(message = "Status is required (PAID or NOT_REQUIRED)")
    private String status;

    private String notes;
}
