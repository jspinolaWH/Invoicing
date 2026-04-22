package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.customer.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class BillingProfileRequest {
    @NotBlank @Pattern(regexp = "\\d{6,9}", message = "customerIdNumber must be 6-9 digits")
    private String customerIdNumber;
    @NotNull private DeliveryMethod deliveryMethod;
    @NotNull @Valid private BillingAddressRequest billingAddress;
    private String businessId;
    @NotBlank private String languageCode;
    @NotNull private InvoicingMode invoicingMode;
    private Long invoiceTemplateId;
    private LegalClassification defaultLegalClassification;
    private String defaultLedgerCode;
    private boolean invoicePerProject;
    private boolean allowExternalRecall;
    @Pattern(regexp = "\\d{6,9}", message = "parentCustomerNumber must be 6-9 digits")
    private String parentCustomerNumber;

    public BillingProfile toBillingProfile() {
        BillingAddress addr = new BillingAddress(
            billingAddress.getStreetAddress(), billingAddress.getPostalCode(),
            billingAddress.getCity(), billingAddress.getCountryCode(),
            billingAddress.getStreetAddressAlt(), billingAddress.getCityAlt(),
            billingAddress.getCountryCodeAlt(),
            billingAddress.getEmailAddress(), billingAddress.getEInvoicingAddress());
        BillingProfile profile = new BillingProfile(customerIdNumber, deliveryMethod, addr, businessId, languageCode, invoicingMode, invoiceTemplateId, defaultLegalClassification, defaultLedgerCode, invoicePerProject, allowExternalRecall);
        return profile;
    }
}
