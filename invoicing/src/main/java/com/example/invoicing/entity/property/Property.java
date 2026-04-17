package com.example.invoicing.entity.property;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "properties")
public class Property extends BaseAuditEntity {

    @Column(name = "property_id", nullable = false, unique = true, length = 50)
    private String propertyId;

    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "customer_number", length = 9)
    private String customerNumber;

    // Basic / Classification
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "municipality_code", length = 20)
    private String municipalityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_classification", length = 40)
    private BuildingClassification buildingClassification;

    @Column(name = "number_of_apartments")
    private Integer numberOfApartments;

    // R1 — Building information
    @Enumerated(EnumType.STRING)
    @Column(name = "building_status", length = 20)
    private BuildingStatus buildingStatus;

    @Column(name = "building_identifier", length = 30)
    private String buildingIdentifier;

    @Column(name = "building_type", length = 50)
    private String buildingType;

    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "usage_type", length = 100)
    private String usageType;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "total_area", precision = 10, scale = 2)
    private BigDecimal totalArea;

    // R3 — Address validity
    @Column(name = "address_valid_from")
    private LocalDate addressValidFrom;

    @Column(name = "address_valid_to")
    private LocalDate addressValidTo;

    // R9 — Oldest resident (birth year only — no identity data)
    @Column(name = "oldest_resident_year")
    private Integer oldestResidentYear;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyOwner> owners = new ArrayList<>();
}
