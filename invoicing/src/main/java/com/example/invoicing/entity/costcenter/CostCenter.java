package com.example.invoicing.entity.costcenter;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cost_centers")
public class CostCenter extends BaseAuditEntity {

    @Column(nullable = false)
    private String productSegment;

    @Column(nullable = false)
    private String receptionSegment;

    @Column(nullable = false)
    private String responsibilitySegment;

    @Column(nullable = false, unique = true)
    private String compositeCode;

    @Column
    private String description;
}
