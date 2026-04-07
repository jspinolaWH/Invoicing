package com.example.invoicing.entity.invoice.dto;

import com.example.invoicing.entity.invoice.CreditType;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.invoice.dto.InvoiceLineItemResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class CreditNoteResponse {
    private Long creditNoteId;
    private String creditNoteNumber;
    private Long originalInvoiceId;
    private String originalInvoiceNumber;
    private CreditType creditType;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private String status;
    private List<InvoiceLineItemResponse> lineItems;

    public static CreditNoteResponse from(Invoice creditNote, Invoice original, CreditType creditType) {
        List<InvoiceLineItemResponse> lines = creditNote.getLineItems().stream()
            .map(li -> InvoiceLineItemResponse.builder()
                .id(li.getId())
                .description(li.getDescription())
                .quantity(li.getQuantity())
                .unitPrice(li.getUnitPrice())
                .vatRate(li.getVatRate())
                .netAmount(li.getNetAmount())
                .grossAmount(li.getGrossAmount())
                .legalClassification(li.getLegalClassification() != null ? li.getLegalClassification().name() : null)
                .build())
            .toList();

        return CreditNoteResponse.builder()
            .creditNoteId(creditNote.getId())
            .creditNoteNumber(creditNote.getInvoiceNumber())
            .originalInvoiceId(original.getId())
            .originalInvoiceNumber(original.getInvoiceNumber())
            .creditType(creditType)
            .netAmount(creditNote.getNetAmount())
            .grossAmount(creditNote.getGrossAmount())
            .status(creditNote.getStatus() != null ? creditNote.getStatus().name() : null)
            .lineItems(lines)
            .build();
    }
}
