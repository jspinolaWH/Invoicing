package com.example.invoicing.run;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InvoiceRunRepository extends JpaRepository<InvoiceRun, Long> {

    @Query("SELECT r FROM InvoiceRun r WHERE r.status = 'COMPLETED' " +
           "AND r.scheduledSendAt IS NOT NULL AND r.scheduledSendAt <= :now")
    List<InvoiceRun> findScheduledForSend(@Param("now") Instant now);

    @Query("SELECT r FROM InvoiceRun r WHERE r.status IN ('PENDING', 'RUNNING', 'SENDING')")
    List<InvoiceRun> findActiveRuns();
}
