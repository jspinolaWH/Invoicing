package com.example.invoicing.service;
import com.example.invoicing.entity.invoice.dto.BillingCycleGroupEntry;
import com.example.invoicing.entity.invoice.dto.CategoryBreakdownEntry;
import com.example.invoicing.entity.invoice.dto.CostCentreAllocationEntry;
import com.example.invoicing.entity.invoice.dto.InvoiceGenerationResult;
import com.example.invoicing.entity.invoice.dto.InvoicePreviewEntry;
import com.example.invoicing.entity.invoice.dto.SimulationAuditEntry;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import com.example.invoicing.entity.invoice.dto.SimulationReport;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.entity.contract.Contract;

import com.example.invoicing.entity.billingcycle.BillingCycle;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.repository.BillingCycleRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.ContractRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.repository.MinimumFeeConfigRepository;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceSimulationService {

    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceGenerationService invoiceGenerationService;
    private final InvoiceRunRepository invoiceRunRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final BillingCycleService billingCycleService;
    private final MinimumFeeConfigRepository minimumFeeConfigRepository;
    private final MinimumFeeService minimumFeeService;
    private final ContractRepository contractRepository;

    @Transactional
    public SimulationReport simulate(InvoiceRunRequest request) {
        List<SimulationAuditEntry> auditLog = new ArrayList<>();

        List<BillingEvent> allEvents = billingEventRepository.findByRunFilter(
            request.getFilterMunicipality(),
            request.getFilterPeriodFrom(),
            request.getFilterPeriodTo(),
            null,
            resolveClassification(request.getFilterServiceResponsibility()),
            request.getFilterLocation()
        );
        auditLog.add(SimulationAuditEntry.builder()
            .timestamp(Instant.now())
            .step("EVENT_RETRIEVAL")
            .outcome("OK")
            .detail("Retrieved " + allEvents.size() + " billing event(s) matching run filters")
            .build());

        // Group by customerNumber (String)
        Map<String, List<BillingEvent>> byCustomerNumber = allEvents.stream()
            .collect(Collectors.groupingBy(BillingEvent::getCustomerNumber));
        auditLog.add(SimulationAuditEntry.builder()
            .timestamp(Instant.now())
            .step("CUSTOMER_GROUPING")
            .outcome("OK")
            .detail("Grouped events across " + byCustomerNumber.size() + " customer(s)")
            .build());

        int totalCustomers = byCustomerNumber.size();
        int totalInvoices = 0;
        int failedCustomers = 0;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        int minimumFeeAdjustmentCount = 0;
        BigDecimal minimumFeeAdjustmentTotal = BigDecimal.ZERO;
        int minimumFeeExemptCount = 0;
        List<ValidationFailureEntry> allFailures = new ArrayList<>();
        List<InvoicePreviewEntry> previews = new ArrayList<>();

        // Collect category and cost centre breakdowns from all events up front
        Map<String, int[]> categoryCountMap = new LinkedHashMap<>();
        Map<String, BigDecimal[]> categoryAmountMap = new LinkedHashMap<>();
        Map<String, CostCenter> costCentreByCode = new LinkedHashMap<>();
        Map<String, int[]> costCentreCountMap = new LinkedHashMap<>();
        Map<String, BigDecimal[]> costCentreAmountMap = new LinkedHashMap<>();

        for (BillingEvent event : allEvents) {
            String category = event.getProduct() != null ? event.getProduct().getCode() : "UNKNOWN";
            BigDecimal eventNet = eventNetAmount(event);

            categoryCountMap.computeIfAbsent(category, k -> new int[]{0})[0]++;
            categoryAmountMap.computeIfAbsent(category, k -> new BigDecimal[]{BigDecimal.ZERO})[0] =
                categoryAmountMap.get(category)[0].add(eventNet);

            CostCenter cc = event.getCostCenter();
            if (cc != null) {
                String ccCode = cc.getCompositeCode();
                costCentreByCode.putIfAbsent(ccCode, cc);
                costCentreCountMap.computeIfAbsent(ccCode, k -> new int[]{0})[0]++;
                costCentreAmountMap.computeIfAbsent(ccCode, k -> new BigDecimal[]{BigDecimal.ZERO})[0] =
                    costCentreAmountMap.get(ccCode)[0].add(eventNet);
            }
        }

        for (Map.Entry<String, List<BillingEvent>> entry : byCustomerNumber.entrySet()) {
            String customerNumber = entry.getKey();
            List<BillingEvent> events = entry.getValue();

            // Find customer by customerIdNumber
            Optional<Customer> customerOpt = customerRepository
                .findByBillingProfile_CustomerIdNumber(customerNumber);
            if (customerOpt.isEmpty()) continue;
            Long customerId = customerOpt.get().getId();

            InvoiceGenerationResult result =
                invoiceGenerationService.generate(events, customerId, true);

            String custName = customerOpt.get().getName();
            auditLog.add(SimulationAuditEntry.builder()
                .timestamp(Instant.now())
                .step("PRICING_AND_VALIDATION")
                .outcome(result.isSuccess() ? "OK" : "BLOCKING_FAILURE")
                .detail("Customer " + custName + " (" + customerNumber + "): " + events.size() + " event(s) processed" +
                    (result.isSuccess() ? "" : " — blocked by validation"))
                .build());

            if (result.isSuccess()) {
                totalInvoices++;
                totalNet = totalNet.add(
                    result.getInvoice().getNetAmount() != null ? result.getInvoice().getNetAmount() : BigDecimal.ZERO);
                totalGross = totalGross.add(
                    result.getInvoice().getGrossAmount() != null ? result.getInvoice().getGrossAmount() : BigDecimal.ZERO);
                totalVat = totalVat.add(
                    result.getInvoice().getVatAmount() != null ? result.getInvoice().getVatAmount() : BigDecimal.ZERO);

                // Track minimum fee adjustments applied to this invoice
                for (InvoiceLineItem li : result.getInvoice().getLineItems()) {
                    if (li.getDescription() != null && li.getDescription().startsWith("Minimum fee adjustment")) {
                        minimumFeeAdjustmentCount++;
                        minimumFeeAdjustmentTotal = minimumFeeAdjustmentTotal.add(
                            li.getNetAmount() != null ? li.getNetAmount() : BigDecimal.ZERO);
                    }
                }

                // Track minimum fee exemptions: config exists but was not applied due to contract dates
                Customer cust = customerOpt.get();
                if (cust.getCustomerType() != null) {
                    List<BillingEvent> custEvents = entry.getValue();
                    LocalDate periodStart = custEvents.stream()
                        .map(BillingEvent::getEventDate).filter(java.util.Objects::nonNull)
                        .min(LocalDate::compareTo).orElse(null);
                    LocalDate periodEnd = custEvents.stream()
                        .map(BillingEvent::getEventDate).filter(java.util.Objects::nonNull)
                        .max(LocalDate::compareTo).orElse(null);
                    if (periodStart != null && periodEnd != null) {
                        long months = java.time.temporal.ChronoUnit.MONTHS.between(periodStart, periodEnd.plusDays(1));
                        PeriodType periodType = months >= 9 ? PeriodType.ANNUAL : PeriodType.QUARTERLY;
                        String custTypeStr = cust.getCustomerType().name();
                        String lookupNumber = cust.getBillingProfile() != null
                            ? cust.getBillingProfile().getCustomerIdNumber() : customerNumber;
                        Contract activeContract =
                            contractRepository.findByCustomerNumberAndActiveTrue(lookupNumber)
                                .stream().findFirst().orElse(null);
                        LocalDate contractStart = activeContract != null ? activeContract.getStartDate() : null;
                        LocalDate contractEnd = activeContract != null ? activeContract.getEndDate() : null;
                        Optional<MinimumFeeConfig> configOpt =
                            minimumFeeConfigRepository.findByCustomerTypeAndPeriodTypeAndActiveTrue(custTypeStr, periodType);
                        if (configOpt.isPresent() && !minimumFeeService.isMinimumApplicable(
                                configOpt.get(), periodStart, periodEnd, contractStart, contractEnd)) {
                            minimumFeeExemptCount++;
                        }
                    }
                }

                if (previews.size() < 5) {
                    previews.add(InvoicePreviewEntry.builder()
                        .customerId(customerId)
                        .customerName(custName)
                        .netAmount(result.getInvoice().getNetAmount())
                        .grossAmount(result.getInvoice().getGrossAmount())
                        .lineItemCount(result.getInvoice().getLineItems().size())
                        .lineItems(List.of())
                        .build());
                }

                ValidationReport vr = result.getValidationReport();
                if (vr != null) {
                    vr.warnings().forEach(w -> allFailures.add(ValidationFailureEntry.builder()
                        .customerId(customerId)
                        .customerName(customerOpt.get().getName())
                        .ruleType(w.getRule())
                        .severity("WARNING")
                        .description(w.getDescription())
                        .build()));
                }
            } else {
                failedCustomers++;
                ValidationReport vr = result.getValidationReport();
                if (vr != null) {
                    vr.blockingFailures().forEach(f -> allFailures.add(ValidationFailureEntry.builder()
                        .customerId(customerId)
                        .customerName(customerOpt.get().getName())
                        .ruleType(f.getRule())
                        .severity("BLOCKING")
                        .description(f.getDescription())
                        .build()));
                }
            }
        }

        List<CategoryBreakdownEntry> categoryBreakdown = categoryCountMap.entrySet().stream()
            .map(e -> CategoryBreakdownEntry.builder()
                .category(e.getKey())
                .eventCount(e.getValue()[0])
                .netAmount(categoryAmountMap.get(e.getKey())[0])
                .build())
            .collect(Collectors.toList());

        List<CostCentreAllocationEntry> costCentreAllocations = costCentreCountMap.entrySet().stream()
            .map(e -> {
                CostCenter cc = costCentreByCode.get(e.getKey());
                BigDecimal net = costCentreAmountMap.get(e.getKey())[0];
                return CostCentreAllocationEntry.builder()
                    .costCentreCode(e.getKey())
                    .costCentreDescription(cc != null ? cc.getDescription() : null)
                    .eventCount(e.getValue()[0])
                    .netAmount(net)
                    .vatAmount(BigDecimal.ZERO)
                    .build();
            })
            .collect(Collectors.toList());

        List<BillingCycleGroupEntry> billingCycleGrouping = computeCycleGrouping(allEvents, request);

        auditLog.add(SimulationAuditEntry.builder()
            .timestamp(Instant.now())
            .step("ACCOUNTING_ALLOCATION")
            .outcome("OK")
            .detail("Cost centre and accounting assignments reviewed for " + totalInvoices + " invoice(s)")
            .build());
        auditLog.add(SimulationAuditEntry.builder()
            .timestamp(Instant.now())
            .step("SIMULATION_COMPLETE")
            .outcome(failedCustomers == 0 ? "OK" : "COMPLETED_WITH_ERRORS")
            .detail("Simulation finished: " + totalInvoices + " invoice(s) generated, " +
                failedCustomers + " customer(s) blocked, " +
                allFailures.stream().filter(f -> "BLOCKING".equals(f.getSeverity())).count() + " blocking error(s), " +
                allFailures.stream().filter(f -> "WARNING".equals(f.getSeverity())).count() + " warning(s)")
            .build());

        SimulationReport report = SimulationReport.builder()
            .simulationMode(true)
            .totalCustomers(totalCustomers)
            .totalInvoices(totalInvoices)
            .failedCustomers(failedCustomers)
            .totalNetAmount(totalNet)
            .totalGrossAmount(totalGross)
            .totalVatAmount(totalVat)
            .validationFailures(allFailures)
            .sampleLineItems(previews)
            .categoryBreakdown(categoryBreakdown)
            .costCentreAllocations(costCentreAllocations)
            .billingCycleGrouping(billingCycleGrouping)
            .minimumFeeAdjustmentCount(minimumFeeAdjustmentCount)
            .minimumFeeAdjustmentTotal(minimumFeeAdjustmentTotal)
            .minimumFeeExemptCount(minimumFeeExemptCount)
            .simulationAuditLog(auditLog)
            .build();

        persistSimulationRun(request, report, allFailures, minimumFeeAdjustmentCount, minimumFeeAdjustmentTotal, minimumFeeExemptCount);

        return report;
    }

    private List<BillingCycleGroupEntry> computeCycleGrouping(List<BillingEvent> allEvents, InvoiceRunRequest request) {
        List<BillingCycle> dueCycles = billingCycleRepository.findCyclesDueInRunWindow(LocalDate.now());
        if (dueCycles.isEmpty()) return List.of();

        // Group cycles by frequency + serviceType key
        record CycleKey(String frequency, String serviceType) {}
        Map<CycleKey, BillingCycle> firstCycleByKey = new LinkedHashMap<>();
        Map<CycleKey, Set<String>> customersByKey = new LinkedHashMap<>();

        for (BillingCycle cycle : dueCycles) {
            CycleKey key = new CycleKey(cycle.getFrequency().name(), cycle.getServiceType());
            firstCycleByKey.putIfAbsent(key, cycle);
            customersByKey.computeIfAbsent(key, k -> new HashSet<>()).add(cycle.getCustomerNumber());
        }

        List<BillingCycleGroupEntry> result = new ArrayList<>();
        for (Map.Entry<CycleKey, BillingCycle> entry : firstCycleByKey.entrySet()) {
            CycleKey key = entry.getKey();
            BillingCycle representativeCycle = entry.getValue();
            LocalDate periodStart = request.getFilterPeriodFrom() != null
                ? request.getFilterPeriodFrom()
                : billingCycleService.computePeriodStart(representativeCycle);
            LocalDate periodEnd = request.getFilterPeriodTo() != null
                ? request.getFilterPeriodTo()
                : representativeCycle.getNextBillingDate().minusDays(1);
            Set<String> cycleCustomers = customersByKey.get(key);

            int evtCount = 0;
            BigDecimal net = BigDecimal.ZERO;
            Set<String> customersWithEvents = new HashSet<>();
            for (BillingEvent event : allEvents) {
                if (!cycleCustomers.contains(event.getCustomerNumber())) continue;
                if (key.serviceType() != null) {
                    String productCode = event.getProduct() != null ? event.getProduct().getCode() : null;
                    if (!key.serviceType().equals(productCode)) continue;
                }
                if (event.getEventDate().isBefore(periodStart) || event.getEventDate().isAfter(periodEnd)) continue;
                evtCount++;
                net = net.add(eventNetAmount(event));
                customersWithEvents.add(event.getCustomerNumber());
            }

            if (evtCount > 0) {
                result.add(BillingCycleGroupEntry.builder()
                    .frequency(key.frequency())
                    .serviceType(key.serviceType())
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .customerCount(customersWithEvents.size())
                    .eventCount(evtCount)
                    .netAmount(net)
                    .build());
            }
        }
        return result;
    }

    private void persistSimulationRun(InvoiceRunRequest request, SimulationReport report,
                                      List<ValidationFailureEntry> allFailures,
                                      int minFeeAdjCount, BigDecimal minFeeAdjTotal, int minFeeExemptCount) {
        InvoiceRun run = new InvoiceRun();
        run.setSimulationMode(true);
        run.setStatus(InvoiceRunStatus.COMPLETED);
        run.setStartedAt(Instant.now());
        run.setCompletedAt(Instant.now());
        run.setFilterMunicipality(request.getFilterMunicipality());
        run.setFilterPeriodFrom(request.getFilterPeriodFrom());
        run.setFilterPeriodTo(request.getFilterPeriodTo());
        run.setFilterLocation(request.getFilterLocation());
        run.setFilterServiceResponsibility(request.getFilterServiceResponsibility());
        run.setTotalInvoices(report.getTotalInvoices());
        run.setTotalAmount(report.getTotalGrossAmount());
        run.setReportTotalChecked(report.getTotalCustomers());
        run.setReportPassed(report.getTotalInvoices());
        run.setReportBlockingCount((int) allFailures.stream()
            .filter(f -> "BLOCKING".equals(f.getSeverity())).count());
        run.setReportWarningCount((int) allFailures.stream()
            .filter(f -> "WARNING".equals(f.getSeverity())).count());
        run.setMinimumFeeAdjustmentCount(minFeeAdjCount);
        run.setMinimumFeeAdjustmentTotal(minFeeAdjTotal);
        run.setMinimumFeeExemptCount(minFeeExemptCount);
        invoiceRunRepository.save(run);
    }

    private BigDecimal eventNetAmount(BillingEvent event) {
        BigDecimal fee = BigDecimal.ZERO;
        if (event.getWasteFeePrice() != null) fee = fee.add(event.getWasteFeePrice());
        if (event.getTransportFeePrice() != null) fee = fee.add(event.getTransportFeePrice());
        if (event.getEcoFeePrice() != null) fee = fee.add(event.getEcoFeePrice());
        BigDecimal qty = event.getQuantity() != null ? event.getQuantity() : BigDecimal.ONE;
        return fee.multiply(qty);
    }

    private LegalClassification resolveClassification(String serviceResponsibility) {
        if (serviceResponsibility == null) return null;
        try { return LegalClassification.valueOf(serviceResponsibility); }
        catch (IllegalArgumentException e) { return null; }
    }
}
