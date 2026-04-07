package com.example.invoicing.entity.surcharge;

import com.example.invoicing.entity.BaseAuditEntity;
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
    @Column(nullable = false, unique = true)
    private DeliveryMethod deliveryMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean globalSurchargeEnabled = true;
}
