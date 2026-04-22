package com.example.invoicing.entity.property.dto;

import com.example.invoicing.entity.property.BuildingClassification;
import com.example.invoicing.entity.property.BuildingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class PropertyDetailResponse {
    private Long id;
    private String propertyId;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String customerNumber;

    // Basic / Classification
    private String countryCode;
    private String country;
    private String municipalityCode;
    private BuildingClassification buildingClassification;
    private Integer numberOfApartments;

    // R1 — Building information
    private BuildingStatus buildingStatus;
    private String buildingIdentifier;
    private String buildingType;
    private Integer constructionYear;
    private String usageType;
    private Integer numberOfFloors;
    private BigDecimal totalArea;

    // R3 — Address validity
    private LocalDate addressValidFrom;
    private LocalDate addressValidTo;

    // R9 — Oldest resident
    private Integer oldestResidentYear;

    // Invoice template
    private Long invoiceTemplateId;

    // R4 — Owners
    private List<PropertyOwnerDto> owners;

    @Data @Builder
    public static class PropertyOwnerDto {
        private Long id;
        private String ownerId;
        private String ownerName;
        private String ownerContactInfo;
        private String ownershipType;
        private BigDecimal ownershipPercentage;
    }
}
