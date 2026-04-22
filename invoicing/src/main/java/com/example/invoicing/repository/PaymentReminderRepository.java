package com.example.invoicing.repository;

import com.example.invoicing.entity.paymentreminder.PaymentReminder;
import com.example.invoicing.entity.paymentreminder.PaymentReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {
    List<PaymentReminder> findByInvoice_IdOrderByReminderNumberAsc(Long invoiceId);
    List<PaymentReminder> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, PaymentReminderStatus status);
    List<PaymentReminder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
