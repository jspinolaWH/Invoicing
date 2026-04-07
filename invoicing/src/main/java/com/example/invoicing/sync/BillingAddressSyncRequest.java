package com.example.invoicing.sync;

import lombok.Data;

@Data
public class BillingAddressSyncRequest {
    private String customerNumber;   // matches BillingProfile.customerIdNumber
    private String streetAddress;
    private String postalCode;
    private String city;
    private String countryCode;
    private String streetAddressAlt;
    private String cityAlt;
    private String countryCodeAlt;
}
