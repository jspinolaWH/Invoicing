package com.example.invoicing.sharedservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyGroupRepository extends JpaRepository<PropertyGroup, Long> {

    @Query("SELECT DISTINCT p.propertyGroup FROM SharedServiceParticipant p WHERE p.customerNumber = :customerNumber")
    List<PropertyGroup> findGroupsByCustomerNumber(@Param("customerNumber") String customerNumber);

    List<PropertyGroup> findByActiveTrueOrderByNameAsc();
}
