package com.example.invoicing.repository;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InvoiceRunRepository extends JpaRepository<InvoiceRun, Long> {

    Page<InvoiceRun> findAllByOrderByIdDesc(Pageable pageable);

    Page<InvoiceRun> findByStatusOrderByIdDesc(InvoiceRunStatus status, Pageable pageable);

    @Query("SELECT r FROM InvoiceRun r WHERE r.status = 'COMPLETED' " +
           "AND r.scheduledSendAt IS NOT NULL AND r.scheduledSendAt <= :now")
    List<InvoiceRun> findScheduledForSend(@Param("now") Instant now);

    @Query("SELECT r FROM InvoiceRun r WHERE r.status IN ('PENDING', 'RUNNING', 'SENDING')")
    List<InvoiceRun> findActiveRuns();
}
