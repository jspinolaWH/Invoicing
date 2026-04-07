package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder
public class InvoiceLineItemResponse {
    private Long id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private String legalClassification;
    private String accountingAccountCode;
    private String costCenterCode;
    private boolean bundled;
    private Integer lineOrder;
}
