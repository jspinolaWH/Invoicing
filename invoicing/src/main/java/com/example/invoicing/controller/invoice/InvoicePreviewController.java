package com.example.invoicing.controller.invoice;
import com.example.invoicing.service.InvoiceGenerationService;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.validation.ValidationFailure;
import com.example.invoicing.entity.validation.ValidationSeverity;
import com.example.invoicing.entity.invoice.dto.*;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.service.InvoiceService;
import com.example.invoicing.entity.invoice.dto.InvoiceLineItemResponse;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoicePreviewController {

    private final InvoiceGenerationService generationService;
    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceService invoiceService;

    @PostMapping("/preview")
    public ResponseEntity<SimulationReport> preview(@RequestBody InvoicePreviewRequest request) {
        List<BillingEvent> events;
        if (request.getEventIds() != null && !request.getEventIds().isEmpty()) {
            events = billingEventRepository.findAllById(request.getEventIds());
        } else {
            String customerNumber = customerRepository.findById(request.getCustomerId())
                .map(c -> c.getBillingProfile() != null ? c.getBillingProfile().getCustomerIdNumber() : null)
                .orElse(null);
            if (customerNumber == null) {
                return ResponseEntity.badRequest().build();
            }
            events = billingEventRepository.findUnbilledByCustomerAndDateRange(
                customerNumber,
                request.getBillingPeriodFrom(),
                request.getBillingPeriodTo());
        }

        InvoiceGenerationResult result = generationService.generate(events, request.getCustomerId(), true);

        String customerName = customerRepository.findById(request.getCustomerId())
            .map(c -> c.getName())
            .orElse("Unknown");

        List<ValidationFailureEntry> failures = new ArrayList<>();
        if (result.getValidationReport() != null && result.getValidationReport().getFailures() != null) {
            failures = result.getValidationReport().getFailures().stream()
                .map(f -> ValidationFailureEntry.builder()
                    .customerId(request.getCustomerId())
                    .customerName(customerName)
                    .ruleType(f.getRule())
                    .severity(f.getSeverity() == ValidationSeverity.BLOCKING ? "BLOCKING" : "WARNING")
                    .description(f.getDescription())
                    .build())
                .collect(Collectors.toList());
        }

        List<InvoiceLineItemResponse> lineItemResponses = new ArrayList<>();
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimal.ZERO;

        if (result.getInvoice() != null) {
            lineItemResponses = invoiceService.toResponse(result.getInvoice()).getLineItems();
            netTotal = result.getInvoice().getNetAmount() != null ? result.getInvoice().getNetAmount() : BigDecimal.ZERO;
            grossTotal = result.getInvoice().getGrossAmount() != null ? result.getInvoice().getGrossAmount() : BigDecimal.ZERO;
        }

        InvoicePreviewEntry previewEntry = InvoicePreviewEntry.builder()
            .customerId(request.getCustomerId())
            .customerName(customerName)
            .netAmount(netTotal)
            .grossAmount(grossTotal)
            .lineItemCount(lineItemResponses.size())
            .lineItems(lineItemResponses)
            .build();

        SimulationReport report = SimulationReport.builder()
            .simulationMode(true)
            .totalCustomers(1)
            .totalInvoices(result.isSuccess() ? 1 : 0)
            .totalNetAmount(netTotal)
            .totalGrossAmount(grossTotal)
            .totalVatAmount(grossTotal.subtract(netTotal))
            .validationFailures(failures)
            .sampleLineItems(List.of(previewEntry))
            .build();

        return ResponseEntity.ok(report);
    }
}
