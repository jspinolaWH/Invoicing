package com.example.invoicing.retroactive;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PriceAdjustmentResult {
    private String customerNumber;
    private int updatedInProgressCount;
    private int correctionCopiesCreated;
    private int excludedCount;
    private BigDecimal totalDelta;
    private List<Long> createdEventIds;
    private String message;
}
