package com.example.invoicing.repository;

import com.example.invoicing.entity.property.PropertyOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, Long> {

    List<PropertyOwner> findByPropertyId(Long propertyId);
}
