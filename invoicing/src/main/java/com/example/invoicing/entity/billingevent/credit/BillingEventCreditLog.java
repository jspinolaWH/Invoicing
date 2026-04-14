package com.example.invoicing.entity.billingevent.credit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "billing_event_credit_log",
       indexes = {
           @Index(name = "idx_credit_log_original",  columnList = "originalEventId"),
           @Index(name = "idx_credit_log_credit",    columnList = "creditEventId"),
           @Index(name = "idx_credit_log_new_event", columnList = "newEventId"),
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventCreditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long originalEventId;

    @Column(nullable = false)
    private Long creditEventId;

    @Column(nullable = false)
    private Long newEventId;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(nullable = false)
    private Instant performedAt;

    @Column(nullable = false, length = 2000)
    private String reason;
}
