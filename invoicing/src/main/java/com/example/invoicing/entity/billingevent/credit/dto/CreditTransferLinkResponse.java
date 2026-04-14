package com.example.invoicing.entity.billingevent.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CreditTransferLinkResponse {
    private Long originalEventId;
    private Long creditEventId;
    private Long newEventId;
    private String performedBy;
    private Instant performedAt;
    private String reason;
}
