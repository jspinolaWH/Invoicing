package com.example.invoicing.controller.invoice;

import com.example.invoicing.integration.ExternalDeliveryStatus;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.service.InvoiceTransmissionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceCallbackController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTransmissionService transmissionService;

    @PostMapping("/callback")
    public ResponseEntity<Void> receiveCallback(@RequestBody CallbackRequest request) {
        if (request.getDeliveryStatus() != ExternalDeliveryStatus.DELIVERED) {
            return ResponseEntity.ok().build();
        }
        invoiceRepository.findByInvoiceNumber(request.getExternalReference())
            .ifPresent(invoice -> transmissionService.confirmDelivery(invoice.getId()));
        return ResponseEntity.ok().build();
    }

    @Data
    static class CallbackRequest {
        private String externalReference;
        private ExternalDeliveryStatus deliveryStatus;
    }
}
