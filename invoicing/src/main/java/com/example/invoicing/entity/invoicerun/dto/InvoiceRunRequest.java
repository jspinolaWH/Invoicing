package com.example.invoicing.entity.invoicerun.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceRunRequest {
    private boolean simulationMode;
    private String filterMunicipality;
    private BigDecimal filterMinAmount;
    private LocalDate filterPeriodFrom;
    private LocalDate filterPeriodTo;
    private String filterCustomerType;
    private String filterServiceType;
    private String filterLocation;
    private String filterServiceResponsibility;
}
