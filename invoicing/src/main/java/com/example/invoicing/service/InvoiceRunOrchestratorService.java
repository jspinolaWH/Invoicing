package com.example.invoicing.service;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;

import com.example.invoicing.entity.billingcycle.BillingCycle;
import com.example.invoicing.entity.billingcycle.BillingFrequency;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.CustomerType;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.invoice.InvoiceAttachment;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.entity.invoice.dto.InvoiceGenerationResult;
import com.example.invoicing.service.InvoiceGenerationService;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import com.example.invoicing.repository.BillingCycleRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.ContractRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceAttachmentRepository;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.MinimumFeeConfigRepository;
import com.example.invoicing.repository.ProductRepository;
import com.example.invoicing.entity.classification.LegalClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceRunOrchestratorService {

    private final InvoiceRunRepository runRepository;
    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceGenerationService invoiceGenerationService;
    private final InvoiceRunLockService lockService;
    private final InvoiceAttachmentRepository attachmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinvoiceBuilderService finvoiceBuilderService;
    private final ProductRepository productRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final BillingCycleService billingCycleService;
    private final BillingRestrictionService billingRestrictionService;
    private final MinimumFeeConfigRepository minimumFeeConfigRepository;
    private final MinimumFeeService minimumFeeService;
    private final ContractRepository contractRepository;

    @Async("invoiceRunExecutor")
    @Transactional
    public void executeRun(Long runId) {
        InvoiceRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus(InvoiceRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        List<String> lockedCustomers = new ArrayList<>();
        try {
            Long resolvedProductId = resolveProductId(run.getFilterServiceType());

            List<BillingEvent> events = billingEventRepository.findByRunFilter(
                run.getFilterMunicipality(),
                run.getFilterPeriodFrom(),
                run.getFilterPeriodTo(),
                resolvedProductId,
                resolveClassification(run.getFilterServiceResponsibility()),
                run.getFilterLocation()
            );

            BillingFrequency filterFreq = resolveFilterFrequency(run.getFilterBillingFrequency());
            List<BillingCycle> allDueCycles = filterFreq != null
                ? billingCycleRepository.findCyclesDueByFrequency(LocalDate.now(), filterFreq)
                : billingCycleRepository.findCyclesDueInRunWindow(LocalDate.now());
            Map<String, List<BillingCycle>> dueCyclesByCustomer = allDueCycles.stream()
                .collect(Collectors.groupingBy(BillingCycle::getCustomerNumber));

            Set<String> immediateServiceTypes = new HashSet<>(billingRestrictionService.getImmediateServiceTypes());

            Map<String, List<BillingEvent>> byCustomerNumber = events.stream()
                .collect(Collectors.groupingBy(BillingEvent::getCustomerNumber));

            lockedCustomers = new ArrayList<>(byCustomerNumber.keySet());
            lockService.lockCustomers(runId, lockedCustomers);
            run.setLockedCustomerCount(lockedCustomers.size());
            runRepository.save(run);

            int totalInvoices = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            int minimumFeeAdjustmentCount = 0;
            BigDecimal minimumFeeAdjustmentTotal = BigDecimal.ZERO;
            int minimumFeeExemptCount = 0;
            List<ValidationFailureEntry> allFailures = new ArrayList<>();

            for (Map.Entry<String, List<BillingEvent>> entry : byCustomerNumber.entrySet()) {
                String customerNumber = entry.getKey();
                List<BillingEvent> customerEvents = entry.getValue();

                Optional<Customer> customerOpt = customerRepository
                    .findByBillingProfile_CustomerIdNumber(customerNumber);
                if (customerOpt.isEmpty()) {
                    log.warn("Customer not found for customerNumber {}, skipping", customerNumber);
                    continue;
                }
                Customer customer = customerOpt.get();
                Long customerId = customer.getId();

                if (!matchesCustomerType(run.getFilterCustomerType(), customer)) {
                    continue;
                }

                List<BillingEvent> immediateEvents = immediateServiceTypes.isEmpty()
                    ? List.of()
                    : customerEvents.stream()
                        .filter(e -> e.getProduct() != null && immediateServiceTypes.contains(e.getProduct().getCode()))
                        .collect(Collectors.toList());
                List<BillingEvent> cycleEvents = customerEvents.stream()
                    .filter(e -> e.getProduct() == null || !immediateServiceTypes.contains(e.getProduct().getCode()))
                    .collect(Collectors.toList());

                // IMMEDIATE events: always generate, bypass billing frequency filter
                if (!immediateEvents.isEmpty()) {
                    InvoiceGenerationResult res = tryGenerate(immediateEvents, customerId, run, customer, allFailures);
                    if (res != null && res.isSuccess()) {
                        res.getInvoice().setBillingType("IMMEDIATE");
                        invoiceRepository.save(res.getInvoice());
                        BigDecimal gross = res.getInvoice().getGrossAmount();
                        totalInvoices++;
                        if (gross != null) totalAmount = totalAmount.add(gross);
                        propagateBatchAttachment(run, res.getInvoice());
                        MinFeeStats mfs = computeMinFeeStats(res.getInvoice(), customer, immediateEvents);
                        minimumFeeAdjustmentCount += mfs.adjCount;
                        minimumFeeAdjustmentTotal = minimumFeeAdjustmentTotal.add(mfs.adjTotal);
                        minimumFeeExemptCount += mfs.exemptCount;
                    }
                }

                // CYCLE_BASED events: auto-assign matching billing cycle by service type and period
                if (!cycleEvents.isEmpty()) {
                    List<BillingCycle> customerCycles = dueCyclesByCustomer.get(customerNumber);
                    if (filterFreq != null && (customerCycles == null || customerCycles.isEmpty())) {
                        // No due cycle at the required frequency for this customer — skip
                    } else {
                        List<BillingEvent> eligible = (customerCycles == null || customerCycles.isEmpty())
                            ? cycleEvents
                            : filterEventsByDueCycles(cycleEvents, customerCycles, run);
                        if (!eligible.isEmpty()) {
                            InvoiceGenerationResult res = tryGenerate(eligible, customerId, run, customer, allFailures);
                            if (res != null && res.isSuccess()) {
                                res.getInvoice().setBillingType("CYCLE_BASED");
                                invoiceRepository.save(res.getInvoice());
                                BigDecimal gross = res.getInvoice().getGrossAmount();
                                totalInvoices++;
                                if (gross != null) totalAmount = totalAmount.add(gross);
                                propagateBatchAttachment(run, res.getInvoice());
                                MinFeeStats mfs = computeMinFeeStats(res.getInvoice(), customer, eligible);
                                minimumFeeAdjustmentCount += mfs.adjCount;
                                minimumFeeAdjustmentTotal = minimumFeeAdjustmentTotal.add(mfs.adjTotal);
                                minimumFeeExemptCount += mfs.exemptCount;
                            }
                        }
                    }
                }
            }

            run.setTotalInvoices(totalInvoices);
            run.setTotalAmount(totalAmount);
            run.setReportTotalChecked(byCustomerNumber.size());
            run.setReportPassed(totalInvoices);
            run.setReportBlockingCount((int) allFailures.stream()
                .filter(f -> "BLOCKING".equals(f.getSeverity())).count());
            run.setReportWarningCount((int) allFailures.stream()
                .filter(f -> "WARNING".equals(f.getSeverity())).count());
            run.setMinimumFeeAdjustmentCount(minimumFeeAdjustmentCount);
            run.setMinimumFeeAdjustmentTotal(minimumFeeAdjustmentTotal);
            run.setMinimumFeeExemptCount(minimumFeeExemptCount);
            run.setStatus(allFailures.stream().anyMatch(f -> "BLOCKING".equals(f.getSeverity()))
                ? InvoiceRunStatus.COMPLETED_WITH_ERRORS
                : InvoiceRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());

        } catch (Exception ex) {
            run.setStatus(InvoiceRunStatus.ERROR);
            run.setCompletedAt(Instant.now());
            log.error("Invoice run {} failed with exception", runId, ex);
        } finally {
            lockService.releaseLocksForRun(runId);
            run.setLockedCustomerCount(0);
            runRepository.save(run);
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void triggerImmediateInvoicing() {
        List<String> immediateServiceTypes = billingRestrictionService.getImmediateServiceTypes();
        if (immediateServiceTypes.isEmpty()) return;

        List<BillingEvent> events = billingEventRepository.findUninvoicedByProductCodes(immediateServiceTypes);
        if (events.isEmpty()) return;

        Map<String, List<BillingEvent>> byCustomer = events.stream()
            .collect(Collectors.groupingBy(BillingEvent::getCustomerNumber));

        for (Map.Entry<String, List<BillingEvent>> entry : byCustomer.entrySet()) {
            Optional<Customer> customerOpt = customerRepository
                .findByBillingProfile_CustomerIdNumber(entry.getKey());
            if (customerOpt.isEmpty()) {
                log.warn("Immediate invoicing: customer not found for {}", entry.getKey());
                continue;
            }
            Long customerId = customerOpt.get().getId();
            try {
                InvoiceGenerationResult result = invoiceGenerationService.generate(
                    entry.getValue(), customerId, false, null, null);
                if (result.isSuccess()) {
                    result.getInvoice().setBillingType("IMMEDIATE");
                    invoiceRepository.save(result.getInvoice());
                    log.info("Auto-generated IMMEDIATE invoice {} for customer {}",
                        result.getInvoice().getId(), customerId);
                }
            } catch (Exception ex) {
                log.error("Failed auto-immediate invoice for customer {}", customerId, ex);
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void triggerScheduledSends() {
        List<InvoiceRun> due = runRepository.findScheduledForSend(Instant.now());
        for (InvoiceRun run : due) {
            run.setStatus(InvoiceRunStatus.SENDING);
            run.setSentAt(Instant.now());
            run.setStatus(InvoiceRunStatus.SENT);
            runRepository.save(run);
            log.info("Triggered scheduled send for run {}", run.getId());
        }
    }

    private record MinFeeStats(int adjCount, BigDecimal adjTotal, int exemptCount) {}

    private MinFeeStats computeMinFeeStats(Invoice invoice, Customer customer, List<BillingEvent> events) {
        int adjCount = 0;
        BigDecimal adjTotal = BigDecimal.ZERO;
        int exemptCount = 0;

        for (var li : invoice.getLineItems()) {
            if (li.getDescription() != null && li.getDescription().startsWith("Minimum fee adjustment")) {
                adjCount++;
                if (li.getNetAmount() != null) {
                    adjTotal = adjTotal.add(li.getNetAmount());
                }
            }
        }

        if (adjCount == 0 && customer.getCustomerType() != null) {
            LocalDate periodStart = events.stream()
                .map(BillingEvent::getEventDate).filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
            LocalDate periodEnd = events.stream()
                .map(BillingEvent::getEventDate).filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
            if (periodStart != null && periodEnd != null) {
                long months = java.time.temporal.ChronoUnit.MONTHS.between(periodStart, periodEnd.plusDays(1));
                PeriodType periodType = months >= 9 ? PeriodType.ANNUAL : PeriodType.QUARTERLY;
                String custTypeStr = customer.getCustomerType().name();
                String lookupNumber = customer.getBillingProfile() != null
                    ? customer.getBillingProfile().getCustomerIdNumber() : String.valueOf(customer.getId());
                com.example.invoicing.entity.contract.Contract contract =
                    contractRepository.findByCustomerNumberAndActiveTrue(lookupNumber)
                        .stream().findFirst().orElse(null);
                LocalDate contractStart = contract != null ? contract.getStartDate() : null;
                LocalDate contractEnd = contract != null ? contract.getEndDate() : null;
                Optional<MinimumFeeConfig> configOpt =
                    minimumFeeConfigRepository.findByCustomerTypeAndPeriodTypeAndActiveTrue(custTypeStr, periodType);
                if (configOpt.isPresent() && !minimumFeeService.isMinimumApplicable(
                        configOpt.get(), periodStart, periodEnd, contractStart, contractEnd)) {
                    exemptCount = 1;
                }
            }
        }

        return new MinFeeStats(adjCount, adjTotal, exemptCount);
    }

    private void propagateBatchAttachment(InvoiceRun run, Invoice invoice) {
        if (run.getBatchAttachmentIdentifier() == null || run.getBatchAttachmentIdentifier().isBlank()) {
            return;
        }
        InvoiceAttachment att = new InvoiceAttachment();
        att.setInvoice(invoice);
        att.setAttachmentIdentifier(run.getBatchAttachmentIdentifier());
        att.setFilename(run.getBatchAttachmentFilename() != null ? run.getBatchAttachmentFilename() : "batch-attachment.pdf");
        att.setMimeType(run.getBatchAttachmentMimeType() != null ? run.getBatchAttachmentMimeType() : "application/pdf");
        att.setSecurityClass(run.getBatchAttachmentSecurityClass());
        att.setSizeBytes(0L);
        attachmentRepository.save(att);
        try {
            invoice.getAttachments().add(att);
            String xml = finvoiceBuilderService.build(invoice);
            invoice.setFinvoiceXml(xml);
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.warn("Failed to rebuild FINVOICE XML with batch attachment for invoice {}", invoice.getId(), e);
        }
        log.info("AUDIT batch_attachment_linked runId={} invoiceId={} attachmentIdentifier={} at={}",
            run.getId(), invoice.getId(), run.getBatchAttachmentIdentifier(), Instant.now());
    }

    private LegalClassification resolveClassification(String serviceResponsibility) {
        if (serviceResponsibility == null) return null;
        try { return LegalClassification.valueOf(serviceResponsibility); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Long resolveProductId(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) return null;
        return productRepository.findByCode(serviceType).map(p -> p.getId()).orElse(null);
    }

    private boolean matchesCustomerType(String filterCustomerType, Customer customer) {
        if (filterCustomerType == null || filterCustomerType.isBlank()) return true;
        try {
            CustomerType expected = CustomerType.valueOf(filterCustomerType);
            return customer.getCustomerType() == expected;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private InvoiceGenerationResult tryGenerate(List<BillingEvent> eventsToProcess, Long customerId,
                                                InvoiceRun run, Customer customer,
                                                List<ValidationFailureEntry> allFailures) {
        try {
            InvoiceGenerationResult result = invoiceGenerationService.generate(
                eventsToProcess, customerId, false, run.getNumberSeriesId(), run.getTemplateId());
            if (result.isSuccess()) {
                BigDecimal grossAmount = result.getInvoice().getGrossAmount();
                if (run.getFilterMinAmount() != null && grossAmount != null
                        && grossAmount.compareTo(run.getFilterMinAmount()) < 0) {
                    invoiceRepository.delete(result.getInvoice());
                    return null;
                }
                return result;
            } else {
                ValidationReport vr = result.getValidationReport();
                if (vr != null) {
                    vr.blockingFailures().forEach(f -> allFailures.add(
                        ValidationFailureEntry.builder()
                            .customerId(customerId)
                            .customerName(customer.getName())
                            .ruleType(f.getRule())
                            .severity("BLOCKING")
                            .description(f.getDescription())
                            .build()));
                }
                return null;
            }
        } catch (Exception ex) {
            log.error("Failed to generate invoice for customer {}", customerId, ex);
            allFailures.add(ValidationFailureEntry.builder()
                .customerId(customerId)
                .customerName(customer.getName())
                .ruleType("SYSTEM_ERROR")
                .severity("BLOCKING")
                .description(ex.getMessage())
                .build());
            return null;
        }
    }

    private BillingFrequency resolveFilterFrequency(String filterBillingFrequency) {
        if (filterBillingFrequency == null || filterBillingFrequency.isBlank()) return null;
        try { return BillingFrequency.valueOf(filterBillingFrequency); }
        catch (IllegalArgumentException e) { return null; }
    }

    private List<BillingEvent> filterEventsByDueCycles(
            List<BillingEvent> events, List<BillingCycle> customerCycles, InvoiceRun run) {
        return events.stream()
            .filter(event -> {
                String productCode = event.getProduct() != null ? event.getProduct().getCode() : null;
                for (BillingCycle cycle : customerCycles) {
                    boolean serviceMatch = cycle.getServiceType() == null
                        || cycle.getServiceType().equals(productCode);
                    if (!serviceMatch) continue;
                    LocalDate periodStart = run.getFilterPeriodFrom() != null
                        ? run.getFilterPeriodFrom()
                        : billingCycleService.computePeriodStart(cycle);
                    LocalDate periodEnd = run.getFilterPeriodTo() != null
                        ? run.getFilterPeriodTo()
                        : cycle.getNextBillingDate().minusDays(1);
                    if (!event.getEventDate().isBefore(periodStart)
                            && !event.getEventDate().isAfter(periodEnd)) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
    }
}
