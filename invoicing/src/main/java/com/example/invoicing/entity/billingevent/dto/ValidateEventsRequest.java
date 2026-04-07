package com.example.invoicing.entity.billingevent.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ValidateEventsRequest {
    @NotEmpty
    private List<Long> eventIds;
}
