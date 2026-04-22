package com.example.invoicing.service;

import com.example.invoicing.entity.property.Property;
import com.example.invoicing.entity.property.dto.PropertyDetailResponse;
import com.example.invoicing.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public PropertyDetailResponse getPropertyDetail(Long id) {
        Property p = propertyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));
        return toDetailResponse(p);
    }

    @Transactional
    public PropertyDetailResponse updateTemplate(Long id, Long invoiceTemplateId) {
        Property p = propertyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));
        p.setInvoiceTemplateId(invoiceTemplateId);
        return toDetailResponse(propertyRepository.save(p));
    }

    private PropertyDetailResponse toDetailResponse(Property p) {
        return PropertyDetailResponse.builder()
            .id(p.getId())
            .propertyId(p.getPropertyId())
            .streetAddress(p.getStreetAddress())
            .city(p.getCity())
            .postalCode(p.getPostalCode())
            .customerNumber(p.getCustomerNumber())
            .countryCode(p.getCountryCode())
            .country(p.getCountry())
            .municipalityCode(p.getMunicipalityCode())
            .buildingClassification(p.getBuildingClassification())
            .numberOfApartments(p.getNumberOfApartments())
            .buildingStatus(p.getBuildingStatus())
            .buildingIdentifier(p.getBuildingIdentifier())
            .buildingType(p.getBuildingType())
            .constructionYear(p.getConstructionYear())
            .usageType(p.getUsageType())
            .numberOfFloors(p.getNumberOfFloors())
            .totalArea(p.getTotalArea())
            .addressValidFrom(p.getAddressValidFrom())
            .addressValidTo(p.getAddressValidTo())
            .oldestResidentYear(p.getOldestResidentYear())
            .invoiceTemplateId(p.getInvoiceTemplateId())
            .owners(p.getOwners().stream().map(o -> PropertyDetailResponse.PropertyOwnerDto.builder()
                .id(o.getId())
                .ownerId(o.getOwnerId())
                .ownerName(o.getOwnerName())
                .ownerContactInfo(o.getOwnerContactInfo())
                .ownershipType(o.getOwnershipType())
                .ownershipPercentage(o.getOwnershipPercentage())
                .build()).toList())
            .build();
    }
}
