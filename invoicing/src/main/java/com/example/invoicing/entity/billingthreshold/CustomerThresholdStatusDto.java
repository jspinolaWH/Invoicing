package com.example.invoicing.entity.billingthreshold;

import java.math.BigDecimal;

public record CustomerThresholdStatusDto(
    String customerNumber,
    BigDecimal annualAmount,
    boolean exceeded,
    String triggerStatus,
    Long triggerId
) {}
