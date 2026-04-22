package com.example.invoicing.entity.invoice.dto;

import com.example.invoicing.entity.invoice.dto.InvoicePreviewEntry;
import com.example.invoicing.entity.invoice.dto.ValidationFailureEntry;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class SimulationReport {
    private int totalCustomers;
    private int totalInvoices;
    private int failedCustomers;
    private BigDecimal totalNetAmount;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalVatAmount;
    private List<ValidationFailureEntry> validationFailures;
    private List<InvoicePreviewEntry> sampleLineItems;
    private List<CategoryBreakdownEntry> categoryBreakdown;
    private List<CostCentreAllocationEntry> costCentreAllocations;
    private boolean simulationMode;
}
