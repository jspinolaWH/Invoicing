package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ResponsibilityChangeResult {
    private String fromCustomerNumber;
    private String toCustomerNumber;
    private int movedInProgressCount;
    private int correctionCopiesCreated;
    private int excludedCount;
    private List<Long> createdEventIds;
    private String message;
}
