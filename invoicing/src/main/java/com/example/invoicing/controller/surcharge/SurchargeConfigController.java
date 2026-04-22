package com.example.invoicing.controller.surcharge;
import com.example.invoicing.entity.surcharge.dto.GlobalToggleResponse;
import com.example.invoicing.entity.surcharge.dto.GlobalToggleRequest;
import com.example.invoicing.entity.surcharge.dto.SurchargeConfigResponse;
import com.example.invoicing.entity.surcharge.dto.SurchargeConfigRequest;
import com.example.invoicing.entity.surcharge.SurchargeConfig;
import com.example.invoicing.repository.SurchargeConfigRepository;
import com.example.invoicing.service.BillingSurchargeService;

import com.example.invoicing.entity.surcharge.dto.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/surcharge-config")
@RequiredArgsConstructor
public class SurchargeConfigController {

    private final SurchargeConfigRepository repository;
    private final BillingSurchargeService surchargeService;

    @GetMapping
    public List<SurchargeConfigResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SurchargeConfigResponse get(@PathVariable Long id) {
        return toResponse(load(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurchargeConfigResponse create(@Valid @RequestBody SurchargeConfigRequest req) {
        SurchargeConfig config = SurchargeConfig.builder()
            .deliveryMethod(req.getDeliveryMethod())
            .customerType(req.getCustomerType())
            .amount(req.getAmount())
            .description(req.getDescription())
            .active(req.isActive())
            .exemptFirstInvoice(req.isExemptFirstInvoice())
            .requiresTariffInclusion(req.isRequiresTariffInclusion())
            .globalSurchargeEnabled(surchargeService.isGlobalSurchargeEnabled())
            .build();
        return toResponse(repository.save(config));
    }

    @PutMapping("/{id}")
    public SurchargeConfigResponse update(@PathVariable Long id, @Valid @RequestBody SurchargeConfigRequest req) {
        SurchargeConfig config = load(id);
        config.setAmount(req.getAmount());
        config.setDescription(req.getDescription());
        config.setActive(req.isActive());
        config.setExemptFirstInvoice(req.isExemptFirstInvoice());
        config.setRequiresTariffInclusion(req.isRequiresTariffInclusion());
        return toResponse(repository.save(config));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        SurchargeConfig config = load(id);
        config.setActive(false);
        repository.save(config);
    }

    @PutMapping("/global-toggle")
    public GlobalToggleResponse globalToggle(@RequestBody GlobalToggleRequest req) {
        int updated = surchargeService.setGlobalEnabled(req.isEnabled());
        return new GlobalToggleResponse(req.isEnabled(), updated);
    }

    private SurchargeConfig load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("SurchargeConfig not found: " + id));
    }

    private SurchargeConfigResponse toResponse(SurchargeConfig c) {
        return SurchargeConfigResponse.builder()
            .id(c.getId())
            .deliveryMethod(c.getDeliveryMethod())
            .customerType(c.getCustomerType())
            .amount(c.getAmount())
            .description(c.getDescription())
            .active(c.isActive())
            .globalSurchargeEnabled(c.isGlobalSurchargeEnabled())
            .exemptFirstInvoice(c.isExemptFirstInvoice())
            .requiresTariffInclusion(c.isRequiresTariffInclusion())
            .createdAt(c.getCreatedAt())
            .createdBy(c.getCreatedBy())
            .build();
    }
}
