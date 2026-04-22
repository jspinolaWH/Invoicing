package com.example.invoicing.entity.customer.dto;

import lombok.*;

@Getter @Builder
public class EInvoiceOrderIngestionResult {
    private String status;
    private Long customerId;
    private String customerNumber;
    private String matchedBy;
    private String message;
}
