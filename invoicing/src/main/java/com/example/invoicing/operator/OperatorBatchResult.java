package com.example.invoicing.operator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperatorBatchResult {
    private int startedCount;
    private int terminatedCount;
    private int failedCount;
    private String message;
}
