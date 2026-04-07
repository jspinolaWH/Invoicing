package com.example.invoicing.billingcycle;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "billing_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCycle extends BaseAuditEntity {

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingFrequency frequency;

    @Column(nullable = false)
    private LocalDate nextBillingDate;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String contractReference;

    @Column(length = 100)
    private String propertyReference;

    @Column(length = 100)
    private String serviceType;

    @Column(nullable = false)
    private boolean active = true;
}
