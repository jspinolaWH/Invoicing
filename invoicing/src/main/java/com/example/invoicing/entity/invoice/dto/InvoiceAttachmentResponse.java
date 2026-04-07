package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.time.Instant;

@Data @Builder
public class InvoiceAttachmentResponse {
    private Long id;
    private Long invoiceId;
    private String attachmentIdentifier;
    private String filename;
    private String mimeType;
    private long sizeBytes;
    private String securityClass;
    private Instant createdAt;
}
