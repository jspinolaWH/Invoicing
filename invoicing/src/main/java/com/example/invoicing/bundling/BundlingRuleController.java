package com.example.invoicing.bundling;

import com.example.invoicing.bundling.dto.BundlingRuleRequest;
import com.example.invoicing.bundling.dto.BundlingRuleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerNumber}/bundling-rules")
@RequiredArgsConstructor
public class BundlingRuleController {

    private final BundlingRuleService service;

    @GetMapping
    public List<BundlingRuleResponse> list(@PathVariable String customerNumber) {
        return service.findByCustomer(customerNumber);
    }

    @PutMapping
    public List<BundlingRuleResponse> replace(
        @PathVariable String customerNumber,
        @RequestBody List<@Valid BundlingRuleRequest> rules
    ) {
        return service.replaceAll(customerNumber, rules);
    }
}
