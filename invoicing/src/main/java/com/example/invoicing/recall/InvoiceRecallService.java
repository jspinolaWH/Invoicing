package com.example.invoicing.recall;

import com.example.invoicing.integration.ExternalInvoicingClient;
import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceRepository;
import com.example.invoicing.invoice.InvoiceStatus;
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
