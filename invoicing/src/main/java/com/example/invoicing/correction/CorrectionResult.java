package com.example.invoicing.correction;

import lombok.*;
import java.util.List;

@Data @AllArgsConstructor
public class CorrectionResult {
    private Long originalInvoiceId;
    private Long creditNoteId;
    private List<Long> copiedEventIds;
    private Long targetCustomerId;
    private String message;
}
