package com.example.invoicing.repository;
import com.example.invoicing.entity.invoicerun.ActiveRunLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveRunLockRepository extends JpaRepository<ActiveRunLock, Long> {
    boolean existsByCustomerId(Long customerId);
    void deleteByRunId(Long runId);
}
