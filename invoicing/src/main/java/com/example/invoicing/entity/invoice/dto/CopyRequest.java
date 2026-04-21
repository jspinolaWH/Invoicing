package com.example.invoicing.entity.invoice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CopyRequest {
    private Long targetCustomerId;
    private String targetPropertyId;
    private List<Long> billingEventIds;
    private String internalComment;
}
