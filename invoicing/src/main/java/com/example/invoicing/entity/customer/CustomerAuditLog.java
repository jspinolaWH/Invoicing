package com.example.invoicing.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customer_audit_log",
    indexes = {
        @Index(name = "idx_customer_audit_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_audit_changed_at", columnList = "changed_at")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 100)
    private String field;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
