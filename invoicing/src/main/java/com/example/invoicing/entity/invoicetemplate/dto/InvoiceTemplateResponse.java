package com.example.invoicing.entity.invoicetemplate.dto;

import com.example.invoicing.entity.invoicetemplate.InvoiceTemplate;
import lombok.Getter;

import java.time.Instant;

@Getter
public class InvoiceTemplateResponse {
    private final Long id;
    private final String name;
    private final String code;
    private final Long numberSeriesId;
    private final String numberSeriesName;
    private final String createdBy;
    private final Instant createdAt;

    private InvoiceTemplateResponse(Long id, String name, String code,
                                    Long numberSeriesId, String numberSeriesName,
                                    String createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.numberSeriesId = numberSeriesId;
        this.numberSeriesName = numberSeriesName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static InvoiceTemplateResponse from(InvoiceTemplate t) {
        Long seriesId = t.getNumberSeries() != null ? t.getNumberSeries().getId() : null;
        String seriesName = t.getNumberSeries() != null ? t.getNumberSeries().getName() : null;
        return new InvoiceTemplateResponse(
                t.getId(), t.getName(), t.getCode(),
                seriesId, seriesName,
                t.getCreatedBy(), t.getCreatedAt());
    }
}
