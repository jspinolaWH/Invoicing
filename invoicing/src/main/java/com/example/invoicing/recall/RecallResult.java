package com.example.invoicing.recall;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class RecallResult {
    private Long invoiceId;
    private String invoiceNumber;
    private String externalReference;
    private String status;
    private Instant recalledAt;
    private boolean externalRecallSucceeded;
    private String message;
}
