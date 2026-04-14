package com.example.invoicing.repository;
import com.example.invoicing.entity.customer.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CustomerBillingProfileRepository extends JpaRepository<Customer, Long> {
    @Query("""
        SELECT c FROM Customer c
        WHERE c.billingProfile.businessId IS NULL
           OR c.billingProfile.billingAddress.streetAddress IS NULL
           OR c.billingProfile.deliveryMethod IS NULL
        """)
    List<Customer> findCustomersWithMissingBillingData();
    List<Customer> findByBillingProfile_DeliveryMethod(DeliveryMethod deliveryMethod);

    java.util.Optional<Customer> findByBillingProfile_CustomerIdNumber(String customerIdNumber);

    @Query("""
        SELECT c FROM Customer c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(c.billingProfile.customerIdNumber) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(c.billingProfile.billingAddress.streetAddress) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
    org.springframework.data.domain.Page<Customer> search(
        @org.springframework.data.repository.query.Param("q") String q,
        org.springframework.data.domain.Pageable pageable);
}
