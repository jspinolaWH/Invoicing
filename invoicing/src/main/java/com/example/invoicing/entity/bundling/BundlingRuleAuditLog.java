package com.example.invoicing.entity.bundling;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bundling_rule_audit_log",
    indexes = {
        @Index(name = "idx_bral_customer_number", columnList = "customer_number"),
        @Index(name = "idx_bral_changed_by",      columnList = "changed_by")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundlingRuleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "product_group", nullable = false, length = 100)
    private String productGroup;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "old_value", length = 50)
    private String oldValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
