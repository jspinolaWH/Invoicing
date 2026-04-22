package com.example.invoicing.entity.surcharge;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.customer.DeliveryMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "surcharge_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurchargeConfig extends BaseAuditEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    private CustomerType customerType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean globalSurchargeEnabled = true;

    @Column(nullable = false)
    private boolean exemptFirstInvoice = false;

    @Column(nullable = false)
    private boolean requiresTariffInclusion = false;
}
