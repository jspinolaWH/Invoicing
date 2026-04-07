package com.example.invoicing.entity.billingevent.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkTransferResult {
    private List<Long> succeeded;
    private List<BulkTransferFailure> failed;
}
