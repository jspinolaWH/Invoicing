package com.example.invoicing.accounting.vat;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class VatCalculationResult {
    Long billingEventId;
    String vatRateCode;
    BigDecimal effectiveRatePercent;
    boolean reverseCharge;
    BigDecimal amountNet;
    BigDecimal amountVat;
    BigDecimal amountGross;
    String buyerVatNumber;
    String reverseChargeNoticeText;
}
