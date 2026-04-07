package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.entity.validation.dto.ValidationRequest;
import com.example.invoicing.service.BillingProfileValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/billing-profile/validate")
@RequiredArgsConstructor
public class BillingProfileValidationController {
    private final BillingProfileValidationService service;

    @PostMapping
    public ResponseEntity<ValidationReport> validate(
            @PathVariable Long customerId,
            @RequestBody @Valid ValidationRequest request) {
        return ResponseEntity.ok(service.validate(customerId, request.getCompanyId()));
    }
}
