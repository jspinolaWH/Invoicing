package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidationOverrideRequest {
    @NotBlank
    private String reason;
}
