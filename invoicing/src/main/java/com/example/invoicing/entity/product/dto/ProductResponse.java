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
    private final Long defaultAccountingAccountId;
    private final String defaultAccountingAccountCode;
    private final Long defaultCostCenterId;
    private final String defaultCostCenterCode;
    private final Long priceListId;
    private final String priceListCode;

    private ProductResponse(Long id, String code, PricingUnit pricingUnit,
                            boolean reverseChargeVat, List<TranslationResponse> translations,
                            Long defaultAccountingAccountId, String defaultAccountingAccountCode,
                            Long defaultCostCenterId, String defaultCostCenterCode,
                            Long priceListId, String priceListCode) {
        this.id = id;
        this.code = code;
        this.pricingUnit = pricingUnit;
        this.reverseChargeVat = reverseChargeVat;
        this.translations = translations;
        this.defaultAccountingAccountId = defaultAccountingAccountId;
        this.defaultAccountingAccountCode = defaultAccountingAccountCode;
        this.defaultCostCenterId = defaultCostCenterId;
        this.defaultCostCenterCode = defaultCostCenterCode;
        this.priceListId = priceListId;
        this.priceListCode = priceListCode;
    }

    public static ProductResponse from(Product p) {
        List<TranslationResponse> translations = p.getTranslations().stream()
                .map(TranslationResponse::from)
                .toList();
        return new ProductResponse(
                p.getId(), p.getCode(), p.getPricingUnit(), p.isReverseChargeVat(), translations,
                p.getDefaultAccountingAccount() != null ? p.getDefaultAccountingAccount().getId() : null,
                p.getDefaultAccountingAccount() != null ? p.getDefaultAccountingAccount().getCode() : null,
                p.getDefaultCostCenter() != null ? p.getDefaultCostCenter().getId() : null,
                p.getDefaultCostCenter() != null ? p.getDefaultCostCenter().getCompositeCode() : null,
                p.getPriceList() != null ? p.getPriceList().getId() : null,
                p.getPriceList() != null ? p.getPriceList().getCode() : null
        );
    }
}
