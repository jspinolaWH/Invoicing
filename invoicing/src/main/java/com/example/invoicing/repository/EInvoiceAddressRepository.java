package com.example.invoicing.repository;
import com.example.invoicing.entity.customer.EInvoiceAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EInvoiceAddressRepository extends JpaRepository<EInvoiceAddress, Long> {
    Optional<EInvoiceAddress> findByCustomer_Id(Long customerId);
    Optional<EInvoiceAddress> findByCustomer_IdAndManuallyLockedFalse(Long customerId);
    @Query("SELECT e FROM EInvoiceAddress e WHERE e.manuallyLocked = true")
    List<EInvoiceAddress> findAllManuallyLocked();

    @Query("SELECT e FROM EInvoiceAddress e WHERE e.registeredWithOperator = false AND e.manuallyLocked = false AND e.customer.billingProfile.deliveryMethod = 'E_INVOICE'")
    List<EInvoiceAddress> findUnregistered();

    @Query("SELECT e FROM EInvoiceAddress e WHERE e.registeredWithOperator = true AND (e.customer.billingProfile.deliveryMethod IS NULL OR e.customer.billingProfile.deliveryMethod <> 'E_INVOICE')")
    List<EInvoiceAddress> findRegisteredButInactive();
}
