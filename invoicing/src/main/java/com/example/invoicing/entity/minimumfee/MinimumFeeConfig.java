package com.example.invoicing.entity.minimumfee;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "minimum_fee_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinimumFeeConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 50)
    private String customerType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmountThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private boolean contractStartAdjustment = true;

    @Column(nullable = false)
    private boolean contractEndAdjustment = true;

    @Column(nullable = false, length = 50)
    private String adjustmentProductCode;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
