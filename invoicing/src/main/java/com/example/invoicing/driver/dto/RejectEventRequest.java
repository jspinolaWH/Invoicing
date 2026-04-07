package com.example.invoicing.driver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectEventRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
