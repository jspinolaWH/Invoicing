package com.example.invoicing.responsibility;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ResponsibilityChangeRequest {
    private String fromCustomerNumber;
    private String toCustomerNumber;
    private LocalDate eventDateFrom;
    private LocalDate eventDateTo;
    private Long productId;
    private List<Long> specificEventIds;
    private String reason;
    private String internalComment;
}
