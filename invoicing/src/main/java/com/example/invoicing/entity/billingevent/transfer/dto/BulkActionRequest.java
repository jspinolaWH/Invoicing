package com.example.invoicing.entity.billingevent.transfer.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequest {
    @NotEmpty
    private List<Long> eventIds;
}
