package com.example.invoicing.service;
import com.example.invoicing.common.exception.BillingRunLockException;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.CustomerAuditLog;
import com.example.invoicing.entity.customer.DeliveryMethod;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.entity.customer.event.BillingAddressChangedEvent;
import com.example.invoicing.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BillingProfileService {
    private final CustomerBillingProfileRepository customerRepo;
    private final ActiveRunLockRepository runLockRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerService customerService;
    private final EInvoiceAddressService einvoiceAddressService;
    private final CustomerAuditLogRepository auditLogRepo;
    private final AuditorAware<String> auditorAware;

    @Transactional(readOnly = true)
    public BillingProfileResponse getBillingProfile(Long customerId) {
        return BillingProfileResponse.from(findCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public List<CustomerAuditLogResponse> getAuditLog(Long customerId) {
        return auditLogRepo.findByCustomerIdOrderByChangedAtDesc(customerId)
            .stream().map(CustomerAuditLogResponse::from).toList();
    }

    @Transactional
    public BillingProfileResponse updateBillingProfile(Long customerId, BillingProfileRequest request) {
        Customer customer = findCustomer(customerId);
        String customerNumber = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getCustomerIdNumber() : null;
        if (customerNumber != null && runLockRepo.existsByCustomerNumber(customerNumber)) {
            throw new BillingRunLockException(
                "Invoice processing in progress. Address changes cannot be made during this time.");
        }
        customerService.validateParentExists(request.getParentCustomerNumber());
        DeliveryMethod oldMethod = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getDeliveryMethod() : null;

        List<CustomerAuditLog> auditEntries = buildAuditEntries(customerId, customer, request);

        customer.setBillingProfile(request.toBillingProfile());
        customer.setParentCustomerNumber(request.getParentCustomerNumber());
        if (oldMethod == DeliveryMethod.E_INVOICE && request.getDeliveryMethod() != DeliveryMethod.E_INVOICE) {
            einvoiceAddressService.clearAddress(customerId);
        }
        Customer saved = customerRepo.save(customer);
        auditLogRepo.saveAll(auditEntries);
        eventPublisher.publishEvent(new BillingAddressChangedEvent(this, customerId));
        return BillingProfileResponse.from(saved);
    }

    private List<CustomerAuditLog> buildAuditEntries(Long customerId, Customer customer, BillingProfileRequest request) {
        String changedBy = auditorAware.getCurrentAuditor().orElse("system");
        Instant now = Instant.now();
        List<CustomerAuditLog> entries = new ArrayList<>();

        String oldMethod = customer.getBillingProfile() != null && customer.getBillingProfile().getDeliveryMethod() != null
            ? customer.getBillingProfile().getDeliveryMethod().name() : null;
        String newMethod = request.getDeliveryMethod() != null ? request.getDeliveryMethod().name() : null;
        if (!Objects.equals(oldMethod, newMethod)) {
            entries.add(CustomerAuditLog.builder()
                .customerId(customerId).field("deliveryMethod")
                .oldValue(oldMethod).newValue(newMethod)
                .changedBy(changedBy).changedAt(now).build());
        }

        String oldCustNum = customer.getBillingProfile() != null ? customer.getBillingProfile().getCustomerIdNumber() : null;
        if (!Objects.equals(oldCustNum, request.getCustomerIdNumber())) {
            entries.add(CustomerAuditLog.builder()
                .customerId(customerId).field("customerIdNumber")
                .oldValue(oldCustNum).newValue(request.getCustomerIdNumber())
                .changedBy(changedBy).changedAt(now).build());
        }

        return entries;
    }

    private Customer findCustomer(Long id) {
        return customerRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }
}
