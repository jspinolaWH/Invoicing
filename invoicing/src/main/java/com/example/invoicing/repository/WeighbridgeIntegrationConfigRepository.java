package com.example.invoicing.repository;

import com.example.invoicing.entity.weighbridge.WeighbridgeIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeighbridgeIntegrationConfigRepository extends JpaRepository<WeighbridgeIntegrationConfig, Long> {
    Optional<WeighbridgeIntegrationConfig> findByCustomerNumber(String customerNumber);
}
