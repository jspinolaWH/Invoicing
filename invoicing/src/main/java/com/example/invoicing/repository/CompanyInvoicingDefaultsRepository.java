package com.example.invoicing.repository;

import com.example.invoicing.entity.company.CompanyInvoicingDefaults;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInvoicingDefaultsRepository extends JpaRepository<CompanyInvoicingDefaults, Long> {
}
