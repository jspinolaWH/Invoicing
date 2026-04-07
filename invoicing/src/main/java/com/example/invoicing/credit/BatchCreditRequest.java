package com.example.invoicing.credit;

import lombok.Data;
import java.util.List;

@Data
public class BatchCreditRequest {
    private List<Long> invoiceIds;
    private String customText;
    private String internalComment;
}
