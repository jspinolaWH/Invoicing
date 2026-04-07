package com.example.invoicing.run;

import lombok.Getter;
import java.time.Instant;

@Getter
public class CustomerLockedException extends RuntimeException {
    private final String customerNumber;
    private final Long runId;
    private final Instant lockedAt;

    public CustomerLockedException(String customerNumber, Long runId, Instant lockedAt) {
        super("Invoice processing in progress. Address changes cannot be made during this time.");
        this.customerNumber = customerNumber;
        this.runId = runId;
        this.lockedAt = lockedAt;
    }
}
