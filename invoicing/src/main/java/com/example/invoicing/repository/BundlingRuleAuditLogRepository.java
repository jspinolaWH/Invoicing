package com.example.invoicing.repository;

import com.example.invoicing.entity.bundling.BundlingRuleAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BundlingRuleAuditLogRepository extends JpaRepository<BundlingRuleAuditLog, Long> {

    List<BundlingRuleAuditLog> findByCustomerNumberOrderByChangedAtDesc(String customerNumber);

    @Override
    default void delete(BundlingRuleAuditLog entity) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll(Iterable<? extends BundlingRuleAuditLog> entities) {
        throw new UnsupportedOperationException("Audit log entries cannot be deleted.");
    }
}
