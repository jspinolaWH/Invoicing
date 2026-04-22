package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.classification.LegalClassification;
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
    private Long invoiceTemplateId;
    private LegalClassification defaultLegalClassification;
    private String defaultLedgerCode;
    private boolean invoicePerProject;
    private boolean allowExternalRecall;

    public static BillingProfileDto from(BillingProfile p) {
        if (p == null) return null;
        return BillingProfileDto.builder()
            .customerIdNumber(p.getCustomerIdNumber())
            .deliveryMethod(p.getDeliveryMethod())
            .billingAddress(BillingAddressDto.from(p.getBillingAddress()))
            .businessId(p.getBusinessId())
            .languageCode(p.getLanguageCode())
            .invoicingMode(p.getInvoicingMode())
            .invoiceTemplateId(p.getInvoiceTemplateId())
            .defaultLegalClassification(p.getDefaultLegalClassification())
            .defaultLedgerCode(p.getDefaultLedgerCode())
            .invoicePerProject(p.isInvoicePerProject())
            .allowExternalRecall(p.isAllowExternalRecall())
            .build();
    }
}
