package com.example.invoicing.entity.invoicetemplate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceTemplateRequest {
    @NotBlank private String name;
    @NotBlank private String code;
    private Long numberSeriesId;
}
