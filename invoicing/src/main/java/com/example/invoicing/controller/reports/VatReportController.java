package com.example.invoicing.controller.reports;

import com.example.invoicing.controller.reports.dto.VatReportResponse;
import com.example.invoicing.service.VatReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports/vat")
@RequiredArgsConstructor
public class VatReportController {

    private final VatReportService vatReportService;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping
    public VatReportResponse generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return vatReportService.generate(from, to);
    }
}
