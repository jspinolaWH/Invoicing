package com.example.invoicing.entity.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class DirectDebitOrderRequest {
    private String customerNumber;
    private String invoiceReference;
    private String invoiceNumber;
    @NotBlank @Size(max = 35) private String mandate;
    @NotBlank @Size(max = 50) private String bankAccount;
    private EInvoiceOrderType orderType;
}
