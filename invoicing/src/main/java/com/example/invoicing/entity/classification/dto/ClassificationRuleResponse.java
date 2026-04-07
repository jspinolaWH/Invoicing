package com.example.invoicing.entity.classification.dto;
import com.example.invoicing.entity.classification.*;
import com.example.invoicing.entity.customer.CustomerType;
import lombok.*;
import java.time.Instant;

@Getter @Builder
public class ClassificationRuleResponse {
    private Long id;
    private Long companyId;
    private int priority;
    private String label;
    private CustomerType customerTypeCondition;
    private String productCodeCondition;
    private String regionCondition;
    private LegalClassification resultClassification;
    private boolean active;
    private String createdBy;
    private Instant lastModifiedAt;

    public static ClassificationRuleResponse from(ClassificationRule r) {
        return ClassificationRuleResponse.builder()
            .id(r.getId()).companyId(r.getCompanyId()).priority(r.getPriority())
            .label(r.getLabel()).customerTypeCondition(r.getCustomerTypeCondition())
            .productCodeCondition(r.getProductCodeCondition()).regionCondition(r.getRegionCondition())
            .resultClassification(r.getResultClassification()).active(r.isActive())
            .createdBy(r.getCreatedBy()).lastModifiedAt(r.getLastModifiedAt()).build();
    }
}
