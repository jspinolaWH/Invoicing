package com.example.invoicing.entity.property;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "property_owners")
public class PropertyOwner extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "owner_id", length = 50)
    private String ownerId;

    @Column(name = "owner_name", length = 200)
    private String ownerName;

    @Column(name = "owner_contact_info", length = 500)
    private String ownerContactInfo;

    @Column(name = "ownership_type", length = 50)
    private String ownershipType;

    @Column(name = "ownership_percentage", precision = 5, scale = 2)
    private BigDecimal ownershipPercentage;
}
