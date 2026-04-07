package com.example.invoicing.simulation;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.generation.InvoiceGenerationResult;
import com.example.invoicing.generation.InvoiceGenerationService;
import com.example.invoicing.generation.SimulationReport;
import com.example.invoicing.generation.dto.InvoicePreviewEntry;
import com.example.invoicing.generation.dto.ValidationFailureEntry;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.run.dto.InvoiceRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceSimulationService {

    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceGenerationService invoiceGenerationService;

    @Transactional(readOnly = true)
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
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        List<ValidationFailureEntry> allFailures = new ArrayList<>();
        List<InvoicePreviewEntry> previews = new ArrayList<>();

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

        return SimulationReport.builder()
            .simulationMode(true)
            .totalCustomers(totalCustomers)
            .totalInvoices(totalInvoices)
            .totalNetAmount(totalNet)
            .totalGrossAmount(totalGross)
            .totalVatAmount(totalVat)
            .validationFailures(allFailures)
            .sampleLineItems(previews)
            .build();
    }

    private LegalClassification resolveClassification(String serviceResponsibility) {
        if (serviceResponsibility == null) return null;
        try { return LegalClassification.valueOf(serviceResponsibility); }
        catch (IllegalArgumentException e) { return null; }
    }
}
