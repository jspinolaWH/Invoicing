package com.example.invoicing.responsibility;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResponsibilityChangePreview {
    private String fromCustomerNumber;
    private String toCustomerNumber;
    private int totalEventCount;
    private int inProgressCount;
    private int billedCount;
    private BigDecimal totalTransferAmount;
    private List<AffectedEventSummary> events;
}
