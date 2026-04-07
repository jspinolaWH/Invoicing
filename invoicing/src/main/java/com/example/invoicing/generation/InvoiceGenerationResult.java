package com.example.invoicing.generation;

import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.invoice.Invoice;
import lombok.*;

import java.util.List;

@Getter
public class InvoiceGenerationResult {

    private final boolean success;
    private final Invoice invoice;
    private final List<String> profileErrors;
    private final ValidationReport validationReport;
    private final boolean simulation;

    private InvoiceGenerationResult(boolean success, Invoice invoice,
                                     List<String> profileErrors,
                                     ValidationReport validationReport,
                                     boolean simulation) {
        this.success = success;
        this.invoice = invoice;
        this.profileErrors = profileErrors;
        this.validationReport = validationReport;
        this.simulation = simulation;
    }

    public static InvoiceGenerationResult success(Invoice invoice) {
        return new InvoiceGenerationResult(true, invoice, null, null, false);
    }

    public static InvoiceGenerationResult simulation(Invoice preview, ValidationReport report) {
        return new InvoiceGenerationResult(true, preview, null, report, true);
    }

    public static InvoiceGenerationResult profileError(Long customerId, List<String> issues) {
        return new InvoiceGenerationResult(false, null, issues, null, false);
    }

    public static InvoiceGenerationResult validationError(Long customerId, ValidationReport report) {
        return new InvoiceGenerationResult(false, null, null, report, false);
    }
}
