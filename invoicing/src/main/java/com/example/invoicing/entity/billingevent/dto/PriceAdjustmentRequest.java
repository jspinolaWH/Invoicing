package com.example.invoicing.entity.billingevent.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PriceAdjustmentRequest {
    private String customerNumber;
    private LocalDate eventDateFrom;
    private LocalDate eventDateTo;
    private Long productId;
    private String serviceResponsibility;
    private List<Long> eventIds;

    private AdjustmentType adjustmentType = AdjustmentType.FIXED;
    private BigDecimal adjustmentValue;

    private BigDecimal newWasteFeePrice;
    private BigDecimal newTransportFeePrice;
    private BigDecimal newEcoFeePrice;

    private String performedBy;
    private String reason;
    private String internalComment;
}
