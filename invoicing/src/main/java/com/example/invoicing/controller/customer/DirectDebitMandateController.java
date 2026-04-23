package com.example.invoicing.controller.customer;

import com.example.invoicing.entity.customer.dto.DirectDebitMandateResponse;
import com.example.invoicing.service.DirectDebitMandateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/direct-debit-mandate")
@RequiredArgsConstructor
public class DirectDebitMandateController {

    private final DirectDebitMandateService service;

    @GetMapping
    public ResponseEntity<DirectDebitMandateResponse> get(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getMandate(customerId));
    }
}
