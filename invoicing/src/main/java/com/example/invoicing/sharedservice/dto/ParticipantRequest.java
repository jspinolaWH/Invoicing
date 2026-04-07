package com.example.invoicing.sharedservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ParticipantRequest {
    private String customerNumber;
    private BigDecimal sharePercentage;
    private LocalDate validFrom;
    private LocalDate validTo;
}
