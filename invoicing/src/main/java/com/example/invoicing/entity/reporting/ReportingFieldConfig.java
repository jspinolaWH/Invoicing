package com.example.invoicing.entity.reporting;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "reporting_field_configs",
       indexes = @Index(name = "idx_reporting_field_configs_company", columnList = "company_id"),
       uniqueConstraints = @UniqueConstraint(name = "uq_reporting_field_company", columnNames = {"company_id", "field_name"}))
public class ReportingFieldConfig extends BaseAuditEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "label_override", length = 100)
    private String labelOverride;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
