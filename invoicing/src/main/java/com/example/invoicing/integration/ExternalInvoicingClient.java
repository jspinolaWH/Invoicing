package com.example.invoicing.integration;

import com.example.invoicing.invoice.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Stub integration client for the external FINVOICE operator.
 * In production this would use HTTP/SFTP to transmit FINVOICE XML.
 */
@Component
@Slf4j
public class ExternalInvoicingClient {

    public ExternalTransmissionResult transmit(Invoice invoice) {
        String extRef = "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Transmitting invoice {} (finvoice={}) to external operator → ref={}",
            invoice.getInvoiceNumber(),
            invoice.getFinvoiceXml() != null ? "present" : "missing",
            extRef);
        return ExternalTransmissionResult.builder()
            .externalReference(extRef)
            .status(ExternalDeliveryStatus.PENDING)
            .transmittedAt(Instant.now())
            .operatorResponse("Accepted by operator stub")
            .build();
    }

    public ExternalDeliveryStatus fetchDeliveryStatus(String externalReference) {
        log.info("Fetching delivery status for external ref {}", externalReference);
        // Stub: always returns DELIVERED
        return ExternalDeliveryStatus.DELIVERED;
    }

    public byte[] fetchImage(String externalReference) {
        log.info("Fetching invoice image for external ref {}", externalReference);
        // Stub: return minimal PDF-like bytes
        String stubPdf = "%PDF-1.4 stub invoice image for " + externalReference;
        return stubPdf.getBytes();
    }

    public List<ExternalAttachmentDto> fetchAttachments(String externalReference) {
        log.info("Fetching attachments for external ref {}", externalReference);
        // Stub: return empty list (real system would return actual attachments)
        return List.of();
    }

    public boolean requestRecall(String externalReference, String reason) {
        log.info("Requesting recall of external ref {} — reason: {}", externalReference, reason);
        // Stub: always succeeds
        return true;
    }

    public boolean startOperatorRegistration(String einvoiceAddress, String operatorCode, String customerName) {
        log.info("START operator registration for address={}, operator={}, customer={}",
            einvoiceAddress, operatorCode, customerName);
        return true;
    }

    public boolean terminateOperatorRegistration(String einvoiceAddress, String operatorCode) {
        log.info("TERMINATE operator registration for address={}, operator={}", einvoiceAddress, operatorCode);
        return true;
    }
}
