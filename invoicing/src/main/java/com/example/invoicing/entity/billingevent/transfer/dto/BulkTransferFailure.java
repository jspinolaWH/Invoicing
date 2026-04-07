package com.example.invoicing.entity.billingevent.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkTransferFailure {
    private Long eventId;
    private String reason;
}
