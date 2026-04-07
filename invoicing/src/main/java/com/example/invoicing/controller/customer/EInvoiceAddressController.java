package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.service.EInvoiceAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/einvoice-address")
@RequiredArgsConstructor
public class EInvoiceAddressController {
    private final EInvoiceAddressService service;

    @GetMapping
    public ResponseEntity<EInvoiceAddressResponse> get(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getAddress(customerId));
    }

    @PutMapping
    public ResponseEntity<EInvoiceAddressResponse> set(
            @PathVariable Long customerId,
            @RequestBody @Valid EInvoiceAddressRequest request) {
        return ResponseEntity.ok(service.setAddress(customerId, request));
    }
}
