package com.example.invoicing.service;
import com.example.invoicing.entity.invoice.dto.InvoicePreviewEntry;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import com.example.invoicing.entity.invoice.dto.SimulationReport;
import com.example.invoicing.entity.invoice.dto.InvoiceGenerationResult;
import com.example.invoicing.entity.invoice.InvoiceLanguage;
import com.example.invoicing.entity.invoice.InvoiceType;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.vat.VatCalculationResult;
import com.example.invoicing.entity.surcharge.SurchargeConfig;

import com.example.invoicing.service.InvoiceBundlingService;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.customer.DeliveryMethod;
import com.example.invoicing.entity.customer.InvoicingMode;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.entity.validation.ValidationSeverity;
import com.example.invoicing.entity.invoice.*;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.ContractRepository;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.repository.PropertyRepository;
import com.example.invoicing.service.*;
import com.example.invoicing.entity.invoice.AccountingLedgerEntry;
import com.example.invoicing.entity.reportingaudit.ReportingDataAuditLog;
import com.example.invoicing.repository.ReportingDataAuditLogRepository;
import com.example.invoicing.service.AccountingAllocationService;
import com.example.invoicing.service.BillingSurchargeService;
import com.example.invoicing.service.SharedServiceInvoicingService;
import com.example.invoicing.service.MinimumFeeService;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.repository.CompanyInvoicingDefaultsRepository;
import com.example.invoicing.repository.MinimumFeeConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final InvoiceValidationEngine invoiceValidationEngine;
    private final InvoiceBundlingService invoiceBundlingService;
    private final LegalClassificationService legalClassificationService;
    private final SharedServiceInvoicingService sharedServiceInvoicingService;
    private final InvoiceNumberSeriesRepository invoiceNumberSeriesRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingEventRepository billingEventRepository;
    private final FinvoiceBuilderService finvoiceBuilderService;
    private final com.example.invoicing.repository.InvoiceTemplateRepository invoiceTemplateRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;
    private final VatCalculationService vatCalculationService;
    private final BillingSurchargeService billingSurchargeService;
    private final AccountingAllocationService accountingAllocationService;
    private final ReportingDataAuditLogRepository reportingAuditLogRepository;
    private final MinimumFeeService minimumFeeService;
    private final MinimumFeeConfigRepository minimumFeeConfigRepository;
    private final CompanyInvoicingDefaultsRepository companyInvoicingDefaultsRepository;

    /**
     * Generate an invoice for one customer's event group.
     *
     * @param events         The BillingEvents to invoice (already filtered/grouped by the caller)
     * @param customerId     The target customer
     * @param simulationMode When true: skip number assignment, do not persist
     */
    public InvoiceGenerationResult generate(List<BillingEvent> events, Long customerId, boolean simulationMode) {
        return generate(events, customerId, simulationMode, null, null);
    }

    public InvoiceGenerationResult generate(List<BillingEvent> events, Long customerId, boolean simulationMode, Long seriesId) {
        return generate(events, customerId, simulationMode, seriesId, null);
    }

    public InvoiceGenerationResult generate(List<BillingEvent> events, Long customerId, boolean simulationMode, Long seriesId, Long templateId) {

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

        // STEP 3 — Apply legal classification per line and carry wasteType for FINVOICE
        for (InvoiceLineItem line : lineItems) {
            BillingEvent src = line.getSourceEvent();
            if (line.getLegalClassification() == null) {
                LegalClassification cls = src != null
                    ? legalClassificationService.classify(src)
                    : LegalClassification.PRIVATE_LAW;
                line.setLegalClassification(cls);
            }
            if (line.getWasteType() == null && src != null) {
                line.setWasteType(src.getWasteType());
            }
        }

        // STEP 3b — Populate ledger entries from allocation rules
        for (InvoiceLineItem line : lineItems) {
            BillingEvent src = line.getSourceEvent();
            if (src != null && line.getLedgerEntries().isEmpty()) {
                try {
                    List<com.example.invoicing.entity.account.LedgerEntry> entries =
                        accountingAllocationService.resolveEntries(src);
                    for (com.example.invoicing.entity.account.LedgerEntry e : entries) {
                        line.getLedgerEntries().add(new AccountingLedgerEntry(
                            e.getAccountingAccount() != null ? e.getAccountingAccount().getCode() : null,
                            e.getDescription(),
                            e.getAmountNet(),
                            e.getAmountVat()));
                    }
                } catch (Exception ignored) {
                    // allocation rule may not exist for all events
                }
            }
        }

        // STEP 7 — Shared service distribution
        for (BillingEvent sharedEvent : sharedServiceEvents) {
            List<InvoiceLineItem> distributedLines = sharedServiceInvoicingService.distributeEvent(sharedEvent);
            // Apply classification and wasteType to distributed lines
            for (InvoiceLineItem dl : distributedLines) {
                if (dl.getLegalClassification() == null) {
                    dl.setLegalClassification(legalClassificationService.classify(sharedEvent));
                }
                if (dl.getWasteType() == null) {
                    dl.setWasteType(sharedEvent.getWasteType());
                }
            }
            lineItems.addAll(distributedLines);
        }

        // STEP 8 — Assign invoice number (skip in simulation mode)
        String invoiceNumber = null;
        if (!simulationMode) {
            invoiceNumber = assignNextNumber(seriesId);
        }

        // Apply correct VAT rates via VatCalculationService (Gap 2: reverse charge lines get 0% effective rate)
        boolean hasReverseCharge = false;
        for (InvoiceLineItem line : lineItems) {
            BillingEvent src = line.getSourceEvent();
            if (src == null) continue;
            try {
                VatCalculationResult vatResult = vatCalculationService.calculate(src);
                if (vatResult.isReverseCharge()) {
                    line.setVatRate(java.math.BigDecimal.ZERO.setScale(2));
                    line.setGrossAmount(line.getNetAmount());
                    hasReverseCharge = true;
                }
            } catch (Exception ignored) {
                // keep original rate if calculation fails (e.g. missing VAT rate for date)
            }
        }

        // STEP 9 — Apply billing surcharge if configured for this customer's delivery method / type
        DeliveryMethod deliveryMethod = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getDeliveryMethod() : null;
        if (deliveryMethod != null) {
            CustomerType customerType = customer.getCustomerType();
            billingSurchargeService.resolveConfig(deliveryMethod, customerType).ifPresent(sc -> {
                boolean isFirstInvoice = sc.isExemptFirstInvoice()
                    && !invoiceRepository.existsByCustomer_Id(customerId);
                boolean tariffExcluded = sc.isRequiresTariffInclusion()
                    && (events.isEmpty() || contractRepository
                        .findByCustomerNumberAndActiveTrue(events.get(0).getCustomerNumber())
                        .isEmpty());
                if (!isFirstInvoice && !tariffExcluded) {
                    lineItems.add(buildSurchargeLineItem(sc));
                }
            });
        }

        // STEP 9b — Apply minimum fee if configured for this customer type and period
        if (!events.isEmpty()) {
            LocalDate periodStart = events.stream()
                .map(BillingEvent::getEventDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
            LocalDate periodEnd = events.stream()
                .map(BillingEvent::getEventDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
            if (periodStart != null && periodEnd != null && customer.getCustomerType() != null) {
                long months = java.time.temporal.ChronoUnit.MONTHS.between(periodStart, periodEnd.plusDays(1));
                PeriodType periodType = months >= 9 ? PeriodType.ANNUAL : PeriodType.QUARTERLY;
                String customerTypeStr = customer.getCustomerType().name();
                BigDecimal netTotal = lineItems.stream()
                    .map(InvoiceLineItem::getNetAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                String lookupNumber = customer.getBillingProfile() != null
                    ? customer.getBillingProfile().getCustomerIdNumber() : String.valueOf(customerId);
                com.example.invoicing.entity.contract.Contract activeContract =
                    contractRepository.findByCustomerNumberAndActiveTrue(lookupNumber)
                        .stream().findFirst().orElse(null);
                LocalDate contractStart = activeContract != null ? activeContract.getStartDate() : null;
                LocalDate contractEnd = activeContract != null ? activeContract.getEndDate() : null;
                minimumFeeService.applyMinimumFee(
                    netTotal, customerTypeStr, periodType, periodStart, periodEnd, contractStart, contractEnd
                ).ifPresent(adjustmentAmount -> {
                    minimumFeeConfigRepository
                        .findByCustomerTypeAndPeriodTypeAndActiveTrue(customerTypeStr, periodType)
                        .ifPresent(config -> lineItems.add(buildMinimumFeeLineItem(adjustmentAmount, config)));
                });
            }
        }

        // STEP 10 — Basic validation: ensure all lines have required fields
        ValidationReport validationReport = runBasicValidation(lineItems, customer);
        if (validationReport.hasBlockingFailures()) {
            return InvoiceGenerationResult.validationError(customerId, validationReport);
        }

        // STEP 11 — Build invoice and attach line items
        String resolvedTemplateCode = resolveTemplateCode(templateId, events, customer);
        Invoice invoice = buildInvoice(lineItems, customer, invoiceNumber, simulationMode, resolvedTemplateCode);
        invoice.setReverseChargeVat(hasReverseCharge);
        for (InvoiceLineItem li : lineItems) {
            li.setInvoice(invoice);
        }
        invoice.getLineItems().addAll(lineItems);

        // STEP 11b — Run configurable validation rules
        ValidationReport engineReport = invoiceValidationEngine.validate(invoice, 1L);
        if (engineReport.hasBlockingFailures()) {
            return InvoiceGenerationResult.validationError(customerId, engineReport);
        }

        if (!simulationMode) {
            Invoice saved = invoiceRepository.save(invoice);
            try {
                String xml = finvoiceBuilderService.build(saved);
                saved.setFinvoiceXml(xml);
                invoiceRepository.save(saved);
            } catch (Exception e) {
                // XML generation failure does not block invoice creation
            }
            writeReportingAuditLogs(saved);
            return InvoiceGenerationResult.success(saved);
        } else {
            return InvoiceGenerationResult.simulation(invoice, engineReport);
        }
    }

    private String resolveTemplateCode(Long templateId, List<BillingEvent> events, Customer customer) {
        // run-level
        if (templateId != null) {
            return invoiceTemplateRepository.findById(templateId)
                .map(t -> t.getCode())
                .orElse("WASTE_STANDARD");
        }
        // contract-level: find first active contract for the customer with a template set
        if (!events.isEmpty()) {
            String customerNumber = events.get(0).getCustomerNumber();
            Long contractTemplateId = contractRepository
                .findByCustomerNumberAndActiveTrue(customerNumber)
                .stream()
                .map(c -> c.getInvoiceTemplateId())
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
            if (contractTemplateId != null) {
                return invoiceTemplateRepository.findById(contractTemplateId)
                    .map(t -> t.getCode())
                    .orElse("WASTE_STANDARD");
            }
        }
        // property-level: find first event with a locationId that maps to a property with a template set
        for (BillingEvent event : events) {
            if (event.getLocationId() == null) continue;
            Long propertyTemplateId = propertyRepository
                .findByPropertyId(event.getLocationId())
                .map(p -> p.getInvoiceTemplateId())
                .orElse(null);
            if (propertyTemplateId != null) {
                return invoiceTemplateRepository.findById(propertyTemplateId)
                    .map(t -> t.getCode())
                    .orElse("WASTE_STANDARD");
            }
        }
        // customer-level
        Long customerTemplateId = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getInvoiceTemplateId() : null;
        if (customerTemplateId != null) {
            return invoiceTemplateRepository.findById(customerTemplateId)
                .map(t -> t.getCode())
                .orElse("WASTE_STANDARD");
        }
        return "WASTE_STANDARD";
    }

    private Invoice buildInvoice(List<InvoiceLineItem> lineItems, Customer customer,
                                  String invoiceNumber, boolean simulationMode, String templateCode) {
        BigDecimal netAmount = lineItems.stream()
            .map(InvoiceLineItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal grossAmount = lineItems.stream()
            .map(InvoiceLineItem::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal vatAmount = grossAmount.subtract(netAmount).setScale(4, RoundingMode.HALF_UP);

        InvoicingMode companyDefault = companyInvoicingDefaultsRepository.findById(1L)
            .map(d -> d.getDefaultInvoicingMode())
            .orElse(InvoicingMode.NET);
        InvoicingMode invoicingMode = customer.getBillingProfile() != null
            && customer.getBillingProfile().getInvoicingMode() != null
            ? customer.getBillingProfile().getInvoicingMode()
            : companyDefault;

        String langCode = customer.getBillingProfile() != null
            ? customer.getBillingProfile().getLanguageCode() : null;
        InvoiceLanguage language = resolveLanguage(langCode);

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTemplateCode(templateCode);
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

    private String assignNextNumber(Long seriesId) {
        var seriesOpt = seriesId != null
            ? invoiceNumberSeriesRepository.findById(seriesId)
            : invoiceNumberSeriesRepository.findAll().stream().findFirst();
        return seriesOpt
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

    private InvoiceLineItem buildSurchargeLineItem(SurchargeConfig sc) {
        BigDecimal amount = sc.getAmount().setScale(4, RoundingMode.HALF_UP);
        String desc = "Invoicing surcharge" + (sc.getDescription() != null && !sc.getDescription().isBlank()
            ? " - " + sc.getDescription() : "");
        InvoiceLineItem line = new InvoiceLineItem();
        line.setDescription(desc);
        line.setQuantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        line.setUnitPrice(amount);
        line.setVatRate(BigDecimal.ZERO.setScale(2));
        line.setNetAmount(amount);
        line.setGrossAmount(amount);
        line.setLegalClassification(LegalClassification.PRIVATE_LAW);
        line.setBundled(false);
        return line;
    }

    private InvoiceLineItem buildMinimumFeeLineItem(BigDecimal adjustmentAmount, MinimumFeeConfig config) {
        BigDecimal amount = adjustmentAmount.setScale(4, RoundingMode.HALF_UP);
        String desc = "Minimum fee adjustment" + (config.getAdjustmentProductCode() != null
            ? " - " + config.getAdjustmentProductCode() : "");
        InvoiceLineItem line = new InvoiceLineItem();
        line.setDescription(desc);
        line.setQuantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        line.setUnitPrice(amount);
        line.setVatRate(BigDecimal.ZERO.setScale(2));
        line.setNetAmount(amount);
        line.setGrossAmount(amount);
        line.setLegalClassification(LegalClassification.PRIVATE_LAW);
        line.setBundled(false);
        return line;
    }

    private void writeReportingAuditLogs(Invoice invoice) {
        try {
            Instant now = Instant.now();
            for (InvoiceLineItem li : invoice.getLineItems()) {
                if (li.getLegalClassification() != null) {
                    reportingAuditLogRepository.save(ReportingDataAuditLog.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .lineItemId(li.getId())
                        .field("legalClassification")
                        .assignedValue(li.getLegalClassification().name())
                        .loggedAt(now)
                        .loggedBy("system")
                        .build());
                }
                if (!li.getLedgerEntries().isEmpty()) {
                    String codes = li.getLedgerEntries().stream()
                        .map(e -> e.getLedgerCode() != null ? e.getLedgerCode() : "?")
                        .collect(Collectors.joining(","));
                    reportingAuditLogRepository.save(ReportingDataAuditLog.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .lineItemId(li.getId())
                        .field("ledgerEntries")
                        .assignedValue(codes)
                        .loggedAt(now)
                        .loggedBy("system")
                        .build());
                }
                if (li.getAccountingAccount() != null) {
                    reportingAuditLogRepository.save(ReportingDataAuditLog.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .lineItemId(li.getId())
                        .field("accountingAccount")
                        .assignedValue(li.getAccountingAccount().getCode())
                        .loggedAt(now)
                        .loggedBy("system")
                        .build());
                }
            }
        } catch (Exception ignored) {
            // audit log failure must not block invoice creation
        }
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
