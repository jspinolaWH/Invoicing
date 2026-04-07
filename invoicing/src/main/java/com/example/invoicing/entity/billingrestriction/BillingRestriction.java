package com.example.invoicing.entity.billingrestriction;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "billing_restrictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRestriction extends BaseAuditEntity {

    @Column(length = 100)
    private String municipality;

    @Column(length = 50)
    private String customerType;

    @Column(length = 100)
    private String serviceType;

    @Column
    private Long locationId;

    @Column(precision = 10, scale = 2)
    private BigDecimal minAmount;

    @Column(length = 50)
    private String period;

    @Column(length = 30)
    private String serviceResponsibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingType billingType;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
