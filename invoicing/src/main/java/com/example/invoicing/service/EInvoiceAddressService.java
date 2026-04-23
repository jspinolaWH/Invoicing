package com.example.invoicing.service;
import com.example.invoicing.entity.customer.*;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EInvoiceAddressService {
    private final EInvoiceAddressRepository addressRepo;
    private final CustomerBillingProfileRepository customerRepo;

    @Transactional(readOnly = true)
    public EInvoiceAddressResponse getAddress(Long customerId) {
        return addressRepo.findByCustomer_Id(customerId)
            .map(EInvoiceAddressResponse::from)
            .orElse(EInvoiceAddressResponse.empty(customerId));
    }

    @Transactional
    public EInvoiceAddressResponse setAddress(Long customerId, EInvoiceAddressRequest request) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        EInvoiceAddress entity = addressRepo.findByCustomer_Id(customerId)
            .orElse(new EInvoiceAddress());
        entity.setCustomer(customer);
        entity.setAddress(request.getAddress());
        entity.setOperatorCode(request.getOperatorCode());
        entity.setManuallyLocked(request.isLock());
        entity.setLockReason(request.isLock() ? request.getLockReason() : null);
        return EInvoiceAddressResponse.from(addressRepo.save(entity));
    }

    @Transactional
    public void clearAddress(Long customerId) {
        addressRepo.findByCustomer_Id(customerId).ifPresent(addressRepo::delete);
    }

    @Transactional
    public void updateFromOperator(Long customerId, String address, String operatorCode) {
        addressRepo.findByCustomer_IdAndManuallyLockedFalse(customerId).ifPresentOrElse(
            existing -> { existing.setAddress(address); existing.setOperatorCode(operatorCode); addressRepo.save(existing); },
            () -> {
                boolean locked = addressRepo.findByCustomer_Id(customerId).isPresent();
                if (locked) {
                    log.warn("Skipped operator update for customerId={}: address is manually locked", customerId);
                } else {
                    Customer customer = customerRepo.findById(customerId)
                        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Customer not found: " + customerId));
                    EInvoiceAddress created = new EInvoiceAddress();
                    created.setCustomer(customer);
                    created.setAddress(address);
                    created.setOperatorCode(operatorCode);
                    addressRepo.save(created);
                    log.info("Created new EInvoiceAddress for customerId={} via operator update", customerId);
                }
            }
        );
    }
}
