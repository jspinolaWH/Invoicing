package com.example.invoicing.service;

import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EInvoiceMatchingService {

    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    public record MatchResult(Customer customer, String matchedBy) {}

    public Optional<MatchResult> match(String customerNumber, String invoiceNumber, String invoiceReference) {
        if (customerNumber != null && !customerNumber.isBlank()) {
            Optional<Customer> c = customerRepository.findByBillingProfile_CustomerIdNumber(customerNumber);
            if (c.isPresent()) {
                return Optional.of(new MatchResult(c.get(), "CUSTOMER_NUMBER"));
            }
            log.debug("No match by customerNumber={}", customerNumber);
        }

        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            Optional<Customer> c = invoiceRepository.findByInvoiceNumber(invoiceNumber).map(i -> i.getCustomer());
            if (c.isPresent()) {
                return Optional.of(new MatchResult(c.get(), "INVOICE_NUMBER"));
            }
            log.debug("No match by invoiceNumber={}", invoiceNumber);
        }

        if (invoiceReference != null && !invoiceReference.isBlank()) {
            Optional<Customer> c = invoiceRepository.findByInvoiceNumber(invoiceReference).map(i -> i.getCustomer());
            if (c.isPresent()) {
                return Optional.of(new MatchResult(c.get(), "INVOICE_REFERENCE"));
            }
            log.debug("No match by invoiceReference={}", invoiceReference);
        }

        return Optional.empty();
    }
}
