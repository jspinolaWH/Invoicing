package com.example.invoicing.integration;

import lombok.Data;

@Data
public class OperatorAttachmentResponse {
    private String filename;
    private String mimeType;
    private String content;
    private long size;
    private String attachmentIdentifier;
    private String description;
}
