package com.example.invoicing.entity.billingevent.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingEventTransferLogRepository extends JpaRepository<BillingEventTransferLog, Long> {

    List<BillingEventTransferLog> findByBillingEventIdOrderByTransferredAtDesc(Long billingEventId);

    List<BillingEventTransferLog> findBySourceCustomerNumberOrderByTransferredAtDesc(String customerNumber);

    List<BillingEventTransferLog> findByTargetCustomerNumberOrderByTransferredAtDesc(String customerNumber);
}
