package com.example.invoicing.controller.reports;

import com.example.invoicing.controller.reports.dto.AccountingReportResponse;
import com.example.invoicing.service.AccountingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports/accounting")
@RequiredArgsConstructor
public class AccountingReportController {

    private final AccountingReportService accountingReportService;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping
    public AccountingReportResponse generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return accountingReportService.generate(from, to);
    }
}
