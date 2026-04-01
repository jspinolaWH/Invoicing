package com.example.invoicing.entity.vat.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VatRateRequest {
    private String code;
    private BigDecimal rate;
    private LocalDate validFrom;
    private LocalDate validTo;
}
