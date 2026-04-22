package com.example.invoicing.service.retroactive;

import com.example.invoicing.entity.billingevent.retroactive.ServiceResponsibilityChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ServiceResponsibilityChangeEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ServiceResponsibilityChangedEvent event) {
        log.info("ServiceResponsibilityChanged: runId={} from={} to={} newResponsibility={} effectiveDate={} affectedCount={} appliedBy={}",
            event.getChangeRunId(),
            event.getFromCustomerNumber(),
            event.getToCustomerNumber(),
            event.getNewServiceResponsibility(),
            event.getChangeEffectiveDate(),
            event.getAffectedCount(),
            event.getAppliedBy());
    }
}
