package com.example.invoicing.service;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.entity.invoice.dto.InvoiceGenerationResult;
import com.example.invoicing.service.InvoiceGenerationService;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.entity.classification.LegalClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Async("invoiceRunExecutor")
    @Transactional
    public void executeRun(Long runId) {
        InvoiceRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus(InvoiceRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        List<String> lockedCustomers = new ArrayList<>();
        try {
            List<BillingEvent> events = billingEventRepository.findByRunFilter(
                run.getFilterMunicipality(),
                run.getFilterPeriodFrom(),
                run.getFilterPeriodTo(),
                null,
                resolveClassification(run.getFilterServiceResponsibility()),
                run.getFilterLocation()
            );

            Map<String, List<BillingEvent>> byCustomerNumber = events.stream()
                .collect(Collectors.groupingBy(BillingEvent::getCustomerNumber));

            lockedCustomers = new ArrayList<>(byCustomerNumber.keySet());
            lockService.lockCustomers(runId, lockedCustomers);
            run.setLockedCustomerCount(lockedCustomers.size());
            runRepository.save(run);

            int totalInvoices = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
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
                Long customerId = customerOpt.get().getId();

                try {
                    InvoiceGenerationResult result =
                        invoiceGenerationService.generate(customerEvents, customerId, false);

                    if (result.isSuccess()) {
                        totalInvoices++;
                        if (result.getInvoice().getGrossAmount() != null) {
                            totalAmount = totalAmount.add(result.getInvoice().getGrossAmount());
                        }
                    } else {
                        ValidationReport vr = result.getValidationReport();
                        if (vr != null) {
                            vr.blockingFailures().forEach(f -> allFailures.add(
                                ValidationFailureEntry.builder()
                                    .customerId(customerId)
                                    .customerName(customerOpt.get().getName())
                                    .ruleType(f.getRule())
                                    .severity("BLOCKING")
                                    .description(f.getDescription())
                                    .build()));
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to generate invoice for customer {}", customerId, ex);
                    allFailures.add(ValidationFailureEntry.builder()
                        .customerId(customerId)
                        .customerName(customerOpt.get().getName())
                        .ruleType("SYSTEM_ERROR")
                        .severity("BLOCKING")
                        .description(ex.getMessage())
                        .build());
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

    private LegalClassification resolveClassification(String serviceResponsibility) {
        if (serviceResponsibility == null) return null;
        try { return LegalClassification.valueOf(serviceResponsibility); }
        catch (IllegalArgumentException e) { return null; }
    }
}
