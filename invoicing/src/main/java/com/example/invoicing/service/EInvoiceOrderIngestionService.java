package com.example.invoicing.service;

import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.CustomerAuditLog;
import com.example.invoicing.entity.customer.DeliveryMethod;
import com.example.invoicing.entity.customer.DirectDebitMandate;
import com.example.invoicing.entity.customer.dto.*;
import com.example.invoicing.repository.CustomerAuditLogRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.DirectDebitMandateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EInvoiceOrderIngestionService {

    private final EInvoiceMatchingService matchingService;
    private final EInvoiceAddressService einvoiceAddressService;
    private final CustomerBillingProfileRepository customerRepo;
    private final CustomerAuditLogRepository auditLogRepo;
    private final DirectDebitMandateRepository mandateRepo;

    @Transactional
    public EInvoiceOrderIngestionResult ingestEInvoiceOrder(EInvoiceOrderRequest request) {
        Optional<EInvoiceMatchingService.MatchResult> match =
            matchingService.match(request.getCustomerNumber(), request.getInvoiceNumber(), request.getInvoiceReference());

        if (match.isEmpty()) {
            log.warn("EInvoiceOrder: no customer match for customerNumber={}, invoiceNumber={}, invoiceReference={}",
                request.getCustomerNumber(), request.getInvoiceNumber(), request.getInvoiceReference());
            return EInvoiceOrderIngestionResult.builder()
                .status("UNMATCHED")
                .customerNumber(request.getCustomerNumber())
                .message("No customer found for provided identifiers")
                .build();
        }

        Customer customer = match.get().customer();
        String matchedBy = match.get().matchedBy();
        boolean isTerminate = request.getOrderType() == EInvoiceOrderType.TERMINATE;

        if (isTerminate) {
            String oldAddress = einvoiceAddressService.getAddress(customer.getId()).getAddress();
            einvoiceAddressService.clearAddress(customer.getId());
            auditAddress(customer.getId(), oldAddress, null);
            changeDeliveryMethod(customer, DeliveryMethod.PAPER);
            log.info("EInvoiceOrder TERMINATE for customerId={} (matchedBy={})", customer.getId(), matchedBy);
            return EInvoiceOrderIngestionResult.builder()
                .status("MATCHED")
                .customerId(customer.getId())
                .customerNumber(customer.getBillingProfile() != null ? customer.getBillingProfile().getCustomerIdNumber() : null)
                .matchedBy(matchedBy)
                .message("E-invoice terminated: billing channel set to PAPER and address removed")
                .build();
        }

        // START (default when orderType is null or START)
        String oldAddress = einvoiceAddressService.getAddress(customer.getId()).getAddress();
        einvoiceAddressService.updateFromOperator(customer.getId(), request.getEinvoiceAddress(), request.getOperatorCode());
        auditAddress(customer.getId(), oldAddress, request.getEinvoiceAddress());
        changeDeliveryMethod(customer, DeliveryMethod.E_INVOICE);
        log.info("EInvoiceOrder START for customerId={} (matchedBy={})", customer.getId(), matchedBy);
        return EInvoiceOrderIngestionResult.builder()
            .status("MATCHED")
            .customerId(customer.getId())
            .customerNumber(customer.getBillingProfile() != null ? customer.getBillingProfile().getCustomerIdNumber() : null)
            .matchedBy(matchedBy)
            .message("E-invoice activated: address updated and billing channel set to E_INVOICE")
            .build();
    }

    @Transactional
    public EInvoiceOrderIngestionResult ingestDirectDebitOrder(DirectDebitOrderRequest request) {
        Optional<EInvoiceMatchingService.MatchResult> match =
            matchingService.match(request.getCustomerNumber(), request.getInvoiceNumber(), request.getInvoiceReference());

        if (match.isEmpty()) {
            log.warn("DirectDebitOrder: no customer match for customerNumber={}, invoiceNumber={}, invoiceReference={}",
                request.getCustomerNumber(), request.getInvoiceNumber(), request.getInvoiceReference());
            return EInvoiceOrderIngestionResult.builder()
                .status("UNMATCHED")
                .customerNumber(request.getCustomerNumber())
                .message("No customer found for provided identifiers")
                .build();
        }

        Customer customer = match.get().customer();
        String matchedBy = match.get().matchedBy();
        boolean isTerminate = request.getOrderType() == EInvoiceOrderType.TERMINATE;

        if (isTerminate) {
            mandateRepo.findByCustomer_Id(customer.getId()).ifPresent(m -> {
                m.setTerminatedAt(Instant.now());
                mandateRepo.save(m);
                auditLogRepo.save(CustomerAuditLog.builder()
                    .customerId(customer.getId())
                    .field("directDebitMandate")
                    .oldValue(m.getMandateReference())
                    .newValue(null)
                    .changedBy("integration")
                    .changedAt(Instant.now())
                    .build());
            });
            changeDeliveryMethod(customer, DeliveryMethod.PAPER);
            log.info("DirectDebitOrder TERMINATE for customerId={} (matchedBy={})", customer.getId(), matchedBy);
            return EInvoiceOrderIngestionResult.builder()
                .status("MATCHED")
                .customerId(customer.getId())
                .customerNumber(customer.getBillingProfile() != null ? customer.getBillingProfile().getCustomerIdNumber() : null)
                .matchedBy(matchedBy)
                .message("Direct debit terminated: mandate closed and billing channel set to PAPER")
                .build();
        }

        DirectDebitMandate mandate = mandateRepo.findByCustomer_Id(customer.getId())
            .orElse(new DirectDebitMandate());
        mandate.setCustomer(customer);
        mandate.setMandateReference(request.getMandate());
        mandate.setBankAccount(request.getBankAccount());
        mandate.setActivatedAt(Instant.now());
        mandate.setTerminatedAt(null);
        mandateRepo.save(mandate);

        changeDeliveryMethod(customer, DeliveryMethod.DIRECT_PAYMENT);
        auditLogRepo.save(CustomerAuditLog.builder()
            .customerId(customer.getId())
            .field("directDebitMandate")
            .oldValue(null)
            .newValue(request.getMandate())
            .changedBy("integration")
            .changedAt(Instant.now())
            .build());

        log.info("DirectDebitOrder START for customerId={} mandate={} (matchedBy={})",
            customer.getId(), request.getMandate(), matchedBy);
        return EInvoiceOrderIngestionResult.builder()
            .status("MATCHED")
            .customerId(customer.getId())
            .customerNumber(customer.getBillingProfile() != null ? customer.getBillingProfile().getCustomerIdNumber() : null)
            .matchedBy(matchedBy)
            .message("Direct debit activated: mandate persisted and billing channel set to DIRECT_PAYMENT")
            .build();
    }

    private void changeDeliveryMethod(Customer customer, DeliveryMethod newMethod) {
        if (customer.getBillingProfile() == null) {
            return;
        }
        DeliveryMethod oldMethod = customer.getBillingProfile().getDeliveryMethod();
        if (oldMethod == newMethod) {
            return;
        }
        customer.getBillingProfile().setDeliveryMethod(newMethod);
        customerRepo.save(customer);
        auditLogRepo.save(CustomerAuditLog.builder()
            .customerId(customer.getId())
            .field("deliveryMethod")
            .oldValue(oldMethod != null ? oldMethod.name() : null)
            .newValue(newMethod.name())
            .changedBy("integration")
            .changedAt(Instant.now())
            .build());
    }

    private void auditAddress(Long customerId, String oldAddress, String newAddress) {
        if (Objects.equals(oldAddress, newAddress)) {
            return;
        }
        auditLogRepo.save(CustomerAuditLog.builder()
            .customerId(customerId)
            .field("einvoiceAddress")
            .oldValue(oldAddress)
            .newValue(newAddress)
            .changedBy("integration")
            .changedAt(Instant.now())
            .build());
    }
}
