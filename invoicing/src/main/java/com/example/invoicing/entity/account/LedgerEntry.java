package com.example.invoicing.entity.account;

import com.example.invoicing.entity.account.AccountingAccount;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Immutable value object produced by AccountingAllocationService.
 * Not persisted — assembled in memory during invoice generation.
 */
@Value
@Builder
public class LedgerEntry {
    Long billingEventId;
    AccountingAccount accountingAccount;
    BigDecimal amountNet;
    BigDecimal amountVat;
    BigDecimal amountGross;
    BigDecimal vatRate;
    PriceComponent priceComponent;
    String description;
    Long matchedRuleId;
}
