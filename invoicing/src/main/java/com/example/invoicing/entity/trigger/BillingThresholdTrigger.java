package com.example.invoicing.entity.trigger;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billing_threshold_triggers",
    indexes = {
        @Index(name = "idx_bt_trigger_customer", columnList = "customer_number"),
        @Index(name = "idx_bt_trigger_status",   columnList = "status"),
        @Index(name = "idx_bt_trigger_year",      columnList = "trigger_year")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingThresholdTrigger extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 9)
    private String customerNumber;

    @Column(name = "service_responsibility", nullable = false, length = 50)
    private String serviceResponsibility;

    @Column(name = "annual_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualAmount;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "trigger_year", nullable = false)
    private int triggerYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TriggerStatus status = TriggerStatus.OPEN;

    @Column(name = "audited_by", length = 100)
    private String auditedBy;

    @Column(name = "audited_at")
    private Instant auditedAt;

    @Column(name = "decision", length = 2000)
    private String decision;

    public enum TriggerStatus {
        OPEN, CONVERTED_TO_TICKET, REVIEWED
    }
}
