package com.example.invoicing.entity.product;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "product_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "locale"})
)
public class ProductTranslation extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 5)
    private String locale;

    @Column(nullable = false)
    private String name;
}
