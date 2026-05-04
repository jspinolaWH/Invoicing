package com.example.invoicing.entity.pricelist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PriceListRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String tariffVariant;
    @NotNull private LocalDate validFrom;
    private LocalDate validTo;
    private String description;
    private boolean active = true;
}
