package com.example.invoicing.service;

import com.example.invoicing.entity.weighbridge.WeighbridgeIntegrationConfig;
import com.example.invoicing.entity.weighbridge.dto.WeighbridgeConfigRequest;
import com.example.invoicing.entity.weighbridge.dto.WeighbridgeConfigResponse;
import com.example.invoicing.repository.WeighbridgeIntegrationConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeighbridgeIntegrationConfigService {

    private final WeighbridgeIntegrationConfigRepository repository;

    public List<WeighbridgeConfigResponse> listAll() {
        return repository.findAll().stream().map(WeighbridgeConfigResponse::from).toList();
    }

    public Optional<WeighbridgeConfigResponse> findByCustomerNumber(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber).map(WeighbridgeConfigResponse::from);
    }

    @Transactional
    public WeighbridgeConfigResponse upsert(WeighbridgeConfigRequest req) {
        WeighbridgeIntegrationConfig config = repository
            .findByCustomerNumber(req.getCustomerNumber())
            .orElseGet(WeighbridgeIntegrationConfig::new);
        config.setCustomerNumber(req.getCustomerNumber());
        config.setExternalSystemId(req.getExternalSystemId());
        config.setDefaultProductCode(req.getDefaultProductCode());
        config.setSiteReference(req.getSiteReference());
        config.setActive(true);
        return WeighbridgeConfigResponse.from(repository.save(config));
    }

    @Transactional
    public void deactivate(Long id) {
        WeighbridgeIntegrationConfig config = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("WeighbridgeIntegrationConfig not found: " + id));
        config.setActive(false);
        repository.save(config);
    }
}
