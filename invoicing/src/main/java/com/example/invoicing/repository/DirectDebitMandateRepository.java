package com.example.invoicing.repository;

import com.example.invoicing.entity.customer.DirectDebitMandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectDebitMandateRepository extends JpaRepository<DirectDebitMandate, Long> {
    Optional<DirectDebitMandate> findByCustomer_Id(Long customerId);
}
