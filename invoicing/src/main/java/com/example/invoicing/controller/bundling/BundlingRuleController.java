package com.example.invoicing.controller.bundling;
import com.example.invoicing.service.BundlingRuleService;

import com.example.invoicing.entity.bundling.dto.BundlingRuleAuditLogResponse;
import com.example.invoicing.entity.bundling.dto.BundlingRuleRequest;
import com.example.invoicing.entity.bundling.dto.BundlingRuleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/audit-log")
    public List<BundlingRuleAuditLogResponse> auditLog(@PathVariable String customerNumber) {
        return service.findAuditLog(customerNumber);
    }

    @PutMapping
    public List<BundlingRuleResponse> replace(
        @PathVariable String customerNumber,
        @RequestBody List<@Valid BundlingRuleRequest> rules,
        @AuthenticationPrincipal String currentUser
    ) {
        return service.replaceAll(customerNumber, rules, currentUser != null ? currentUser : "system");
    }
}
