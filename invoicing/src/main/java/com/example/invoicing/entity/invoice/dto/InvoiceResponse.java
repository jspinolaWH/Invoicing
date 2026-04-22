package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String templateCode;
    private String language;
    private String invoicingMode;
    private boolean reverseChargeVat;
    private String customText;
    private String internalComment;
    private String status;
    private String invoiceType;
    private Long originalInvoiceId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private BigDecimal vatAmount;
    private Long customerId;
    private String customerName;
    private boolean allowExternalRecall;
    private String billingType;
    private String projectReference;
    private List<InvoiceLineItemResponse> lineItems;
    private List<InvoiceAttachmentResponse> attachments;
    private List<CreditNoteSummary> creditNotes;
}
