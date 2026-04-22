package com.example.invoicing.service;
import com.example.invoicing.entity.invoice.dto.CategoryBreakdownEntry;
import com.example.invoicing.entity.invoice.dto.CostCentreAllocationEntry;
import com.example.invoicing.entity.invoice.dto.InvoiceGenerationResult;
import com.example.invoicing.entity.invoice.dto.InvoicePreviewEntry;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import com.example.invoicing.entity.invoice.dto.SimulationReport;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceSimulationService {

    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceGenerationService invoiceGenerationService;
    private final InvoiceRunRepository invoiceRunRepository;

    @Transactional
    public SimulationReport simulate(InvoiceRunRequest request) {
        List<BillingEvent> allEvents = billingEventRepository.findByRunFilter(
            request.getFilterMunicipality(),
            request.getFilterPeriodFrom(),
            request.getFilterPeriodTo(),
            null,
            resolveClassification(request.getFilterServiceResponsibility()),
            request.getFilterLocation()
        );

        // Group by customerNumber (String)
        Map<String, List<BillingEvent>> byCustomerNumber = allEvents.stream()
            .collect(Collectors.groupingBy(BillingEvent::getCustomerNumber));

        int totalCustomers = byCustomerNumber.size();
        int totalInvoices = 0;
        int failedCustomers = 0;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
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

            if (result.isSuccess()) {
                totalInvoices++;
                totalNet = totalNet.add(
                    result.getInvoice().getNetAmount() != null ? result.getInvoice().getNetAmount() : BigDecimal.ZERO);
                totalGross = totalGross.add(
                    result.getInvoice().getGrossAmount() != null ? result.getInvoice().getGrossAmount() : BigDecimal.ZERO);
                totalVat = totalVat.add(
                    result.getInvoice().getVatAmount() != null ? result.getInvoice().getVatAmount() : BigDecimal.ZERO);

                if (previews.size() < 5) {
                    String custName = customerOpt.get().getName();
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
            .build();

        persistSimulationRun(request, report, allFailures);

        return report;
    }

    private void persistSimulationRun(InvoiceRunRequest request, SimulationReport report,
                                      List<ValidationFailureEntry> allFailures) {
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
