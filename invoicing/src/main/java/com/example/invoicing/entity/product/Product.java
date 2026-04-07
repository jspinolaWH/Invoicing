package com.example.invoicing.entity.product;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "product_group_code", length = 100)
    private String productGroupCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingUnit pricingUnit;

    @Column(nullable = false)
    private boolean reverseChargeVat;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTranslation> translations = new ArrayList<>();
}
