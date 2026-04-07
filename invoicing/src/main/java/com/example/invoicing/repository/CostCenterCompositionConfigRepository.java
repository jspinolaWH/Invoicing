package com.example.invoicing.repository;
import com.example.invoicing.entity.costcenter.CostCenterCompositionConfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterCompositionConfigRepository extends JpaRepository<CostCenterCompositionConfig, Long> {
}
