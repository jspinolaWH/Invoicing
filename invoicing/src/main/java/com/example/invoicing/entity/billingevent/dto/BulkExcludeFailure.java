package com.example.invoicing.entity.billingevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkExcludeFailure {
    private Long eventId;
    private String reason;
}
