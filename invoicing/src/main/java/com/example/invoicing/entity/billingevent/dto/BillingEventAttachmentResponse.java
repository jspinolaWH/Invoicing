package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder
public class BillingEventAttachmentResponse {
    private Long id;
    private Long billingEventId;
    private String fileName;
    private long fileSize;
    private String contentType;
    private Instant createdAt;
    private String createdBy;
}
