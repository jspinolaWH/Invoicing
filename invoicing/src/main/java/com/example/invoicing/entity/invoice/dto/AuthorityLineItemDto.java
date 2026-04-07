package com.example.invoicing.entity.invoice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AuthorityLineItemDto {
    private Long id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private String legalClassification;
}
