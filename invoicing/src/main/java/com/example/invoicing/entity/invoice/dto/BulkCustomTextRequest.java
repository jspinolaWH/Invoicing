package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.util.List;

@Data
public class BulkCustomTextRequest {
    private List<Long> invoiceIds;
    private String customText;
}
