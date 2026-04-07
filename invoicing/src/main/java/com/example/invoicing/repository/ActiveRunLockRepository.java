package com.example.invoicing.repository;

import com.example.invoicing.entity.invoicerun.ActiveRunLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActiveRunLockRepository extends JpaRepository<ActiveRunLock, Long> {

    boolean existsByCustomerNumber(String customerNumber);

    Optional<ActiveRunLock> findByCustomerNumber(String customerNumber);

    @Modifying
    @Query("DELETE FROM ActiveRunLock l WHERE l.runId = :runId")
    void deleteByRunId(@Param("runId") Long runId);
}
