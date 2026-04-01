package com.example.invoicing.entity.vat.dto;

import com.example.invoicing.entity.vat.VatRate;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class VatRateResponse {
    private Long id;
    private String code;
    private BigDecimal rate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String createdBy;
    private Instant createdAt;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static VatRateResponse from(VatRate v) {
        VatRateResponse r = new VatRateResponse();
        r.id = v.getId();
        r.code = v.getCode();
        r.rate = v.getRate();
        r.validFrom = v.getValidFrom();
        r.validTo = v.getValidTo();
        r.createdBy = v.getCreatedBy();
        r.createdAt = v.getCreatedAt();
        r.lastModifiedBy = v.getLastModifiedBy();
        r.lastModifiedAt = v.getLastModifiedAt();
        return r;
    }
}
