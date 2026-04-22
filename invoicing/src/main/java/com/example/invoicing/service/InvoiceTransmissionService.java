package com.example.invoicing.service;
import com.example.invoicing.integration.ExternalDeliveryStatus;
import com.example.invoicing.integration.TransmissionStatusResponse;
import com.example.invoicing.integration.ExternalAttachmentDto;
import com.example.invoicing.integration.ExternalTransmissionResult;
import com.example.invoicing.integration.ExternalInvoicingClient;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceTransmissionService {

    private final InvoiceRepository invoiceRepository;
    private final ExternalInvoicingClient externalClient;
    private final BillingEventRepository billingEventRepository;
    private final BillingEventStatusService billingEventStatusService;
    private final FinvoiceBuilderService finvoiceBuilderService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalTransmissionResult transmit(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.READY) {
            throw new IllegalStateException(
                "Invoice must be in READY status to transmit, current status: " + invoice.getStatus());
        }

        if (invoice.getFinvoiceXml() == null) {
            String xml = finvoiceBuilderService.build(invoice);
            invoice.setFinvoiceXml(xml);
            invoiceRepository.save(invoice);
        }

        try {
            ExternalTransmissionResult result = externalClient.transmit(invoice);
            invoice.setExternalReference(result.getExternalReference());
            invoice.setTransmittedAt(result.getTransmittedAt());
            invoice.setStatus(InvoiceStatus.SENT);
            invoiceRepository.save(invoice);

            transitionLinkedBillingEvents(invoiceId, BillingEventStatus.SENT, null);
            log.info("Invoice {} transmitted, externalRef={}", invoice.getInvoiceNumber(), result.getExternalReference());
            return result;
        } catch (Exception e) {
            transitionLinkedBillingEvents(invoiceId, BillingEventStatus.ERROR, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public TransmissionStatusResponse fetchStatus(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        ExternalDeliveryStatus deliveryStatus = invoice.getExternalReference() != null
            ? externalClient.fetchDeliveryStatus(invoice.getExternalReference())
            : ExternalDeliveryStatus.PENDING;

        if (deliveryStatus == ExternalDeliveryStatus.DELIVERED) {
            transitionLinkedBillingEvents(invoiceId, BillingEventStatus.COMPLETED, null);
        }

        return TransmissionStatusResponse.builder()
            .invoiceId(invoiceId)
            .invoiceNumber(invoice.getInvoiceNumber())
            .externalReference(invoice.getExternalReference())
            .deliveryStatus(deliveryStatus)
            .transmittedAt(invoice.getTransmittedAt())
            .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalTransmissionResult retransmit(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.SENT) {
            throw new IllegalStateException(
                "Only SENT invoices can be retransmitted, current status: " + invoice.getStatus());
        }

        String xml = finvoiceBuilderService.build(invoice);
        invoice.setFinvoiceXml(xml);
        invoiceRepository.save(invoice);

        try {
            ExternalTransmissionResult result = externalClient.transmit(invoice);
            invoice.setExternalReference(result.getExternalReference());
            invoice.setTransmittedAt(result.getTransmittedAt());
            invoiceRepository.save(invoice);
            log.info("Invoice {} retransmitted with updated billing address, externalRef={}", invoice.getInvoiceNumber(), result.getExternalReference());
            return result;
        } catch (Exception e) {
            log.error("Retransmission failed for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void confirmDelivery(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));
        transitionLinkedBillingEvents(invoice.getId(), BillingEventStatus.COMPLETED, null);
        log.info("Invoice {} delivery confirmed, billing events transitioned to COMPLETED", invoice.getInvoiceNumber());
    }

    private void transitionLinkedBillingEvents(Long invoiceId, BillingEventStatus targetStatus, String reason) {
        List<BillingEvent> events = billingEventRepository.findByInvoiceId(invoiceId);
        for (BillingEvent event : events) {
            try {
                billingEventStatusService.transitionTo(event, targetStatus, reason);
            } catch (IllegalStateException ex) {
                log.warn("Could not transition billing event {} to {}: {}", event.getId(), targetStatus, ex.getMessage());
            }
        }
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
