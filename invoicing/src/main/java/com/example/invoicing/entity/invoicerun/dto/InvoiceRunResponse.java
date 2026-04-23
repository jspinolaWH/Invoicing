package com.example.invoicing.entity.invoicerun.dto;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class InvoiceRunResponse {
    private Long id;
    private boolean simulationMode;
    private InvoiceRunStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Integer totalInvoices;
    private BigDecimal totalAmount;
    private String filterMunicipality;
    private LocalDate filterPeriodFrom;
    private LocalDate filterPeriodTo;
    private BigDecimal filterMinAmount;
    private String filterCustomerType;
    private String filterServiceType;
    private String filterLocation;
    private String filterServiceResponsibility;
    private String filterBillingFrequency;
    private Integer reportTotalChecked;
    private Integer reportPassed;
    private Integer reportBlockingCount;
    private Integer reportWarningCount;
    private Instant scheduledSendAt;
    private Instant sentAt;
    private String cancellationReason;
    private String batchAttachmentIdentifier;
    private String batchAttachmentFilename;
    private String batchAttachmentMimeType;
    private String batchAttachmentSecurityClass;
    private Long templateId;
    private Long numberSeriesId;
    private Integer minimumFeeAdjustmentCount;
    private BigDecimal minimumFeeAdjustmentTotal;
    private Integer minimumFeeExemptCount;

    public static InvoiceRunResponse from(InvoiceRun run) {
        return InvoiceRunResponse.builder()
            .id(run.getId())
            .simulationMode(run.isSimulationMode())
            .status(run.getStatus())
            .startedAt(run.getStartedAt())
            .completedAt(run.getCompletedAt())
            .totalInvoices(run.getTotalInvoices())
            .totalAmount(run.getTotalAmount())
            .filterMunicipality(run.getFilterMunicipality())
            .filterPeriodFrom(run.getFilterPeriodFrom())
            .filterPeriodTo(run.getFilterPeriodTo())
            .filterMinAmount(run.getFilterMinAmount())
            .filterCustomerType(run.getFilterCustomerType())
            .filterServiceType(run.getFilterServiceType())
            .filterLocation(run.getFilterLocation())
            .filterServiceResponsibility(run.getFilterServiceResponsibility())
            .filterBillingFrequency(run.getFilterBillingFrequency())
            .reportTotalChecked(run.getReportTotalChecked())
            .reportPassed(run.getReportPassed())
            .reportBlockingCount(run.getReportBlockingCount())
            .reportWarningCount(run.getReportWarningCount())
            .scheduledSendAt(run.getScheduledSendAt())
            .sentAt(run.getSentAt())
            .cancellationReason(run.getCancellationReason())
            .batchAttachmentIdentifier(run.getBatchAttachmentIdentifier())
            .batchAttachmentFilename(run.getBatchAttachmentFilename())
            .batchAttachmentMimeType(run.getBatchAttachmentMimeType())
            .batchAttachmentSecurityClass(run.getBatchAttachmentSecurityClass())
            .templateId(run.getTemplateId())
            .numberSeriesId(run.getNumberSeriesId())
            .minimumFeeAdjustmentCount(run.getMinimumFeeAdjustmentCount())
            .minimumFeeAdjustmentTotal(run.getMinimumFeeAdjustmentTotal())
            .minimumFeeExemptCount(run.getMinimumFeeExemptCount())
            .build();
    }
}
