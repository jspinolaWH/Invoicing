package com.example.invoicing.entity.invoice;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.seller")
@Getter @Setter
public class SellerDetails {
    private String vatNumber;
    private String organisationName;
    private String streetName;
    private String townName;
    private String postCode;
    private String countryCode;
    private String bankAccountIban;
    private String bicCode;
}
