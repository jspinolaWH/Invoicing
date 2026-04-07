package com.example.invoicing.entity.classification.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter
public class ReorderRequest {
    @NotNull private Long companyId;
    @NotEmpty private List<Long> orderedIds;
}
