package com.example.invoicing.entity.invoicerun;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "active_run_locks",
    indexes = @Index(name = "idx_run_lock_customer", columnList = "customer_number"))
public class ActiveRunLock extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;
}
