package com.example.invoicing.entity.customer;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BillingAddress {
    @Column(name = "street_address") private String streetAddress;
    @Column(name = "postal_code") private String postalCode;
    @Column(name = "city") private String city;
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(name = "street_address_alt") private String streetAddressAlt;
    @Column(name = "city_alt") private String cityAlt;
    @Column(name = "country_code_alt", length = 2) private String countryCodeAlt;
}
