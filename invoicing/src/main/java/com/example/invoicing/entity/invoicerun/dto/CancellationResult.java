package com.example.invoicing.entity.invoicerun.dto;

import lombok.*;

@Data @AllArgsConstructor
public class CancellationResult {
    private Long runId;
    private int invoicesCancelled;
    private String message;
}
