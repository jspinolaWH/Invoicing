package com.example.invoicing.entity.validation.dto;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
public class ValidationRequest {
    @NotNull private Long companyId;
}
