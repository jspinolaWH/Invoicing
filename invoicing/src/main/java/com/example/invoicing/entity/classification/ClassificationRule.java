package com.example.invoicing.entity.classification;
import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.customer.CustomerType;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "classification_rules",
    indexes = {
        @Index(name = "idx_class_rule_priority", columnList = "company_id, priority"),
        @Index(name = "idx_class_rule_active",   columnList = "company_id, active")
    })
public class ClassificationRule extends BaseAuditEntity {
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "priority", nullable = false) private int priority;
    @Enumerated(EnumType.STRING) @Column(name = "customer_type_condition", length = 20) private CustomerType customerTypeCondition;
    @Column(name = "product_code_condition", length = 50) private String productCodeCondition;
    @Column(name = "region_condition", length = 50) private String regionCondition;
    @Enumerated(EnumType.STRING) @Column(name = "result_classification", nullable = false, length = 20) private LegalClassification resultClassification;
    @Column(name = "label", length = 200) private String label;
    @Column(name = "active", nullable = false) private boolean active = true;
}
