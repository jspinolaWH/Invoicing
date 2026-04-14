package com.example.invoicing.entity.property;
import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "properties")
public class Property extends BaseAuditEntity {

    @Column(name = "property_id", nullable = false, unique = true, length = 50)
    private String propertyId;

    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "customer_number", length = 9)
    private String customerNumber;
}
