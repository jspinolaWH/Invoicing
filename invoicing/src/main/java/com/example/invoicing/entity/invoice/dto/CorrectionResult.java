package com.example.invoicing.entity.invoice.dto;

import lombok.*;
import java.util.List;

@Data @AllArgsConstructor
public class CorrectionResult {
    private Long originalInvoiceId;
    private Long creditNoteId;
    private List<Long> copiedEventIds;
    private Long targetCustomerId;
    private Long draftInvoiceId;
    private String message;
}
