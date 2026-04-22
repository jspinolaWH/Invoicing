package com.example.invoicing.entity.paymentreminder;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.invoice.Invoice;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "payment_reminders")
public class PaymentReminder extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "reminder_number", nullable = false)
    private int reminderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentReminderStatus status = PaymentReminderStatus.PENDING;

    @Column(name = "recipient_address", length = 500)
    private String recipientAddress;

    @Column(name = "delivery_method", length = 20)
    private String deliveryMethod;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "message", length = 2000)
    private String message;
}
