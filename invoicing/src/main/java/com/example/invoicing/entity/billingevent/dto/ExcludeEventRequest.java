package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExcludeEventRequest {

    @NotBlank(message = "Exclusion reason is required")
    private String exclusionReason;
}
