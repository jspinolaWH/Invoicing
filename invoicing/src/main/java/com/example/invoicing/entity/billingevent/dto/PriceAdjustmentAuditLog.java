package com.example.invoicing.entity.billingevent.dto;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_adjustment_audit_log",
    indexes = {
        @Index(name = "idx_price_adj_audit_customer",   columnList = "customer_number"),
        @Index(name = "idx_price_adj_audit_applied_at", columnList = "applied_at")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAdjustmentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false, length = 50)
    private String customerNumber;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(length = 2000)
    private String reason;

    @Column(name = "internal_comment", length = 2000)
    private String internalComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private AdjustmentType adjustmentType;

    @Column(name = "adjustment_value", precision = 19, scale = 4)
    private BigDecimal adjustmentValue;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "affected_event_ids", length = 4000)
    private String affectedEventIds;

    @Column(name = "total_delta", precision = 19, scale = 4)
    private BigDecimal totalDelta;
}
