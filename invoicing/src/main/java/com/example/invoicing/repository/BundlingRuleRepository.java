package com.example.invoicing.repository;
import com.example.invoicing.entity.bundling.BundlingRule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BundlingRuleRepository extends JpaRepository<BundlingRule, Long> {

    List<BundlingRule> findByCustomerNumber(String customerNumber);

    @Modifying
    @Transactional
    void deleteByCustomerNumber(String customerNumber);

    Optional<BundlingRule> findByCustomerNumberAndProductGroup(String customerNumber, String productGroup);
}
