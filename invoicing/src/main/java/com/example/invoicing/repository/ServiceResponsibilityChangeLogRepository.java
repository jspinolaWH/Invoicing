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

    @Query(value = """
        SELECT * FROM service_responsibility_change_log
        WHERE (:customerNumber IS NULL
               OR from_customer_number = :customerNumber
               OR to_customer_number   = :customerNumber)
          AND (:fromStr IS NULL OR applied_at >= CAST(:fromStr AS TIMESTAMP))
          AND (:toStr   IS NULL OR applied_at <= CAST(:toStr   AS TIMESTAMP))
        ORDER BY applied_at DESC
        """, nativeQuery = true)
    List<ServiceResponsibilityChangeLog> findFiltered(
        @Param("customerNumber") String customerNumber,
        @Param("fromStr") String fromStr,
        @Param("toStr") String toStr
    );

    default List<ServiceResponsibilityChangeLog> findFiltered(String customerNumber, Instant from, Instant to) {
        return findFiltered(customerNumber,
                from != null ? from.toString().replace("Z", "") : null,
                to != null ? to.toString().replace("Z", "") : null);
    }
}
