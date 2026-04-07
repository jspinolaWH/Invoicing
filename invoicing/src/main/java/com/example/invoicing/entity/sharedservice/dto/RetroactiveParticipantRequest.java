package com.example.invoicing.entity.sharedservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RetroactiveParticipantRequest {
    private String customerNumber;
    private BigDecimal sharePercentage;
    private LocalDate validFrom;
    private boolean adjustOtherParticipants;
}
