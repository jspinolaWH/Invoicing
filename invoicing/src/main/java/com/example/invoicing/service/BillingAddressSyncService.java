package com.example.invoicing.service;
import com.example.invoicing.entity.customer.dto.BillingAddressSyncResult;
import com.example.invoicing.entity.customer.dto.BillingAddressSyncRequest;

import com.example.invoicing.entity.customer.BillingAddress;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.event.BillingAddressChangedEvent;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.ActiveRunLockRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingAddressSyncService {

    private final CustomerBillingProfileRepository customerRepository;
    private final ActiveRunLockRepository runLockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public BillingAddressSyncResult syncFromWasteHero(BillingAddressSyncRequest request) {
        Customer customer = customerRepository
            .findByBillingProfile_CustomerIdNumber(request.getCustomerNumber())
            .orElse(null);

        if (customer == null) {
            log.warn("Billing address sync: customer not found for customerNumber={}", request.getCustomerNumber());
            return BillingAddressSyncResult.builder()
                .customerNumber(request.getCustomerNumber())
                .status("NOT_FOUND")
                .message("No customer found for customerNumber: " + request.getCustomerNumber())
                .build();
        }

        if (runLockRepository.existsByCustomerNumber(request.getCustomerNumber())) {
            log.info("Billing address sync skipped for customerNumber={} — invoice run in progress",
                request.getCustomerNumber());
            return BillingAddressSyncResult.builder()
                .customerId(customer.getId())
                .customerNumber(request.getCustomerNumber())
                .status("SKIPPED_LOCKED")
                .message("Sync skipped: invoice run in progress for this customer")
                .build();
        }

        BillingAddress updated = new BillingAddress(
            request.getStreetAddress(),
            request.getPostalCode(),
            request.getCity(),
            request.getCountryCode(),
            request.getStreetAddressAlt(),
            request.getCityAlt(),
            request.getCountryCodeAlt(),
            request.getEmailAddress(),
            request.getEInvoicingAddress()
        );
        customer.getBillingProfile().setBillingAddress(updated);
        customerRepository.save(customer);
        eventPublisher.publishEvent(new BillingAddressChangedEvent(this, customer.getId()));

        log.info("Billing address synced from WasteHero for customerId={}, customerNumber={}",
            customer.getId(), request.getCustomerNumber());

        return BillingAddressSyncResult.builder()
            .customerId(customer.getId())
            .customerNumber(request.getCustomerNumber())
            .status("UPDATED")
            .message("Billing address updated successfully")
            .build();
    }

    @EventListener
    @Transactional
    public void onBillingAddressChanged(BillingAddressChangedEvent event) {
        log.info("Billing address changed for customerId={} — clearing cached FINVOICE XML on open invoices",
            event.getCustomerId());
        List<Invoice> openInvoices = invoiceRepository.findOpenByCustomerId(event.getCustomerId());
        for (Invoice invoice : openInvoices) {
            invoice.setFinvoiceXml(null);
        }
        if (!openInvoices.isEmpty()) {
            invoiceRepository.saveAll(openInvoices);
            log.info("Cleared finvoiceXml on {} open invoice(s) for customerId={}", openInvoices.size(), event.getCustomerId());
        }
    }
}
