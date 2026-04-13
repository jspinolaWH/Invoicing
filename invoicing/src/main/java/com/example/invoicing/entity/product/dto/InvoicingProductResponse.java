package com.example.invoicing.entity.product.dto;

import com.example.invoicing.entity.product.PricingUnit;
import com.example.invoicing.entity.product.Product;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class InvoicingProductResponse {
    private final Long id;
    private final String code;
    private final String nameFi;
    private final String nameSv;
    private final String nameEn;
    private final PricingUnit pricingUnit;
    private final BigDecimal defaultWasteFee;
    private final BigDecimal defaultTransportFee;
    private final BigDecimal defaultEcoFee;
    private final BigDecimal vatRate;
    private final boolean reverseChargeVat;

    private InvoicingProductResponse(Long id, String code, String nameFi, String nameSv, String nameEn,
                                     PricingUnit pricingUnit, BigDecimal defaultWasteFee,
                                     BigDecimal defaultTransportFee, BigDecimal defaultEcoFee,
                                     BigDecimal vatRate, boolean reverseChargeVat) {
        this.id = id;
        this.code = code;
        this.nameFi = nameFi;
        this.nameSv = nameSv;
        this.nameEn = nameEn;
        this.pricingUnit = pricingUnit;
        this.defaultWasteFee = defaultWasteFee;
        this.defaultTransportFee = defaultTransportFee;
        this.defaultEcoFee = defaultEcoFee;
        this.vatRate = vatRate;
        this.reverseChargeVat = reverseChargeVat;
    }

    public static InvoicingProductResponse from(Product p) {
        String nameFi = p.getTranslations().stream()
                .filter(t -> "fi".equals(t.getLocale())).findFirst()
                .map(t -> t.getName()).orElse(p.getCode());
        String nameSv = p.getTranslations().stream()
                .filter(t -> "sv".equals(t.getLocale())).findFirst()
                .map(t -> t.getName()).orElse(p.getCode());
        String nameEn = p.getTranslations().stream()
                .filter(t -> "en".equals(t.getLocale())).findFirst()
                .map(t -> t.getName()).orElse(p.getCode());

        return new InvoicingProductResponse(
                p.getId(), p.getCode(), nameFi, nameSv, nameEn,
                p.getPricingUnit(), p.getDefaultWasteFee(), p.getDefaultTransportFee(),
                p.getDefaultEcoFee(), p.getVatRate(), p.isReverseChargeVat());
    }
}
