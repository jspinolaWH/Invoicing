package com.example.invoicing.repository;

import com.example.invoicing.entity.validation.BillingEventValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingEventValidationLogRepository extends JpaRepository<BillingEventValidationLog, Long> {
    List<BillingEventValidationLog> findByBillingEventIdOrderByValidatedAtDesc(Long billingEventId);
}
