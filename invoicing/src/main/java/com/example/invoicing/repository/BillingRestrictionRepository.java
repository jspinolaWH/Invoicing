package com.example.invoicing.repository;
import com.example.invoicing.entity.billingrestriction.BillingType;
import com.example.invoicing.entity.billingrestriction.BillingRestriction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRestrictionRepository extends JpaRepository<BillingRestriction, Long> {

    List<BillingRestriction> findByBillingTypeAndActiveTrue(BillingType billingType);

    List<BillingRestriction> findByActiveTrue();

    List<BillingRestriction> findByServiceTypeAndActiveTrue(String serviceType);
}
