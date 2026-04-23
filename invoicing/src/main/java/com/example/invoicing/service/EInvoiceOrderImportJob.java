package com.example.invoicing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * This system uses a push-only delivery model for e-invoice and direct debit orders.
 * The external billing system posts orders directly to:
 *   POST /api/v1/integration/einvoice-orders
 *   POST /api/v1/integration/direct-debit-orders
 * No scheduled pull job is required.
 */
@Slf4j
public class EInvoiceOrderImportJob {
    // Push-only model — no scheduled job needed.
}
