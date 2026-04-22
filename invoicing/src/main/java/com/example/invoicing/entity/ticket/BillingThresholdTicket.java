package com.example.invoicing.entity.ticket;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "billing_threshold_tickets",
    indexes = {
        @Index(name = "idx_bt_ticket_trigger",  columnList = "trigger_id"),
        @Index(name = "idx_bt_ticket_customer", columnList = "customer_number"),
        @Index(name = "idx_bt_ticket_status",   columnList = "status")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingThresholdTicket extends BaseAuditEntity {

    @Column(name = "trigger_id", nullable = false)
    private Long triggerId;

    @Column(name = "customer_number", nullable = false, length = 9)
    private String customerNumber;

    @Column(name = "service_responsibility", nullable = false, length = 50)
    private String serviceResponsibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(length = 2000)
    private String notes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    public enum TicketStatus {
        OPEN, IN_PROGRESS, RESOLVED
    }
}
