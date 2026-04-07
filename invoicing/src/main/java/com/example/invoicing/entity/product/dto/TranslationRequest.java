package com.example.invoicing.entity.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationRequest {
    private String locale;
    private String name;
}
