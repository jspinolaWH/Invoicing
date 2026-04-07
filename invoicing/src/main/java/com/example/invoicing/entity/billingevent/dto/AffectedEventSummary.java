package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AffectedEventSummary {
    private Long eventId;
    private LocalDate eventDate;
    private String productCode;
    private BigDecimal quantity;
    private BigDecimal totalFeeAmount;
    private String status;
}
