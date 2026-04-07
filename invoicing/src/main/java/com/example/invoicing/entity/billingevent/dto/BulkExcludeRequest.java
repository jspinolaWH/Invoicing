package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkExcludeRequest {

    @NotEmpty(message = "At least one event ID is required")
    private List<Long> eventIds;

    @NotBlank(message = "Exclusion reason is required for bulk exclusion")
    private String exclusionReason;
}
