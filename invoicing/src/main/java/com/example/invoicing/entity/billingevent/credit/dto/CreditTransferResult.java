package com.example.invoicing.entity.billingevent.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreditTransferResult {
    private Long originalEventId;
    private Long creditEventId;
    private Long newEventId;
    private String reason;
}
