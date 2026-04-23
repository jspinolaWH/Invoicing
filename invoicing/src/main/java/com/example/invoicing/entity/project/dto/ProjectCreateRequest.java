package com.example.invoicing.entity.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ProjectCreateRequest {
    @NotBlank @Pattern(regexp = "\\d{6,9}", message = "customerNumber must be 6-9 digits")
    private String customerNumber;

    @NotBlank @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 50)
    private String linkedPropertyId;
}
