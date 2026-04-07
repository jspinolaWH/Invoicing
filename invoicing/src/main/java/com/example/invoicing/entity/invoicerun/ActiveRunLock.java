package com.example.invoicing.entity.invoicerun;
import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "active_run_locks",
       indexes = @Index(name = "idx_run_lock_customer", columnList = "customer_id"))
public class ActiveRunLock extends BaseAuditEntity {
    @Column(name = "customer_id", nullable = false) private Long customerId;
    @Column(name = "run_id", nullable = false) private Long runId;
}
