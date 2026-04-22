package com.example.invoicing.controller.allocation;

import com.example.invoicing.entity.account.PriceComponent;
import com.example.invoicing.entity.allocation.dto.AllocationResolveResponse;
import com.example.invoicing.entity.allocation.dto.AllocationRuleRequest;
import com.example.invoicing.entity.allocation.dto.AllocationRuleResponse;
import com.example.invoicing.service.AccountingAllocationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/allocation-rules")
@RequiredArgsConstructor
public class AccountingAllocationRuleController {

    private final AccountingAllocationRuleService ruleService;

    @GetMapping
    public List<AllocationRuleResponse> list() {
        return ruleService.findAll();
    }

    @GetMapping("/{id}")
    public AllocationRuleResponse getById(@PathVariable Long id) {
        return ruleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationRuleResponse create(@Valid @RequestBody AllocationRuleRequest request) {
        return ruleService.create(request);
    }

    @PutMapping("/{id}")
    public AllocationRuleResponse update(@PathVariable Long id,
                                         @Valid @RequestBody AllocationRuleRequest request) {
        return ruleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }

    @GetMapping("/resolve")
    public AllocationResolveResponse resolve(
        @RequestParam Long productId,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) String municipality,
        @RequestParam(required = false) PriceComponent priceComponent
    ) {
        return ruleService.resolve(productId, region, municipality, priceComponent);
    }
}
