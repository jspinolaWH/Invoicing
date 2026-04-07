package com.example.invoicing.repository;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MinimumFeeConfigRepository extends JpaRepository<MinimumFeeConfig, Long> {

    Optional<MinimumFeeConfig> findByCustomerTypeAndPeriodTypeAndActiveTrue(String customerType, PeriodType periodType);

    List<MinimumFeeConfig> findAllByActiveTrue();
}
