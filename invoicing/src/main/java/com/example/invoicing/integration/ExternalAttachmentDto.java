package com.example.invoicing.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalAttachmentDto {
    private String filename;
    private String mimeType;
    private String contentBase64;
    private long sizeBytes;
    private String attachmentIdentifier;
    private String description;
}
