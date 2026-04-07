package com.example.invoicing.entity.validation.dto;
import com.example.invoicing.entity.validation.*;
import lombok.*;
import java.time.Instant;

@Getter @Builder
public class ValidationRuleResponse {
    private Long id;
    private Long companyId;
    private ValidationRuleType ruleType;
    private String ruleCode;
    private String config;
    private boolean blocking;
    private boolean active;
    private String description;
    private Instant lastModifiedAt;

    public static ValidationRuleResponse from(ValidationRule v) {
        return ValidationRuleResponse.builder()
            .id(v.getId()).companyId(v.getCompanyId()).ruleType(v.getRuleType())
            .ruleCode(v.getRuleCode()).config(v.getConfig()).blocking(v.isBlocking())
            .active(v.isActive()).description(v.getDescription())
            .lastModifiedAt(v.getLastModifiedAt()).build();
    }
}
