package com.example.invoicing.retroactive;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AffectedEventEntry {
    private Long eventId;
    private LocalDate eventDate;
    private String productCode;
    private BigDecimal currentWasteFeePrice;
    private BigDecimal currentTransportFeePrice;
    private BigDecimal currentEcoFeePrice;
    private BigDecimal projectedWasteFeePrice;
    private BigDecimal projectedTransportFeePrice;
    private BigDecimal projectedEcoFeePrice;
    private BigDecimal quantity;
    private BigDecimal currentNetAmount;
    private BigDecimal projectedNetAmount;
    private BigDecimal delta;
    private String status;
}
