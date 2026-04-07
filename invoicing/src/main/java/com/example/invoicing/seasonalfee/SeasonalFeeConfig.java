package com.example.invoicing.seasonalfee;

import com.example.invoicing.billingcycle.BillingFrequency;
import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "seasonal_fee_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalFeeConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 20)
    private String customerNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingFrequency billingFrequency;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate nextDueDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String propertyReference;

    @Column(length = 255)
    private String description;
}
