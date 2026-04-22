package com.example.invoicing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Daily scheduled job to import e-invoice and direct debit order changes from
 * the external billing system. The actual fetch/push of order data is handled
 * by EInvoiceOrderIngestionService once orders are received; this job
 * triggers any pull-based polling or file-drop ingestion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EInvoiceOrderImportJob {

    private final EInvoiceOrderIngestionService ingestionService;

    @Scheduled(cron = "0 0 3 * * *") // daily at 03:00, after the operator batch at 02:00
    public void runDailyImport() {
        log.info("Starting daily e-invoice/direct-debit order import from billing system");
        try {
            importPendingOrders();
            log.info("Daily e-invoice/direct-debit order import completed");
        } catch (Exception ex) {
            log.error("Daily e-invoice/direct-debit order import failed", ex);
        }
    }

    public void triggerManually() {
        log.info("Manual trigger: e-invoice/direct-debit order import");
        runDailyImport();
    }

    private void importPendingOrders() {
        // Placeholder for pull-based polling or file-drop ingestion.
        // In production: call the billing intermediary API, parse the response,
        // and invoke ingestionService.ingestEInvoiceOrder / ingestDirectDebitOrder
        // for each received order.
        log.info("importPendingOrders: no external source configured — awaiting push via POST /api/v1/integration/einvoice-orders");
    }
}
