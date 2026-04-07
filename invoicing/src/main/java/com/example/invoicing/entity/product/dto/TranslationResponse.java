package com.example.invoicing.entity.product.dto;

import com.example.invoicing.entity.product.ProductTranslation;
import lombok.Getter;

@Getter
public class TranslationResponse {
    private final Long id;
    private final String locale;
    private final String name;

    private TranslationResponse(Long id, String locale, String name) {
        this.id = id;
        this.locale = locale;
        this.name = name;
    }

    public static TranslationResponse from(ProductTranslation t) {
        return new TranslationResponse(t.getId(), t.getLocale(), t.getName());
    }
}
