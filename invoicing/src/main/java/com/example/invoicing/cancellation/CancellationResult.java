package com.example.invoicing.cancellation;

import lombok.*;

@Data @AllArgsConstructor
public class CancellationResult {
    private Long runId;
    private int invoicesCancelled;
    private String message;
}
