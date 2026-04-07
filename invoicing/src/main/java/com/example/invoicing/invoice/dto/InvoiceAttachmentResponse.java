package com.example.invoicing.invoice.dto;

import lombok.*;

@Data @Builder
public class InvoiceAttachmentResponse {
    private Long id;
    private String attachmentIdentifier;
    private String filename;
    private String mimeType;
    private long sizeBytes;
    private String securityClass;
}
