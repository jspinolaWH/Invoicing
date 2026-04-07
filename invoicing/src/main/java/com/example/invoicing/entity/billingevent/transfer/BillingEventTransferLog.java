package com.example.invoicing.entity.billingevent.transfer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "billing_event_transfer_log",
       indexes = {
           @Index(name = "idx_transfer_log_event",       columnList = "billingEventId"),
           @Index(name = "idx_transfer_log_source_cust", columnList = "sourceCustomerNumber"),
           @Index(name = "idx_transfer_log_target_cust", columnList = "targetCustomerNumber"),
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEventTransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long billingEventId;

    @Column(nullable = false, length = 9)
    private String sourceCustomerNumber;

    @Column(nullable = false, length = 9)
    private String targetCustomerNumber;

    @Column(length = 100)
    private String sourcePropertyId;

    @Column(length = 100)
    private String targetPropertyId;

    @Column(nullable = false, length = 100)
    private String transferredBy;

    @Column(nullable = false)
    private Instant transferredAt;

    @Column(nullable = false, length = 2000)
    private String reason;
}
