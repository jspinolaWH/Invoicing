package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.retroactive.ServiceResponsibilityChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ServiceResponsibilityChangeLogRepository extends JpaRepository<ServiceResponsibilityChangeLog, Long> {

    Optional<ServiceResponsibilityChangeLog> findByChangeRunId(String changeRunId);

    List<ServiceResponsibilityChangeLog> findByFromCustomerNumberOrderByAppliedAtDesc(String fromCustomerNumber);

    List<ServiceResponsibilityChangeLog> findByToCustomerNumberOrderByAppliedAtDesc(String toCustomerNumber);

    @Query("""
        SELECT l FROM ServiceResponsibilityChangeLog l
        WHERE (:customerNumber IS NULL
               OR l.fromCustomerNumber = :customerNumber
               OR l.toCustomerNumber = :customerNumber)
          AND (:from IS NULL OR l.appliedAt >= :from)
          AND (:to   IS NULL OR l.appliedAt <= :to)
        ORDER BY l.appliedAt DESC
        """)
    List<ServiceResponsibilityChangeLog> findFiltered(
        @Param("customerNumber") String customerNumber,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
