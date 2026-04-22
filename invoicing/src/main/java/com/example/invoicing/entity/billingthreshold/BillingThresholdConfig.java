package com.example.invoicing.entity.billingthreshold;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "billing_threshold_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingThresholdConfig extends BaseAuditEntity {

    @Column(name = "service_responsibility", nullable = false, length = 50)
    private String serviceResponsibility;

    @Column(name = "annual_euro_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualEuroLimit;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
