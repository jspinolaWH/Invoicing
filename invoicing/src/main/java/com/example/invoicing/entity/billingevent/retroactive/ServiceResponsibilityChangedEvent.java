package com.example.invoicing.entity.billingevent.retroactive;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class ServiceResponsibilityChangedEvent extends ApplicationEvent {

    private final String changeRunId;
    private final String fromCustomerNumber;
    private final String toCustomerNumber;
    private final String newServiceResponsibility;
    private final LocalDate changeEffectiveDate;
    private final int affectedCount;
    private final String appliedBy;

    public ServiceResponsibilityChangedEvent(
            Object source,
            String changeRunId,
            String fromCustomerNumber,
            String toCustomerNumber,
            String newServiceResponsibility,
            LocalDate changeEffectiveDate,
            int affectedCount,
            String appliedBy) {
        super(source);
        this.changeRunId = changeRunId;
        this.fromCustomerNumber = fromCustomerNumber;
        this.toCustomerNumber = toCustomerNumber;
        this.newServiceResponsibility = newServiceResponsibility;
        this.changeEffectiveDate = changeEffectiveDate;
        this.affectedCount = affectedCount;
        this.appliedBy = appliedBy;
    }
}
