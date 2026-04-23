package com.example.invoicing.entity.invoicerun;

import com.example.invoicing.entity.BaseAuditEntity;
import com.example.invoicing.entity.invoice.Invoice;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "invoice_runs")
public class InvoiceRun extends BaseAuditEntity {

    @Column(name = "simulation_mode", nullable = false)
    private boolean simulationMode;

    @Column(name = "filter_municipality", length = 100)
    private String filterMunicipality;

    @Column(name = "filter_min_amount", precision = 19, scale = 4)
    private BigDecimal filterMinAmount;

    @Column(name = "filter_period_from")
    private java.time.LocalDate filterPeriodFrom;

    @Column(name = "filter_period_to")
    private java.time.LocalDate filterPeriodTo;

    @Column(name = "filter_customer_type", length = 50)
    private String filterCustomerType;

    @Column(name = "filter_service_type", length = 100)
    private String filterServiceType;

    @Column(name = "filter_location", length = 100)
    private String filterLocation;

    @Column(name = "filter_service_responsibility", length = 50)
    private String filterServiceResponsibility;

    @Column(name = "filter_billing_frequency", length = 20)
    private String filterBillingFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceRunStatus status = InvoiceRunStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_invoices")
    private Integer totalInvoices;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "report_total_checked")
    private Integer reportTotalChecked;

    @Column(name = "report_passed")
    private Integer reportPassed;

    @Column(name = "report_blocking_count")
    private Integer reportBlockingCount;

    @Column(name = "report_warning_count")
    private Integer reportWarningCount;

    @Column(name = "report_failures_json", columnDefinition = "TEXT")
    private String reportFailuresJson;

    @Column(name = "locked_customer_count")
    private Integer lockedCustomerCount;

    @Column(name = "scheduled_send_at")
    private Instant scheduledSendAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @OneToMany(mappedBy = "invoiceRun")
    private List<Invoice> invoices = new ArrayList<>();

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "number_series_id")
    private Long numberSeriesId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "batch_attachment_identifier", length = 255)
    private String batchAttachmentIdentifier;

    @Column(name = "batch_attachment_filename", length = 255)
    private String batchAttachmentFilename;

    @Column(name = "batch_attachment_mime_type", length = 100)
    private String batchAttachmentMimeType;

    @Column(name = "batch_attachment_security_class", length = 20)
    private String batchAttachmentSecurityClass;

    @Column(name = "minimum_fee_adjustment_count")
    private Integer minimumFeeAdjustmentCount;

    @Column(name = "minimum_fee_adjustment_total", precision = 19, scale = 4)
    private BigDecimal minimumFeeAdjustmentTotal;

    @Column(name = "minimum_fee_exempt_count")
    private Integer minimumFeeExemptCount;
}
