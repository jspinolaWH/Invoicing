package com.example.invoicing.entity.weighbridge;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "weighbridge_integration_configs",
    uniqueConstraints = @UniqueConstraint(columnNames = "customer_number"))
@Getter @Setter @NoArgsConstructor
public class WeighbridgeIntegrationConfig extends BaseAuditEntity {

    @Column(name = "customer_number", nullable = false, length = 9, unique = true)
    private String customerNumber;

    @Column(name = "external_system_id", length = 100)
    private String externalSystemId;

    @Column(name = "default_product_code", length = 100)
    private String defaultProductCode;

    @Column(name = "site_reference", length = 200)
    private String siteReference;

    @Column(nullable = false)
    private boolean active = true;
}
