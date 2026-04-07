package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.BillingProfileResponse;
import com.example.invoicing.entity.customer.dto.CustomerSummaryResponse;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerBillingProfileRepository customerRepo;

    @GetMapping
    public ResponseEntity<List<CustomerSummaryResponse>> list() {
        List<CustomerSummaryResponse> result = customerRepo.findAll().stream()
            .map(CustomerSummaryResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingProfileResponse> getOne(@PathVariable Long id) {
        return customerRepo.findById(id)
            .map(c -> ResponseEntity.ok(BillingProfileResponse.from(c)))
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }
}
