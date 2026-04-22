package com.example.invoicing.repository;

import com.example.invoicing.entity.customer.CustomerAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAuditLogRepository extends JpaRepository<CustomerAuditLog, Long> {

    List<CustomerAuditLog> findByCustomerIdOrderByChangedAtDesc(Long customerId);

    @Override
    default void delete(CustomerAuditLog entity) {
        throw new UnsupportedOperationException("Customer audit log entries cannot be deleted.");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Customer audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Customer audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll(Iterable<? extends CustomerAuditLog> entities) {
        throw new UnsupportedOperationException("Customer audit log entries cannot be deleted.");
    }
}
