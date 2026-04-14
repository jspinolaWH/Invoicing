package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.BillingProfileResponse;
import com.example.invoicing.entity.customer.dto.CustomerSearchResult;
import com.example.invoicing.entity.customer.dto.CustomerSummaryResponse;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping("/search")
    public ResponseEntity<List<CustomerSearchResult>> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return ResponseEntity.ok(List.of());
        List<CustomerSearchResult> results = customerRepo
            .search(q.trim(), PageRequest.of(0, 10))
            .stream().map(CustomerSearchResult::from).toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingProfileResponse> getOne(@PathVariable Long id) {
        return customerRepo.findById(id)
            .map(c -> ResponseEntity.ok(BillingProfileResponse.from(c)))
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }
}
