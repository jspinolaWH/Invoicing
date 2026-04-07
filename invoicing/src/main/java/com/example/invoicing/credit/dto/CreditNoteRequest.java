package com.example.invoicing.credit.dto;

import com.example.invoicing.credit.CreditType;
import lombok.Data;

import java.util.List;

@Data
public class CreditNoteRequest {
    private CreditType creditType = CreditType.FULL;
    private List<Long> lineItemIds;
    private String customText;
    private String internalComment;
}
