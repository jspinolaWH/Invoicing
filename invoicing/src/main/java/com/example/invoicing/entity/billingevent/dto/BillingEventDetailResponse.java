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
public class BillingEventDetailResponse {
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
    private String internalComments;
    private String registrationNumber;
    private BillingEventStatus status;
    private boolean excluded;
    private String exclusionReason;
    private String excludedBy;
    private Instant excludedAt;
    private boolean nonBillable;
    private LegalClassification legalClassification;
    private boolean officeReviewRequired;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private String projectId;
    private String origin;
    private AccountSummaryDto accountingAccount;
    private CostCenterSummaryDto costCenter;
    private Instant createdAt;
    private String createdBy;
    private Instant lastModifiedAt;
    private String lastModifiedBy;
    // Resolved cost centre (STEP-21)
    private String resolvedCostCenterCode;
    // VAT calculation (STEP-22)
    private String resolvedVatRateCode;
    private BigDecimal resolvedVatRatePercent;
    private boolean reverseCharge;
    private BigDecimal calculatedAmountNet;
    private BigDecimal calculatedAmountVat;
    private BigDecimal calculatedAmountGross;
    private String buyerVatNumber;
    private String wasteType;
    private String receivingSite;
    private String responsibilityArea;
    private String serviceResponsibility;
    private String productGroup;
    private String transmissionErrorReason;
    private boolean priceOverridden;
    private BigDecimal originalWasteFeePrice;
    private BigDecimal originalTransportFeePrice;
    private BigDecimal originalEcoFeePrice;
    private String pendingTransferCustomerNumber;
    private String pendingTransferLocationId;
    private String priorCustomerNumber;
    private String priorLocationId;
    // Selective component invoicing (AC3)
    private boolean includeWasteFee;
    private boolean includeTransportFee;
    private boolean includeEcoFee;
    // Contractor payment (AC5)
    private String contractorPaymentStatus;
    private String contractorPaymentNotes;
    private String contractorPaymentRecordedBy;
    private java.time.Instant contractorPaymentRecordedAt;
    // Validation status (PD-278)
    private BillingEventValidationStatus validationStatus;
    private java.time.Instant lastValidatedAt;
    private String validationOverrideReason;
    private String validationOverriddenBy;
    private java.time.Instant validationOverriddenAt;
    // Resolved billing type based on active restrictions (PD-292 AC10)
    private String resolvedBillingType;
}
