package com.example.invoicing.generation;

import com.example.invoicing.bundling.InvoiceBundlingService;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.InvoicingMode;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.entity.validation.ValidationSeverity;
import com.example.invoicing.invoice.*;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.service.*;
import com.example.invoicing.sharedservice.SharedServiceInvoicingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceGenerationService {

    private final CustomerBillingProfileRepository customerRepository;
    private final BillingProfileValidationService billingProfileValidationService;
    private final InvoiceBundlingService invoiceBundlingService;
    private final LegalClassificationService legalClassificationService;
    private final SharedServiceInvoicingService sharedServiceInvoicingService;
    private final InvoiceNumberSeriesRepository invoiceNumberSeriesRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingEventRepository billingEventRepository;

    /**
     * Generate an invoice for one customer's event group.
     *
     * @param events         The BillingEvents to invoice (already filtered/grouped by the caller)
     * @param customerId     The target customer
     * @param simulationMode When true: skip number assignment, do not persist
     */
    public InvoiceGenerationResult generate(List<BillingEvent> events, Long customerId, boolean simulationMode) {

        // STEP 1 — Validate billing profile completeness
        ValidationReport profileCheck = billingProfileValidationService.validate(customerId, 1L);
        if (profileCheck.hasBlockingFailures()) {
            List<String> issues = profileCheck.blockingFailures().stream()
                .map(f -> f.getField() + ": " + f.getDescription())
                .collect(Collectors.toList());
            return InvoiceGenerationResult.profileError(customerId, issues);
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        String customerNumber = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getCustomerIdNumber() : String.valueOf(customerId);

        // STEP 2 — Group events by bundling rules (regular events only)
        List<BillingEvent> sharedServiceEvents = events.stream()
            .filter(e -> e.getSharedServiceGroupId() != null)
            .collect(Collectors.toList());

        List<BillingEvent> regularEvents = events.stream()
            .filter(e -> e.getSharedServiceGroupId() == null)
            .collect(Collectors.toList());

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        if (!regularEvents.isEmpty()) {
            lineItems.addAll(invoiceBundlingService.bundle(regularEvents, customerNumber));
        }

        // STEP 3 — Apply legal classification per line
        for (InvoiceLineItem line : lineItems) {
            if (line.getLegalClassification() == null) {
                BillingEvent src = line.getSourceEvent();
                LegalClassification cls = src != null
                    ? legalClassificationService.classify(src)
                    : LegalClassification.PRIVATE_LAW;
                line.setLegalClassification(cls);
            }
        }

        // STEP 7 — Shared service distribution
        for (BillingEvent sharedEvent : sharedServiceEvents) {
            List<InvoiceLineItem> distributedLines = sharedServiceInvoicingService.distributeEvent(sharedEvent);
            // Apply classification to distributed lines
            for (InvoiceLineItem dl : distributedLines) {
                if (dl.getLegalClassification() == null) {
                    dl.setLegalClassification(legalClassificationService.classify(sharedEvent));
                }
            }
            lineItems.addAll(distributedLines);
        }

        // STEP 8 — Assign invoice number (skip in simulation mode)
        String invoiceNumber = null;
        if (!simulationMode) {
            invoiceNumber = assignNextNumber(customerId);
        }

        // STEP 10 — Basic validation: ensure all lines have required fields
        ValidationReport validationReport = runBasicValidation(lineItems, customer);
        if (validationReport.hasBlockingFailures()) {
            return InvoiceGenerationResult.validationError(customerId, validationReport);
        }

        // STEP 11 — Build and (optionally) save invoice
        Invoice invoice = buildInvoice(lineItems, customer, invoiceNumber, simulationMode);

        if (!simulationMode) {
            for (InvoiceLineItem li : lineItems) {
                li.setInvoice(invoice);
            }
            invoice.getLineItems().addAll(lineItems);
            Invoice saved = invoiceRepository.save(invoice);
            return InvoiceGenerationResult.success(saved);
        } else {
            // Return in-memory preview — no persistence
            for (InvoiceLineItem li : lineItems) {
                li.setInvoice(invoice);
            }
            invoice.getLineItems().addAll(lineItems);
            return InvoiceGenerationResult.simulation(invoice, validationReport);
        }
    }

    private Invoice buildInvoice(List<InvoiceLineItem> lineItems, Customer customer,
                                  String invoiceNumber, boolean simulationMode) {
        BigDecimal netAmount = lineItems.stream()
            .map(InvoiceLineItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal grossAmount = lineItems.stream()
            .map(InvoiceLineItem::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal vatAmount = grossAmount.subtract(netAmount).setScale(4, RoundingMode.HALF_UP);

        InvoicingMode invoicingMode = customer.getBillingProfile() != null
            && customer.getBillingProfile().getInvoicingMode() != null
            ? customer.getBillingProfile().getInvoicingMode()
            : InvoicingMode.NET;

        String langCode = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getLanguageCode() : null;
        InvoiceLanguage language = resolveLanguage(langCode);

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTemplateCode("WASTE_STANDARD");
        invoice.setLanguage(language);
        invoice.setInvoicingMode(invoicingMode);
        invoice.setStatus(simulationMode ? InvoiceStatus.DRAFT : InvoiceStatus.DRAFT);
        invoice.setInvoiceType(InvoiceType.STANDARD);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(14));
        invoice.setNetAmount(netAmount);
        invoice.setGrossAmount(grossAmount);
        invoice.setVatAmount(vatAmount);
        return invoice;
    }

    private String assignNextNumber(Long customerId) {
        return invoiceNumberSeriesRepository.findAll().stream()
            .findFirst()
            .map(series -> {
                long next = series.getCurrentCounter() + 1;
                series.setCurrentCounter(next);
                invoiceNumberSeriesRepository.save(series);
                return series.getFormatPattern()
                    .replace("{PREFIX}", series.getPrefix())
                    .replace("{YEAR}", String.valueOf(LocalDate.now().getYear()))
                    .replace("{COUNTER:06d}", String.format("%06d", next));
            })
            .orElse("INV-" + System.currentTimeMillis());
    }

    private ValidationReport runBasicValidation(List<InvoiceLineItem> lineItems, Customer customer) {
        List<com.example.invoicing.entity.validation.ValidationFailure> failures = new ArrayList<>();
        for (InvoiceLineItem li : lineItems) {
            if (li.getLegalClassification() == null) {
                failures.add(buildFailure(customer.getId(), "lineItem.legalClassification",
                    "CLASSIFICATION_MISSING", "Legal classification is required on every line item"));
            }
            if (li.getNetAmount() == null || li.getNetAmount().compareTo(BigDecimal.ZERO) < 0) {
                failures.add(buildFailure(customer.getId(), "lineItem.netAmount",
                    "NEGATIVE_AMOUNT", "Net amount must be non-negative"));
            }
        }
        return ValidationReport.builder()
            .totalChecked(lineItems.size())
            .passed(failures.isEmpty() ? lineItems.size() : 0)
            .failures(failures)
            .build();
    }

    private com.example.invoicing.entity.validation.ValidationFailure buildFailure(
            Long entityId, String field, String rule, String description) {
        return com.example.invoicing.entity.validation.ValidationFailure.builder()
            .entityId(entityId)
            .entityType("INVOICE_LINE")
            .field(field)
            .rule(rule)
            .severity(ValidationSeverity.BLOCKING)
            .description(description)
            .build();
    }

    private InvoiceLanguage resolveLanguage(String langCode) {
        if (langCode == null) return InvoiceLanguage.FI;
        return switch (langCode.toLowerCase()) {
            case "sv" -> InvoiceLanguage.SV;
            case "en" -> InvoiceLanguage.EN;
            default -> InvoiceLanguage.FI;
        };
    }
}
