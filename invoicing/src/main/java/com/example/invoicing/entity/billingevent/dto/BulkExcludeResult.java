package com.example.invoicing.entity.billingevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkExcludeResult {
    private List<Long> succeeded;
    private List<BulkExcludeFailure> failed;
}
