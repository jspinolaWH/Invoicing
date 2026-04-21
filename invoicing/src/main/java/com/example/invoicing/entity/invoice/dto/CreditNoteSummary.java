package com.example.invoicing.entity.invoice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class CreditNoteSummary {
    private Long creditNoteId;
    private String creditNoteNumber;
    private LocalDate issuedAt;
    private BigDecimal netAmount;
}
