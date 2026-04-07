package com.example.invoicing.controller.billingcycle;
import com.example.invoicing.service.BillingCycleService;

import com.example.invoicing.entity.billingcycle.dto.BillingCycleRequest;
import com.example.invoicing.entity.billingcycle.dto.BillingCycleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-cycles")
@RequiredArgsConstructor
public class BillingCycleController {

    private final BillingCycleService service;

    @GetMapping
    public List<BillingCycleResponse> list(@RequestParam(required = false) String customerNumber) {
        return customerNumber != null && !customerNumber.isBlank()
            ? service.findByCustomer(customerNumber)
            : service.findAll();
    }

    @GetMapping("/{id}")
    public BillingCycleResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingCycleResponse create(@Valid @RequestBody BillingCycleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public BillingCycleResponse update(@PathVariable Long id, @Valid @RequestBody BillingCycleRequest req) {
        return service.update(id, req);
    }
}
