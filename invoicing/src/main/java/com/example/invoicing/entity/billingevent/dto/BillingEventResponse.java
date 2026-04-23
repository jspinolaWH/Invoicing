package com.example.invoicing.entity.billingevent.dto;

import com.example.invoicing.entity.billingevent.BillingEventDirection;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.BillingEventValidationStatus;
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
    private BillingEventDirection direction;
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
    private String wasteType;
    private String receivingSite;
    private boolean priceOverridden;
    private String pendingTransferCustomerNumber;
    // Selective component invoicing (AC3)
    private boolean includeWasteFee;
    private boolean includeTransportFee;
    private boolean includeEcoFee;
    // Contractor payment (AC5)
    private String contractorPaymentStatus;
    // Validation status (PD-278)
    private BillingEventValidationStatus validationStatus;
    private java.time.Instant lastValidatedAt;
    // Resolved billing type based on active restrictions (PD-292 AC10)
    private String resolvedBillingType;
}
