package com.example.invoicing.bundling;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "bundling_rules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_number", "product_group"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundlingRule extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "product_group", nullable = false, length = 100)
    private String productGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BundlingType bundlingType;

    @Column(length = 255)
    private String description;
}
