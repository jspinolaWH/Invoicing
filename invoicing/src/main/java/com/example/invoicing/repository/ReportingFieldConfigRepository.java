package com.example.invoicing.repository;

import com.example.invoicing.entity.reporting.ReportingFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportingFieldConfigRepository extends JpaRepository<ReportingFieldConfig, Long> {
    List<ReportingFieldConfig> findByCompanyIdOrderByDisplayOrderAsc(Long companyId);
    Optional<ReportingFieldConfig> findByCompanyIdAndFieldName(Long companyId, String fieldName);
}
