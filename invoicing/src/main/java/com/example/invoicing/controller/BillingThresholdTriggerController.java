package com.example.invoicing.controller;

import com.example.invoicing.entity.trigger.BillingThresholdTrigger;
import com.example.invoicing.repository.BillingThresholdTriggerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing-threshold/triggers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INVOICING')")
public class BillingThresholdTriggerController {

    private final BillingThresholdTriggerRepository repository;

    @GetMapping
    public List<BillingThresholdTrigger> list(
            @RequestParam(required = false) String serviceResponsibility,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerNumber) {
        BillingThresholdTrigger.TriggerStatus statusEnum = status != null
            ? BillingThresholdTrigger.TriggerStatus.valueOf(status)
            : null;
        return repository.findFiltered(serviceResponsibility, statusEnum, customerNumber);
    }

    @GetMapping("/{id}")
    public BillingThresholdTrigger get(@PathVariable Long id) {
        return load(id);
    }

    @PatchMapping("/{id}/review")
    public BillingThresholdTrigger review(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String currentUser) {
        BillingThresholdTrigger trigger = load(id);
        trigger.setDecision(body.get("decision"));
        trigger.setAuditedBy(currentUser != null ? currentUser : "system");
        trigger.setAuditedAt(Instant.now());
        trigger.setStatus(BillingThresholdTrigger.TriggerStatus.REVIEWED);
        return repository.save(trigger);
    }

    private BillingThresholdTrigger load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingThresholdTrigger not found: " + id));
    }
}
