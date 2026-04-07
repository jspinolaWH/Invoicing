package com.example.invoicing.entity.validation.dto;
import com.example.invoicing.entity.validation.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ValidationRuleRequest {
    @NotNull private Long companyId;
    @NotNull private ValidationRuleType ruleType;
    @NotBlank @Size(max = 80) private String ruleCode;
    private String config;
    private boolean blocking = true;
    private boolean active = true;
    @Size(max = 500) private String description;
}
