package com.example.invoicing.entity.invoice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class BillingEventForCorrectionDto {
    private Long id;
    private LocalDate eventDate;
    private String productName;
    private String customerNumber;
    private BigDecimal totalFees;
}
