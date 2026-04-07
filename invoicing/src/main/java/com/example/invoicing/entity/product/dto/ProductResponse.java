package com.example.invoicing.entity.product.dto;

import com.example.invoicing.entity.product.PricingUnit;
import com.example.invoicing.entity.product.Product;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductResponse {
    private final Long id;
    private final String code;
    private final PricingUnit pricingUnit;
    private final boolean reverseChargeVat;
    private final List<TranslationResponse> translations;

    private ProductResponse(Long id, String code, PricingUnit pricingUnit,
                            boolean reverseChargeVat, List<TranslationResponse> translations) {
        this.id = id;
        this.code = code;
        this.pricingUnit = pricingUnit;
        this.reverseChargeVat = reverseChargeVat;
        this.translations = translations;
    }

    public static ProductResponse from(Product p) {
        List<TranslationResponse> translations = p.getTranslations().stream()
                .map(TranslationResponse::from)
                .toList();
        return new ProductResponse(p.getId(), p.getCode(), p.getPricingUnit(),
                p.isReverseChargeVat(), translations);
    }
}
