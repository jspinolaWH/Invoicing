package com.example.invoicing.repository;

import com.example.invoicing.entity.billingthreshold.BillingThresholdConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingThresholdConfigRepository extends JpaRepository<BillingThresholdConfig, Long> {
    List<BillingThresholdConfig> findAllByActiveTrue();
    Optional<BillingThresholdConfig> findByServiceResponsibilityAndActiveTrue(String serviceResponsibility);
}
