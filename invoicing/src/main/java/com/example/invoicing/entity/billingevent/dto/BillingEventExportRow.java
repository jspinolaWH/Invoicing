package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class BillingEventExportRow {
    private Long eventId;
    private LocalDate eventDate;
    private String accountingAccount;
    private String responsibilityArea;
    private String productGroup;
    private String wasteType;
    private String serviceResponsibility;
    private String locationId;
    private String municipalityId;
    private String projectCode;
    private String costCenter;
    private String receivingSite;
    private BigDecimal calculatedAmountNet;
    private BigDecimal calculatedAmountVat;
    private BigDecimal calculatedAmountGross;
    private String customerNumber;
}
