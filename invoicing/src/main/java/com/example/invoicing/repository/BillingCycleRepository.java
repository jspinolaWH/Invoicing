package com.example.invoicing.repository;
import com.example.invoicing.entity.billingcycle.BillingFrequency;
import com.example.invoicing.entity.billingcycle.BillingCycle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    @Query("SELECT c FROM BillingCycle c WHERE c.active = true AND c.nextBillingDate <= :runDate ORDER BY c.nextBillingDate")
    List<BillingCycle> findCyclesDueInRunWindow(@Param("runDate") LocalDate runDate);

    @Query("SELECT c FROM BillingCycle c WHERE c.active = true AND c.nextBillingDate <= :runDate AND c.frequency = :frequency ORDER BY c.nextBillingDate")
    List<BillingCycle> findCyclesDueByFrequency(@Param("runDate") LocalDate runDate, @Param("frequency") BillingFrequency frequency);

    List<BillingCycle> findByCustomerNumberAndActiveTrue(String customerNumber);

    List<BillingCycle> findAllByActiveTrueOrderByNextBillingDateAsc();
}
