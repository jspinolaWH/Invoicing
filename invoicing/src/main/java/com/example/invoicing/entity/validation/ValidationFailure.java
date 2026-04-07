package com.example.invoicing.entity.validation;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationFailure {
    private Long entityId;
    private String entityType;
    private String field;
    private String rule;
    private String ruleCode;
    private ValidationSeverity severity;
    private String description;
}
