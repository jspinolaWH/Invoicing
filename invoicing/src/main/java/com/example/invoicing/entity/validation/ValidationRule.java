package com.example.invoicing.entity.validation;
import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "validation_rules",
       indexes = @Index(name = "idx_val_rule_company", columnList = "company_id, active"))
public class ValidationRule extends BaseAuditEntity {
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Enumerated(EnumType.STRING) @Column(name = "rule_type", nullable = false, length = 30) private ValidationRuleType ruleType;
    @Column(name = "rule_code", nullable = false, length = 80) private String ruleCode;
    @Column(name = "config", columnDefinition = "TEXT") private String config;
    @Column(name = "blocking", nullable = false) private boolean blocking = true;
    @Column(name = "active", nullable = false) private boolean active = true;
    @Column(name = "description", length = 500) private String description;
}
