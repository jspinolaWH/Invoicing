package com.example.invoicing.repository;

import com.example.invoicing.entity.trigger.BillingThresholdTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillingThresholdTriggerRepository extends JpaRepository<BillingThresholdTrigger, Long> {

    Optional<BillingThresholdTrigger> findByCustomerNumberAndTriggerYearAndServiceResponsibilityAndStatus(
        String customerNumber, int triggerYear, String serviceResponsibility,
        BillingThresholdTrigger.TriggerStatus status);

    @Query("""
        SELECT t FROM BillingThresholdTrigger t
        WHERE (:serviceResponsibility IS NULL OR t.serviceResponsibility = :serviceResponsibility)
          AND (:status IS NULL OR t.status = :status)
          AND (:customerNumber IS NULL OR t.customerNumber = :customerNumber)
        ORDER BY t.createdAt DESC
        """)
    List<BillingThresholdTrigger> findFiltered(
        @Param("serviceResponsibility") String serviceResponsibility,
        @Param("status") BillingThresholdTrigger.TriggerStatus status,
        @Param("customerNumber") String customerNumber
    );
}
