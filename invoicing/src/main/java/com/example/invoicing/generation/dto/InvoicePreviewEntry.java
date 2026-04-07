package com.example.invoicing.generation.dto;

import com.example.invoicing.invoice.dto.InvoiceLineItemResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class InvoicePreviewEntry {
    private Long customerId;
    private String customerName;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private int lineItemCount;
    private List<InvoiceLineItemResponse> lineItems;
}
