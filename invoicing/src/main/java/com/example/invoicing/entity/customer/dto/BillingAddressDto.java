package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.BillingAddress;
import lombok.*;

@Getter @Builder
public class BillingAddressDto {
    private String streetAddress;
    private String postalCode;
    private String city;
    private String countryCode;
    private String streetAddressAlt;
    private String cityAlt;
    private String countryCodeAlt;

    public static BillingAddressDto from(BillingAddress a) {
        if (a == null) return null;
        return BillingAddressDto.builder()
            .streetAddress(a.getStreetAddress()).postalCode(a.getPostalCode())
            .city(a.getCity()).countryCode(a.getCountryCode())
            .streetAddressAlt(a.getStreetAddressAlt()).cityAlt(a.getCityAlt())
            .countryCodeAlt(a.getCountryCodeAlt()).build();
    }
}
