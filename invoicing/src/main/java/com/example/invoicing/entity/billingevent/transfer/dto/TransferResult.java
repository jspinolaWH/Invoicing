package com.example.invoicing.entity.billingevent.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferResult {
    private Long eventId;
    private String sourceCustomerNumber;
    private String targetCustomerNumber;
    private boolean success;
    private String failureReason;
}
