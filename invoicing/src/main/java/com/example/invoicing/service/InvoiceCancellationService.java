package com.example.invoicing.service;
import com.example.invoicing.common.exception.CannotCancelException;
import com.example.invoicing.entity.invoicerun.dto.CancellationResult;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.service.InvoiceRunLockService;
import com.example.invoicing.repository.InvoiceRunRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceCancellationService {

    private final InvoiceRunRepository runRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberSeriesRepository numberSeriesRepository;
    private final InvoiceRunLockService lockService;

    public CancellationResult cancelRun(Long runId, String reason, String cancelledBy) {
        InvoiceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("InvoiceRun not found: " + runId));

        if (run.getStatus() == InvoiceRunStatus.SENT) {
            throw new CannotCancelException(
                "Run has already been fully transmitted. Use recall for individual invoices.");
        }

        List<Invoice> invoices = invoiceRepository.findByInvoiceRunId(runId);
        int cancelledCount = 0;
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.SENT ||
                invoice.getStatus() == InvoiceStatus.COMPLETED) {
                log.warn("Invoice {} already SENT — cannot auto-cancel; manual recall required",
                    invoice.getId());
                continue;
            }
            releaseInvoiceNumber(invoice);
            invoice.setStatus(InvoiceStatus.CANCELLED);
            invoiceRepository.save(invoice);
            cancelledCount++;
        }

        lockService.releaseLocksForRun(runId);

        run.setStatus(InvoiceRunStatus.CANCELLED);
        run.setCancellationReason(reason);
        run.setCancelledBy(cancelledBy);
        run.setCancelledAt(Instant.now());
        runRepository.save(run);

        return new CancellationResult(runId, cancelledCount, "Run cancelled successfully");
    }

    public void cancelInvoice(Long invoiceId, String reason, String cancelledBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.SENT ||
            invoice.getStatus() == InvoiceStatus.COMPLETED) {
            throw new CannotCancelException(
                "Invoice has been transmitted. Use /recall endpoint to retract from external system.");
        }

        releaseInvoiceNumber(invoice);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }

    private void releaseInvoiceNumber(Invoice invoice) {
        if (invoice.getInvoiceNumber() != null && invoice.getInvoiceNumberSeries() != null) {
            InvoiceNumberSeries series = numberSeriesRepository
                .findById(invoice.getInvoiceNumberSeries().getId())
                .orElse(null);
            if (series != null) {
                series.getReleasedNumbers().add(invoice.getInvoiceNumber());
                numberSeriesRepository.save(series);
            }
        }
    }
}
