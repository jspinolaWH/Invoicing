package com.example.invoicing.common.exception;

public class AllocationRuleNotFoundException extends RuntimeException {

    public AllocationRuleNotFoundException(Long productId, String region) {
        super("No active allocation rule found for productId=" + productId
              + ", region=" + region
              + ". Add a rule at /api/v1/allocation-rules before running invoices.");
    }
}
