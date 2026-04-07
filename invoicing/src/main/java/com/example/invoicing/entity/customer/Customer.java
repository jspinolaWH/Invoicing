package com.example.invoicing.entity.customer;
import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "customers")
public class Customer extends BaseAuditEntity {
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "customer_type", nullable = false, length = 20) private CustomerType customerType;
    @Embedded private BillingProfile billingProfile;
}
