package com.example.invoicing.repository;
import com.example.invoicing.entity.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("""
        SELECT p FROM Property p
        WHERE LOWER(p.streetAddress) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.propertyId) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    Page<Property> search(@Param("q") String q, Pageable pageable);

    @Query("""
        SELECT p FROM Property p
        WHERE p.customerNumber = :customerNumber
          AND (:q IS NULL OR LOWER(p.streetAddress) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.propertyId) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY p.streetAddress ASC
        """)
    java.util.List<Property> findByCustomerNumber(
        @Param("customerNumber") String customerNumber,
        @Param("q") String q,
        Pageable pageable);

    java.util.Optional<Property> findByPropertyId(String propertyId);
}
