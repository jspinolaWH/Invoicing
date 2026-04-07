package com.example.invoicing.billingrestriction;

import com.example.invoicing.billingrestriction.dto.BillingRestrictionRequest;
import com.example.invoicing.billingrestriction.dto.BillingRestrictionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-restrictions")
@RequiredArgsConstructor
public class BillingRestrictionController {

    private final BillingRestrictionService service;

    @GetMapping
    public List<BillingRestrictionResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BillingRestrictionResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingRestrictionResponse create(@Valid @RequestBody BillingRestrictionRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public BillingRestrictionResponse update(@PathVariable Long id, @Valid @RequestBody BillingRestrictionRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
