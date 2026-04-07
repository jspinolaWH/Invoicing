package com.example.invoicing.entity.numberseries.dto;

import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import lombok.Getter;

import java.time.Instant;

@Getter
public class InvoiceNumberSeriesResponse {
    private final Long id;
    private final String name;
    private final String prefix;
    private final String formatPattern;
    private final Long currentCounter;
    private final int releasedNumbersCount;
    private final String createdBy;
    private final Instant createdAt;

    private InvoiceNumberSeriesResponse(Long id, String name, String prefix, String formatPattern,
                                        Long currentCounter, int releasedNumbersCount,
                                        String createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.formatPattern = formatPattern;
        this.currentCounter = currentCounter;
        this.releasedNumbersCount = releasedNumbersCount;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static InvoiceNumberSeriesResponse from(InvoiceNumberSeries s) {
        return new InvoiceNumberSeriesResponse(
                s.getId(), s.getName(), s.getPrefix(), s.getFormatPattern(),
                s.getCurrentCounter(), s.getReleasedNumbers().size(),
                s.getCreatedBy(), s.getCreatedAt());
    }
}
