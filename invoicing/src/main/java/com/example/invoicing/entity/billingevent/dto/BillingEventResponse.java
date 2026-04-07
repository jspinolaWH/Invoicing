package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.classification.LegalClassification;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class BillingEventResponse {
    private Long id;
    private LocalDate eventDate;
    private ProductSummaryDto product;
    private BigDecimal wasteFeePrice;
    private BigDecimal transportFeePrice;
    private BigDecimal ecoFeePrice;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal vatRate0;
    private BigDecimal vatRate24;
    private String vehicleId;
    private String driverId;
    private String customerNumber;
    private String contractor;
    private String locationId;
    private String municipalityId;
    private String sharedServiceGroupId;
    private BigDecimal sharedServiceGroupPercentage;
    private String comments;
    private BillingEventStatus status;
    private boolean excluded;
    private String exclusionReason;
    private boolean nonBillable;
    private LegalClassification legalClassification;
    private boolean officeReviewRequired;
    private String projectId;
    private String origin;
    private AccountSummaryDto accountingAccount;
    private CostCenterSummaryDto costCenter;
    private Instant createdAt;
    private String createdBy;
}
