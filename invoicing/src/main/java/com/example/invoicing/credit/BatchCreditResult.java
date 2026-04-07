package com.example.invoicing.credit;

import com.example.invoicing.credit.dto.CreditNoteResponse;
import lombok.*;

import java.util.List;

@Data @AllArgsConstructor
public class BatchCreditResult {
    private int succeeded;
    private int failed;
    private List<CreditNoteResponse> creditedInvoices;
    private List<String> failureReasons;
}
