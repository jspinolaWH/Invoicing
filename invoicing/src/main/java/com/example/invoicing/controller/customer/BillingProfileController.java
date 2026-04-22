package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.service.BillingProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/billing-profile")
@RequiredArgsConstructor
public class BillingProfileController {
    private final BillingProfileService service;

    @GetMapping
    public ResponseEntity<BillingProfileResponse> get(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getBillingProfile(customerId));
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PutMapping
    public ResponseEntity<BillingProfileResponse> update(
            @PathVariable Long customerId,
            @RequestBody @Valid BillingProfileRequest request) {
        return ResponseEntity.ok(service.updateBillingProfile(customerId, request));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<List<CustomerAuditLogResponse>> getAuditLog(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getAuditLog(customerId));
    }
}
