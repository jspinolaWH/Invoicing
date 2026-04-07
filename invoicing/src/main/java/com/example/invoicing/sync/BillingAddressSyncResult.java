package com.example.invoicing.sync;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingAddressSyncResult {
    private Long customerId;
    private String customerNumber;
    private String status;   // UPDATED, SKIPPED_LOCKED, NOT_FOUND
    private String message;
}
