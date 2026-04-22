package com.example.invoicing.entity.reportingaudit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reporting_data_audit_log",
    indexes = {
        @Index(name = "idx_rdal_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_rdal_logged_at",  columnList = "logged_at")
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportingDataAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "line_item_id")
    private Long lineItemId;

    @Column(nullable = false, length = 100)
    private String field;

    @Column(name = "assigned_value", length = 500)
    private String assignedValue;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "logged_by", length = 100)
    private String loggedBy;
}
