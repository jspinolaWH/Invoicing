package com.example.invoicing.service;

import com.example.invoicing.controller.reports.dto.VatReportResponse;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.InvoiceRepository;
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
public class VatReportService {

    private final InvoiceRepository invoiceRepository;

    public VatReportResponse generate(LocalDate from, LocalDate to) {
        List<Invoice> invoices = invoiceRepository.findForVatReport(from, to);

        BigDecimal standardVatTotal = invoices.stream()
            .filter(i -> !i.isReverseChargeVat())
            .map(i -> i.getVatAmount() != null ? i.getVatAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal reverseChargeBaseTotal = invoices.stream()
            .filter(Invoice::isReverseChargeVat)
            .map(i -> i.getNetAmount() != null ? i.getNetAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        long reverseChargeCount = invoices.stream()
            .filter(Invoice::isReverseChargeVat)
            .count();

        return VatReportResponse.builder()
            .from(from)
            .to(to)
            .standardVatTotal(standardVatTotal)
            .reverseChargeBaseTotal(reverseChargeBaseTotal)
            .reverseChargeInvoiceCount(reverseChargeCount)
            .build();
    }
}
