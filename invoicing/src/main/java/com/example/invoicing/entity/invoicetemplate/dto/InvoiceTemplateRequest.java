package com.example.invoicing.entity.invoicetemplate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceTemplateRequest {
    private String name;
    private String code;
    private Long numberSeriesId;
}
