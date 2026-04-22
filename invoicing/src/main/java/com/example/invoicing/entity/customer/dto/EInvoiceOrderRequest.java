package com.example.invoicing.entity.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class EInvoiceOrderRequest {
    private String customerNumber;
    private String invoiceReference;
    private String invoiceNumber;
    @NotBlank @Size(max = 35) private String einvoiceAddress;
    @Size(max = 20) private String operatorCode;
    private EInvoiceOrderType orderType;
}
