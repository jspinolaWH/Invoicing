package com.example.invoicing.service;
import com.example.invoicing.common.exception.BillingRunLockException;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.entity.customer.event.BillingAddressChangedEvent;
import com.example.invoicing.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingProfileService {
    private final CustomerBillingProfileRepository customerRepo;
    private final ActiveRunLockRepository runLockRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public BillingProfileResponse getBillingProfile(Long customerId) {
        return BillingProfileResponse.from(findCustomer(customerId));
    }

    @Transactional
    public BillingProfileResponse updateBillingProfile(Long customerId, BillingProfileRequest request) {
        if (runLockRepo.existsByCustomerId(customerId)) {
            throw new BillingRunLockException(
                "Invoice processing in progress. Address changes cannot be made during this time.");
        }
        Customer customer = findCustomer(customerId);
        customer.setBillingProfile(request.toBillingProfile());
        Customer saved = customerRepo.save(customer);
        eventPublisher.publishEvent(new BillingAddressChangedEvent(this, customerId));
        return BillingProfileResponse.from(saved);
    }

    private Customer findCustomer(Long id) {
        return customerRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }
}
