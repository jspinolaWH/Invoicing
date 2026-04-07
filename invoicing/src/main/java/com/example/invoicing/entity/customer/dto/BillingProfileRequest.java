package com.example.invoicing.entity.customer.dto;
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

    public BillingProfile toBillingProfile() {
        BillingAddress addr = new BillingAddress(
            billingAddress.getStreetAddress(), billingAddress.getPostalCode(),
            billingAddress.getCity(), billingAddress.getCountryCode(),
            billingAddress.getStreetAddressAlt(), billingAddress.getCityAlt(),
            billingAddress.getCountryCodeAlt());
        return new BillingProfile(customerIdNumber, deliveryMethod, addr, businessId, languageCode, invoicingMode);
    }
}
