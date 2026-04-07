package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.service.BillingProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/billing-profile")
@RequiredArgsConstructor
public class BillingProfileController {
    private final BillingProfileService service;

    @GetMapping
    public ResponseEntity<BillingProfileResponse> get(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getBillingProfile(customerId));
    }

    @PutMapping
    public ResponseEntity<BillingProfileResponse> update(
            @PathVariable Long customerId,
            @RequestBody @Valid BillingProfileRequest request) {
        return ResponseEntity.ok(service.updateBillingProfile(customerId, request));
    }
}
