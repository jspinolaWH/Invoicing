package com.example.invoicing.entity.billingevent.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PriceAdjustmentRequest {
    private String customerNumber;
    private LocalDate eventDateFrom;
    private LocalDate eventDateTo;
    private Long productId;
    private BigDecimal newWasteFeePrice;
    private BigDecimal newTransportFeePrice;
    private BigDecimal newEcoFeePrice;
    private String reason;
    private String internalComment;
}
