package com.example.invoicing.entity.billingevent;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "billing_events",
    indexes = {
        @Index(name = "idx_billing_events_customer_date", columnList = "customer_number,event_date"),
        @Index(name = "idx_billing_events_status",        columnList = "status"),
        @Index(name = "idx_billing_events_excluded",      columnList = "excluded"),
        @Index(name = "idx_billing_events_review",        columnList = "office_review_required,reviewed_at"),
        @Index(name = "idx_billing_events_shared_group",  columnList = "shared_service_group_id"),
        @Index(name = "idx_billing_events_project",       columnList = "project_id")
    })
@Getter @Setter @NoArgsConstructor
public class BillingEvent extends BaseAuditEntity {

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "waste_fee_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal wasteFeePrice;

    @Column(name = "transport_fee_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal transportFeePrice;

    @Column(name = "eco_fee_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal ecoFeePrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal weight;

    @Column(name = "vat_rate_0", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate0;

    @Column(name = "vat_rate_24", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate24;

    @Column(name = "vehicle_id", length = 20)
    private String vehicleId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_account_id")
    private AccountingAccount accountingAccount;

    @Column(name = "customer_number", nullable = false, length = 9)
    private String customerNumber;

    @Column(length = 200)
    private String contractor;

    @Column(name = "location_id", length = 100)
    private String locationId;

    @Column(name = "municipality_id", length = 100)
    private String municipalityId;

    @Column(name = "shared_service_group_percentage", precision = 5, scale = 2)
    private BigDecimal sharedServiceGroupPercentage;

    @Column(name = "shared_service_group_id", length = 100)
    private String sharedServiceGroupId;

    @Column(length = 2000)
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingEventStatus status = BillingEventStatus.IN_PROGRESS;

    @Column(nullable = false)
    private boolean excluded = false;

    @Column(name = "exclusion_reason", length = 1000)
    private String exclusionReason;

    @Column(name = "excluded_by", length = 100)
    private String excludedBy;

    @Column(name = "excluded_at")
    private Instant excludedAt;

    @Column(name = "non_billable", nullable = false)
    private boolean nonBillable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_classification", length = 20)
    private LegalClassification legalClassification;

    @Column(name = "office_review_required", nullable = false)
    private boolean officeReviewRequired = false;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "project_id", length = 100)
    private String projectId;

    @Column(length = 50)
    private String origin;

    @Column(name = "corrected_from_event_id")
    private Long correctedFromEventId;
}
