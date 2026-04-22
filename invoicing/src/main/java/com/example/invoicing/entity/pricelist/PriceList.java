package com.example.invoicing.entity.pricelist;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "price_lists")
public class PriceList extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "tariff_variant", length = 100)
    private String tariffVariant;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
