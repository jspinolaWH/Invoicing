package com.example.invoicing.repository;

import com.example.invoicing.entity.invoicetemplate.InvoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplate, Long> {
    Optional<InvoiceTemplate> findByCode(String code);
}
