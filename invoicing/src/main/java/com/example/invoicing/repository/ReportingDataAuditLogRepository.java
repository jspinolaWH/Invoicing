package com.example.invoicing.repository;

import com.example.invoicing.entity.reportingaudit.ReportingDataAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportingDataAuditLogRepository extends JpaRepository<ReportingDataAuditLog, Long> {

    List<ReportingDataAuditLog> findByInvoiceIdOrderByLoggedAtDesc(Long invoiceId);

    @Query("""
        SELECT r FROM ReportingDataAuditLog r
        WHERE r.loggedAt >= :from
          AND r.loggedAt <= :to
        ORDER BY r.loggedAt DESC
        """)
    List<ReportingDataAuditLog> findByPeriod(
        @Param("from") Instant from,
        @Param("to")   Instant to
    );

    @Override
    default void delete(ReportingDataAuditLog entity) {
        throw new UnsupportedOperationException("Reporting audit log entries cannot be deleted.");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Reporting audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Reporting audit log entries cannot be deleted.");
    }

    @Override
    default void deleteAll(Iterable<? extends ReportingDataAuditLog> entities) {
        throw new UnsupportedOperationException("Reporting audit log entries cannot be deleted.");
    }
}
