package com.example.invoicing.entity.classification.dto;
import com.example.invoicing.entity.classification.*;
import com.example.invoicing.entity.customer.CustomerType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ClassificationRuleRequest {
    @NotNull private Long companyId;
    @Min(1) private int priority;
    @Size(max = 200) private String label;
    private CustomerType customerTypeCondition;
    private String productCodeCondition;
    private String regionCondition;
    @NotNull private LegalClassification resultClassification;
    private boolean active = true;

    public ClassificationRule toEntity(Long companyId) {
        ClassificationRule rule = new ClassificationRule();
        rule.setCompanyId(companyId);
        applyTo(rule);
        return rule;
    }

    public void applyTo(ClassificationRule rule) {
        rule.setPriority(priority); rule.setLabel(label);
        rule.setCustomerTypeCondition(customerTypeCondition);
        rule.setProductCodeCondition(productCodeCondition);
        rule.setRegionCondition(regionCondition);
        rule.setResultClassification(resultClassification);
        rule.setActive(active);
    }
}
