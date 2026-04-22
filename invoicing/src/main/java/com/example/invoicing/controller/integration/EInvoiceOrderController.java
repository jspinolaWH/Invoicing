package com.example.invoicing.controller.integration;

import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.service.EInvoiceOrderIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class EInvoiceOrderController {

    private final EInvoiceOrderIngestionService ingestionService;

    @PostMapping("/einvoice-orders")
    public ResponseEntity<EInvoiceOrderIngestionResult> receiveEInvoiceOrder(
            @RequestBody @Valid EInvoiceOrderRequest request) {
        return ResponseEntity.ok(ingestionService.ingestEInvoiceOrder(request));
    }

    @PostMapping("/direct-debit-orders")
    public ResponseEntity<EInvoiceOrderIngestionResult> receiveDirectDebitOrder(
            @RequestBody @Valid DirectDebitOrderRequest request) {
        return ResponseEntity.ok(ingestionService.ingestDirectDebitOrder(request));
    }
}
