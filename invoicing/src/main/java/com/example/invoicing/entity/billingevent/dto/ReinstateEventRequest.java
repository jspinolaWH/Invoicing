package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReinstateEventRequest {

    @NotBlank(message = "A reason is required for reinstatement")
    private String reason;
}
