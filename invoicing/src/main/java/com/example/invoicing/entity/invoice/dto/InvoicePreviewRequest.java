package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvoicePreviewRequest {
    private Long customerId;
    private LocalDate billingPeriodFrom;
    private LocalDate billingPeriodTo;
    private List<Long> eventIds;
}
