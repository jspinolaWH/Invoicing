package com.example.invoicing.minimumfee;

import com.example.invoicing.minimumfee.dto.MinimumFeeConfigRequest;
import com.example.invoicing.minimumfee.dto.MinimumFeeConfigResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/minimum-fee-config")
@RequiredArgsConstructor
public class MinimumFeeConfigController {

    private final MinimumFeeConfigRepository repository;

    @GetMapping
    public List<MinimumFeeConfigResponse> list() {
        return repository.findAllByActiveTrue().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MinimumFeeConfigResponse get(@PathVariable Long id) {
        return toResponse(load(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MinimumFeeConfigResponse create(@Valid @RequestBody MinimumFeeConfigRequest req) {
        MinimumFeeConfig config = MinimumFeeConfig.builder()
            .customerType(req.getCustomerType())
            .netAmountThreshold(req.getNetAmountThreshold())
            .periodType(req.getPeriodType())
            .contractStartAdjustment(req.isContractStartAdjustment())
            .contractEndAdjustment(req.isContractEndAdjustment())
            .adjustmentProductCode(req.getAdjustmentProductCode())
            .description(req.getDescription())
            .active(req.isActive())
            .build();
        return toResponse(repository.save(config));
    }

    @PutMapping("/{id}")
    public MinimumFeeConfigResponse update(@PathVariable Long id, @Valid @RequestBody MinimumFeeConfigRequest req) {
        MinimumFeeConfig config = load(id);
        config.setCustomerType(req.getCustomerType());
        config.setNetAmountThreshold(req.getNetAmountThreshold());
        config.setPeriodType(req.getPeriodType());
        config.setContractStartAdjustment(req.isContractStartAdjustment());
        config.setContractEndAdjustment(req.isContractEndAdjustment());
        config.setAdjustmentProductCode(req.getAdjustmentProductCode());
        config.setDescription(req.getDescription());
        config.setActive(req.isActive());
        return toResponse(repository.save(config));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        MinimumFeeConfig config = load(id);
        config.setActive(false);
        repository.save(config);
    }

    private MinimumFeeConfig load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("MinimumFeeConfig not found: " + id));
    }

    private MinimumFeeConfigResponse toResponse(MinimumFeeConfig c) {
        return MinimumFeeConfigResponse.builder()
            .id(c.getId())
            .customerType(c.getCustomerType())
            .netAmountThreshold(c.getNetAmountThreshold())
            .periodType(c.getPeriodType())
            .contractStartAdjustment(c.isContractStartAdjustment())
            .contractEndAdjustment(c.isContractEndAdjustment())
            .adjustmentProductCode(c.getAdjustmentProductCode())
            .description(c.getDescription())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .createdBy(c.getCreatedBy())
            .build();
    }
}
