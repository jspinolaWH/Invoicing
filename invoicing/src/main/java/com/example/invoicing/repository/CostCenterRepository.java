package com.example.invoicing.repository;

import com.example.invoicing.entity.costcenter.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    Optional<CostCenter> findByCompositeCode(String compositeCode);

    List<CostCenter> findByProductSegment(String productSegment);

    List<CostCenter> findByResponsibilitySegment(String responsibilitySegment);
}
