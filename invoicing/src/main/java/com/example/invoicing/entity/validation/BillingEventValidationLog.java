package com.example.invoicing.entity.validation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "billing_event_validation_logs",
    indexes = @Index(name = "idx_val_log_event_id", columnList = "billing_event_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventValidationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billing_event_id", nullable = false)
    private Long billingEventId;

    @Column(name = "rule_type", length = 30)
    private String ruleType;

    @Column(name = "rule_code", length = 80)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private ValidationSeverity severity;

    @Column(name = "field", length = 100)
    private String field;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;
}
