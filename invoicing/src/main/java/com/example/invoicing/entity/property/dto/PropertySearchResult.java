package com.example.invoicing.entity.property.dto;
import com.example.invoicing.entity.property.Property;
import lombok.*;

@Getter @Builder
public class PropertySearchResult {
    private Long id;
    private String propertyId;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String customerNumber;

    public static PropertySearchResult from(Property p) {
        return PropertySearchResult.builder()
            .id(p.getId())
            .propertyId(p.getPropertyId())
            .streetAddress(p.getStreetAddress())
            .city(p.getCity())
            .postalCode(p.getPostalCode())
            .customerNumber(p.getCustomerNumber())
            .build();
    }
}
