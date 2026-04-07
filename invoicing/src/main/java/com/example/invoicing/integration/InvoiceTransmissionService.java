package com.example.invoicing.integration;

import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceRepository;
import com.example.invoicing.invoice.InvoiceStatus;
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
public class InvoiceTransmissionService {

    private final InvoiceRepository invoiceRepository;
    private final ExternalInvoicingClient externalClient;

    @Transactional
    public ExternalTransmissionResult transmit(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.READY) {
            throw new IllegalStateException(
                "Invoice must be in READY status to transmit, current status: " + invoice.getStatus());
        }

        ExternalTransmissionResult result = externalClient.transmit(invoice);
        invoice.setExternalReference(result.getExternalReference());
        invoice.setTransmittedAt(result.getTransmittedAt());
        invoice.setStatus(InvoiceStatus.SENT);
        invoiceRepository.save(invoice);

        log.info("Invoice {} transmitted, externalRef={}", invoice.getInvoiceNumber(), result.getExternalReference());
        return result;
    }

    @Transactional(readOnly = true)
    public TransmissionStatusResponse fetchStatus(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        ExternalDeliveryStatus deliveryStatus = invoice.getExternalReference() != null
            ? externalClient.fetchDeliveryStatus(invoice.getExternalReference())
            : ExternalDeliveryStatus.PENDING;

        return TransmissionStatusResponse.builder()
            .invoiceId(invoiceId)
            .invoiceNumber(invoice.getInvoiceNumber())
            .externalReference(invoice.getExternalReference())
            .deliveryStatus(deliveryStatus)
            .transmittedAt(invoice.getTransmittedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public byte[] fetchImage(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.getExternalReference() == null) {
            throw new IllegalStateException("Invoice has not been transmitted yet");
        }
        return externalClient.fetchImage(invoice.getExternalReference());
    }

    @Transactional(readOnly = true)
    public List<ExternalAttachmentDto> fetchExternalAttachments(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.getExternalReference() == null) {
            throw new IllegalStateException("Invoice has not been transmitted yet");
        }
        return externalClient.fetchAttachments(invoice.getExternalReference());
    }
}
