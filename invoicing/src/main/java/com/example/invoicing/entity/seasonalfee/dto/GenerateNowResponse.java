package com.example.invoicing.entity.seasonalfee.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class GenerateNowResponse {
    private Long configId;
    private Long billingEventId;
    private LocalDate eventDate;
    private BigDecimal amount;
    private LocalDate newNextDueDate;
    private String message;
}
