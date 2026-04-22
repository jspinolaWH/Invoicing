package com.example.invoicing.controller.reports.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class VatReportResponse {
    LocalDate from;
    LocalDate to;
    BigDecimal standardVatTotal;
    BigDecimal reverseChargeBaseTotal;
    long reverseChargeInvoiceCount;
}
