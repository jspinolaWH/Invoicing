package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.Customer;
import lombok.*;

@Getter @Builder
public class CustomerSearchResult {
    private Long id;
    private String customerNumber;
    private String name;
    private String streetAddress;
    private String city;
    private String postalCode;

    public static CustomerSearchResult from(Customer c) {
        String street = null, city = null, postal = null;
        if (c.getBillingProfile() != null && c.getBillingProfile().getBillingAddress() != null) {
            street = c.getBillingProfile().getBillingAddress().getStreetAddress();
            city   = c.getBillingProfile().getBillingAddress().getCity();
            postal = c.getBillingProfile().getBillingAddress().getPostalCode();
        }
        return CustomerSearchResult.builder()
            .id(c.getId())
            .customerNumber(c.getBillingProfile() != null ? c.getBillingProfile().getCustomerIdNumber() : null)
            .name(c.getName())
            .streetAddress(street)
            .city(city)
            .postalCode(postal)
            .build();
    }
}
