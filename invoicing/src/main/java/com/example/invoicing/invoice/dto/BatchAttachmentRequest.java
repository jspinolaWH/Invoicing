package com.example.invoicing.invoice.dto;

import lombok.*;

@Data
public class BatchAttachmentRequest {
    private String attachmentIdentifier;
    private String filename;
    private String mimeType;
    private String securityClass;
}
