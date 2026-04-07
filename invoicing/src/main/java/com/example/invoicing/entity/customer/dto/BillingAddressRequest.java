package com.example.invoicing.entity.customer.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class BillingAddressRequest {
    @NotBlank private String streetAddress;
    @NotBlank private String postalCode;
    @NotBlank private String city;
    @NotBlank @Size(min = 2, max = 2) private String countryCode;
    private String streetAddressAlt;
    private String cityAlt;
    private String countryCodeAlt;
}
