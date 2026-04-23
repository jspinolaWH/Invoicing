package com.example.invoicing.entity.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BillingAddressSyncResult {
    private Long customerId;
    private String customerNumber;
    private String status;   // UPDATED, SKIPPED_LOCKED, NOT_FOUND
    private String message;
    private int openInvoicesUpdated;
    private int pendingRemindersUpdated;
    private int sentInvoicesIncluded;
    private List<String> sentInvoiceNumbers;
}
