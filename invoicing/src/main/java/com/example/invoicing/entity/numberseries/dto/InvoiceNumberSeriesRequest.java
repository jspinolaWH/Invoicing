package com.example.invoicing.entity.numberseries.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceNumberSeriesRequest {
    private String name;
    private String prefix;
    private String formatPattern;
}
