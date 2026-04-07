package com.example.invoicing.retroactive;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PriceAdjustmentPreview {
    private String customerNumber;
    private int totalEventCount;
    private int inProgressCount;
    private int billedCount;
    private BigDecimal totalCurrentNet;
    private BigDecimal totalProjectedNet;
    private BigDecimal totalDelta;
    private List<AffectedEventEntry> events;
}
