package com.example.invoicing.entity.billingevent.credit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingEventCreditLogRepository extends JpaRepository<BillingEventCreditLog, Long> {

    Optional<BillingEventCreditLog> findByOriginalEventId(Long originalEventId);

    Optional<BillingEventCreditLog> findByCreditEventId(Long creditEventId);

    Optional<BillingEventCreditLog> findByNewEventId(Long newEventId);
}
