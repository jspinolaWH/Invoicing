package com.example.invoicing.entity.pricelist.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PriceListRequest {
    private String code;
    private String name;
    private String tariffVariant;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String description;
    private boolean active = true;
}
