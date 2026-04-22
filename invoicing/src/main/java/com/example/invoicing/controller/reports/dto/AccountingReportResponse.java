package com.example.invoicing.controller.reports.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class AccountingReportResponse {
    LocalDate from;
    LocalDate to;
    List<AccountLine> byAccount;
    List<CostCenterLine> byCostCenter;

    @Value
    @Builder
    public static class AccountLine {
        String accountCode;
        String accountName;
        BigDecimal totalNetAmount;
        long lineItemCount;
    }

    @Value
    @Builder
    public static class CostCenterLine {
        String compositeCode;
        String description;
        BigDecimal totalNetAmount;
        long lineItemCount;
    }
}
