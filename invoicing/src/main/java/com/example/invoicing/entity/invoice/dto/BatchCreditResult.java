package com.example.invoicing.entity.invoice.dto;

import com.example.invoicing.entity.invoice.dto.CreditNoteResponse;
import lombok.*;

import java.util.List;

@Data @AllArgsConstructor
public class BatchCreditResult {
    private int succeeded;
    private int failed;
    private List<CreditNoteResponse> creditedInvoices;
    private List<String> failureReasons;
}
