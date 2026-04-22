package com.example.invoicing.entity.pricelist.dto;

import com.example.invoicing.entity.pricelist.PriceList;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class PriceListResponse {
    private Long id;
    private String code;
    private String name;
    private String tariffVariant;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String description;
    private boolean active;
    private String createdBy;
    private Instant createdAt;
    private String lastModifiedBy;
    private Instant lastModifiedAt;

    public static PriceListResponse from(PriceList p) {
        PriceListResponse r = new PriceListResponse();
        r.id = p.getId();
        r.code = p.getCode();
        r.name = p.getName();
        r.tariffVariant = p.getTariffVariant();
        r.validFrom = p.getValidFrom();
        r.validTo = p.getValidTo();
        r.description = p.getDescription();
        r.active = p.isActive();
        r.createdBy = p.getCreatedBy();
        r.createdAt = p.getCreatedAt();
        r.lastModifiedBy = p.getLastModifiedBy();
        r.lastModifiedAt = p.getLastModifiedAt();
        return r;
    }
}
