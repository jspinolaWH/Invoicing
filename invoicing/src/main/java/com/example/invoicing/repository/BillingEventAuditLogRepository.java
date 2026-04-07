package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.audit.BillingEventAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BillingEventAuditLogRepository extends JpaRepository<BillingEventAuditLog, Long> {

    List<BillingEventAuditLog> findByBillingEventIdOrderByChangedAtDesc(Long billingEventId);

    @Query("""
        SELECT a FROM BillingEventAuditLog a
        WHERE a.changedBy = :user
          AND a.changedAt >= :from
          AND a.changedAt <= :to
        ORDER BY a.changedAt DESC
        """)
    List<BillingEventAuditLog> findByUserAndTimeRange(
        @Param("user") String user,
        @Param("from") Instant from,
        @Param("to")   Instant to
    );

    List<BillingEventAuditLog> findByFieldOrderByChangedAtDesc(String field);
}
