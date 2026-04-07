package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.*;
import lombok.*;

@Getter @Builder
public class BillingProfileDto {
    private String customerIdNumber;
    private DeliveryMethod deliveryMethod;
    private BillingAddressDto billingAddress;
    private String businessId;
    private String languageCode;
    private InvoicingMode invoicingMode;

    public static BillingProfileDto from(BillingProfile p) {
        if (p == null) return null;
        return BillingProfileDto.builder()
            .customerIdNumber(p.getCustomerIdNumber())
            .deliveryMethod(p.getDeliveryMethod())
            .billingAddress(BillingAddressDto.from(p.getBillingAddress()))
            .businessId(p.getBusinessId())
            .languageCode(p.getLanguageCode())
            .invoicingMode(p.getInvoicingMode())
            .build();
    }
}
