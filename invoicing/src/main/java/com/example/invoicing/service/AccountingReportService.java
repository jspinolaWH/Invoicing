package com.example.invoicing.service;

import com.example.invoicing.controller.reports.dto.AccountingReportResponse;
import com.example.invoicing.repository.InvoiceLineItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingReportService {

    private final InvoiceLineItemRepository lineItemRepository;

    public AccountingReportResponse generate(LocalDate from, LocalDate to) {
        List<Object[]> accountRows = lineItemRepository.sumByAccount(from, to);
        List<Object[]> costCenterRows = lineItemRepository.sumByCostCenter(from, to);

        List<AccountingReportResponse.AccountLine> byAccount = accountRows.stream()
                .map(row -> AccountingReportResponse.AccountLine.builder()
                        .accountCode((String) row[0])
                        .accountName((String) row[1])
                        .totalNetAmount(((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP))
                        .lineItemCount((long) row[3])
                        .build())
                .toList();

        List<AccountingReportResponse.CostCenterLine> byCostCenter = costCenterRows.stream()
                .map(row -> AccountingReportResponse.CostCenterLine.builder()
                        .compositeCode((String) row[0])
                        .description((String) row[1])
                        .totalNetAmount(((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP))
                        .lineItemCount((long) row[3])
                        .build())
                .toList();

        return AccountingReportResponse.builder()
                .from(from)
                .to(to)
                .byAccount(byAccount)
                .byCostCenter(byCostCenter)
                .build();
    }
}
