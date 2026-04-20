package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.BillingEventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingEventTemplateRepository extends JpaRepository<BillingEventTemplate, Long> {
}
