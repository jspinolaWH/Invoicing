package com.example.invoicing.repository;

import com.example.invoicing.entity.billingevent.BillingEventAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingEventAttachmentRepository extends JpaRepository<BillingEventAttachment, Long> {
    List<BillingEventAttachment> findByBillingEventId(Long billingEventId);
    long countByBillingEventId(Long billingEventId);
}
