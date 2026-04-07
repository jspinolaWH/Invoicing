package com.example.invoicing.entity.invoice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CorrectionRequest {
    private Long targetCustomerId;
    private List<Long> lineItemIds;
    private List<Long> billingEventIds;   // IDs of billing events to copy (since BillingEvent has no FK to Invoice)
    private String customText;
    private String internalComment;
}
