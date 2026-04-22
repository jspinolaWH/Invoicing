package com.example.invoicing.service;
import com.example.invoicing.common.exception.CannotRecallException;
import com.example.invoicing.entity.invoice.dto.RecallResult;
import com.example.invoicing.entity.invoice.dto.RecallRequest;

import com.example.invoicing.integration.ExternalInvoicingClient;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceRecallService {

    private final InvoiceRepository invoiceRepository;
    private final ExternalInvoicingClient externalClient;

    @Transactional
    public RecallResult recall(Long invoiceId, RecallRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.SENT) {
            throw new CannotRecallException(
                "Invoice cannot be recalled in status: " + invoice.getStatus()
                    + ". Only SENT invoices can be recalled.");
        }

        if (invoice.getCustomer() == null
                || invoice.getCustomer().getBillingProfile() == null
                || !invoice.getCustomer().getBillingProfile().isAllowExternalRecall()) {
            throw new CannotRecallException(
                "External recall is not permitted for this company. Enable 'Allow external recall' in the customer billing profile.");
        }

        boolean externalSuccess = false;
        if (invoice.getExternalReference() != null) {
            try {
                externalSuccess = externalClient.requestRecall(invoice.getExternalReference(), request.getReason());
            } catch (Exception ex) {
                log.warn("External recall request failed for invoice {}: {}", invoice.getInvoiceNumber(), ex.getMessage());
            }
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        if (request.getInternalComment() != null) {
            invoice.setInternalComment("RECALLED: " + request.getReason()
                + (request.getInternalComment() != null ? " — " + request.getInternalComment() : ""));
        }
        invoiceRepository.save(invoice);

        log.info("Invoice {} recalled, externalRecallSucceeded={}", invoice.getInvoiceNumber(), externalSuccess);

        return RecallResult.builder()
            .invoiceId(invoiceId)
            .invoiceNumber(invoice.getInvoiceNumber())
            .externalReference(invoice.getExternalReference())
            .status(invoice.getStatus().name())
            .recalledAt(Instant.now())
            .externalRecallSucceeded(externalSuccess)
            .message(externalSuccess
                ? "Invoice recalled successfully from operator"
                : "Invoice marked as cancelled locally; external recall may require manual follow-up")
            .build();
    }
}
