package com.example.invoicing.controller;

import com.example.invoicing.entity.billingthreshold.BillingThresholdConfig;
import com.example.invoicing.entity.billingthreshold.BillingThresholdConfigRequest;
import com.example.invoicing.entity.billingthreshold.BillingThresholdConfigResponse;
import com.example.invoicing.repository.BillingThresholdConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-threshold/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INVOICING')")
public class BillingThresholdConfigController {

    private final BillingThresholdConfigRepository repository;

    @GetMapping
    public List<BillingThresholdConfigResponse> list() {
        return repository.findAllByActiveTrue().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BillingThresholdConfigResponse get(@PathVariable Long id) {
        return toResponse(load(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingThresholdConfigResponse create(@RequestBody BillingThresholdConfigRequest req) {
        BillingThresholdConfig config = BillingThresholdConfig.builder()
            .serviceResponsibility(req.getServiceResponsibility())
            .annualEuroLimit(req.getAnnualEuroLimit())
            .description(req.getDescription())
            .active(req.isActive())
            .build();
        return toResponse(repository.save(config));
    }

    @PutMapping("/{id}")
    public BillingThresholdConfigResponse update(@PathVariable Long id, @RequestBody BillingThresholdConfigRequest req) {
        BillingThresholdConfig config = load(id);
        config.setServiceResponsibility(req.getServiceResponsibility());
        config.setAnnualEuroLimit(req.getAnnualEuroLimit());
        config.setDescription(req.getDescription());
        config.setActive(req.isActive());
        return toResponse(repository.save(config));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        BillingThresholdConfig config = load(id);
        config.setActive(false);
        repository.save(config);
    }

    private BillingThresholdConfig load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingThresholdConfig not found: " + id));
    }

    private BillingThresholdConfigResponse toResponse(BillingThresholdConfig c) {
        return BillingThresholdConfigResponse.builder()
            .id(c.getId())
            .serviceResponsibility(c.getServiceResponsibility())
            .annualEuroLimit(c.getAnnualEuroLimit())
            .description(c.getDescription())
            .active(c.isActive())
            .createdBy(c.getCreatedBy())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
