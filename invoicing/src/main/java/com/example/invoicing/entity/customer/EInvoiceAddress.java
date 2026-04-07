package com.example.invoicing.entity.customer;
import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "einvoice_addresses",
       indexes = @Index(name = "idx_einvoice_customer", columnList = "customer_id", unique = true))
public class EInvoiceAddress extends BaseAuditEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;
    @Column(name = "address", nullable = false, length = 35) private String address;
    @Column(name = "operator_code", length = 20) private String operatorCode;
    @Column(name = "manually_locked", nullable = false) private boolean manuallyLocked = false;
    @Column(name = "lock_reason", length = 500) private String lockReason;

    @Column(name = "registered_with_operator", nullable = false)
    private boolean registeredWithOperator = false;

    @Column(name = "registration_date")
    private Instant registrationDate;

    @Column(name = "termination_date")
    private Instant terminationDate;
}
