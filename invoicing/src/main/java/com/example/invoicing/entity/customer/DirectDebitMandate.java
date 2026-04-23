package com.example.invoicing.entity.customer;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "direct_debit_mandates",
       indexes = @Index(name = "idx_ddm_customer", columnList = "customer_id", unique = true))
public class DirectDebitMandate extends BaseAuditEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "mandate_reference", nullable = false, length = 35)
    private String mandateReference;

    @Column(name = "bank_account", nullable = false, length = 50)
    private String bankAccount;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;
}
