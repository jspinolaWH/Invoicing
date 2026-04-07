package com.example.invoicing.entity.billingevent.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "billing_event_audit_log",
    indexes = {
        @Index(name = "idx_audit_log_event_id",   columnList = "billing_event_id"),
        @Index(name = "idx_audit_log_changed_by", columnList = "changed_by")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billing_event_id", nullable = false)
    private Long billingEventId;

    @Column(nullable = false, length = 100)
    private String field;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", length = 2000)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(nullable = false, length = 2000)
    private String reason;
}
