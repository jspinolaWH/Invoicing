package com.example.invoicing.service;
import com.example.invoicing.entity.surcharge.dto.GlobalToggleResponse;
import com.example.invoicing.entity.surcharge.dto.GlobalToggleRequest;
import com.example.invoicing.entity.surcharge.dto.SurchargeConfigResponse;
import com.example.invoicing.entity.surcharge.dto.SurchargeConfigRequest;
import com.example.invoicing.repository.SurchargeConfigRepository;
import com.example.invoicing.entity.surcharge.SurchargeConfig;

import com.example.invoicing.entity.customer.DeliveryMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingSurchargeService {

    private final SurchargeConfigRepository repository;

    @Transactional(readOnly = true)
    public Optional<BigDecimal> resolveSurcharge(DeliveryMethod deliveryMethod) {
        if (!isGlobalSurchargeEnabled()) {
            return Optional.empty();
        }
        return repository.findByDeliveryMethodAndActiveTrue(deliveryMethod)
            .map(SurchargeConfig::getAmount);
    }

    @Transactional(readOnly = true)
    public boolean isGlobalSurchargeEnabled() {
        return repository.findAll().stream()
            .findFirst()
            .map(SurchargeConfig::isGlobalSurchargeEnabled)
            .orElse(true);
    }

    public int setGlobalEnabled(boolean enabled) {
        return repository.setGlobalSurchargeEnabled(enabled);
    }
}
