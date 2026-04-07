package com.example.invoicing.entity.invoice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only invoice view for AUTHORITY_VIEWER role.
 * Intentionally excludes internal fields: internalComment, finvoiceXml, externalReference.
 */
@Data
@Builder
public class AuthorityInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private BigDecimal vatAmount;
    private String invoiceType;
    private String status;
    private Long customerId;
    private String customerName;
    private List<AuthorityLineItemDto> lineItems;
}
