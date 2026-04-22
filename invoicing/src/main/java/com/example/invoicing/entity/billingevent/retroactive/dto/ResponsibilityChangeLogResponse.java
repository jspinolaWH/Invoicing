package com.example.invoicing.entity.billingevent.retroactive.dto;

import com.example.invoicing.entity.billingevent.retroactive.ServiceResponsibilityChangeLog;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ResponsibilityChangeLogResponse {

    private Long id;
    private String changeRunId;
    private String appliedBy;
    private Instant appliedAt;
    private String fromCustomerNumber;
    private String toCustomerNumber;
    private String previousResponsibility;
    private String newResponsibility;
    private LocalDate changeEffectiveDate;
    private int affectedCount;
    private int movedInProgressCount;
    private int correctionCopiesCreated;
    private String reason;

    public static ResponsibilityChangeLogResponse from(ServiceResponsibilityChangeLog log) {
        return ResponsibilityChangeLogResponse.builder()
            .id(log.getId())
            .changeRunId(log.getChangeRunId())
            .appliedBy(log.getAppliedBy())
            .appliedAt(log.getAppliedAt())
            .fromCustomerNumber(log.getFromCustomerNumber())
            .toCustomerNumber(log.getToCustomerNumber())
            .previousResponsibility(log.getPreviousResponsibility())
            .newResponsibility(log.getNewResponsibility())
            .changeEffectiveDate(log.getChangeEffectiveDate())
            .affectedCount(log.getAffectedCount())
            .movedInProgressCount(log.getMovedInProgressCount())
            .correctionCopiesCreated(log.getCorrectionCopiesCreated())
            .reason(log.getReason())
            .build();
    }
}
