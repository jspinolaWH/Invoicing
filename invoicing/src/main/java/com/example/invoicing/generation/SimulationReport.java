package com.example.invoicing.generation;

import com.example.invoicing.generation.dto.InvoicePreviewEntry;
import com.example.invoicing.generation.dto.ValidationFailureEntry;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class SimulationReport {
    private int totalCustomers;
    private int totalInvoices;
    private BigDecimal totalNetAmount;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalVatAmount;
    private List<ValidationFailureEntry> validationFailures;
    private List<InvoicePreviewEntry> sampleLineItems;
    private boolean simulationMode;
}
