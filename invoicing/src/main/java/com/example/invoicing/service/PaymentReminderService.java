package com.example.invoicing.service;

import com.example.invoicing.entity.customer.BillingAddress;
import com.example.invoicing.entity.customer.Customer;
import com.example.invoicing.entity.customer.event.BillingAddressChangedEvent;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.entity.paymentreminder.PaymentReminder;
import com.example.invoicing.entity.paymentreminder.PaymentReminderStatus;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.PaymentReminderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderService {

    private final PaymentReminderRepository reminderRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerBillingProfileRepository customerRepository;

    @Transactional
    public PaymentReminder createReminder(Long invoiceId, String message) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.COMPLETED) {
            throw new IllegalStateException("Reminders can only be created for SENT or COMPLETED invoices");
        }

        Customer customer = invoice.getCustomer();
        BillingAddress address = customer.getBillingProfile().getBillingAddress();
        String deliveryMethod = customer.getBillingProfile().getDeliveryMethod() != null
            ? customer.getBillingProfile().getDeliveryMethod().name()
            : "EMAIL";
        String recipientAddress = resolveRecipientAddress(address, deliveryMethod);

        List<PaymentReminder> existing = reminderRepository.findByInvoice_IdOrderByReminderNumberAsc(invoiceId);
        int nextNumber = existing.size() + 1;

        PaymentReminder reminder = new PaymentReminder();
        reminder.setInvoice(invoice);
        reminder.setCustomerId(customer.getId());
        reminder.setReminderNumber(nextNumber);
        reminder.setRecipientAddress(recipientAddress);
        reminder.setDeliveryMethod(deliveryMethod);
        reminder.setMessage(message != null ? message : buildDefaultMessage(invoice, nextNumber));
        reminder.setStatus(PaymentReminderStatus.PENDING);

        return reminderRepository.save(reminder);
    }

    @Transactional
    public PaymentReminder sendReminder(Long reminderId) {
        PaymentReminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new EntityNotFoundException("Payment reminder not found: " + reminderId));

        if (reminder.getStatus() != PaymentReminderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reminders can be sent, current status: " + reminder.getStatus());
        }

        // Refresh recipient address from current billing address before sending
        Customer customer = customerRepository.findById(reminder.getCustomerId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + reminder.getCustomerId()));
        BillingAddress address = customer.getBillingProfile().getBillingAddress();
        String deliveryMethod = reminder.getDeliveryMethod();
        reminder.setRecipientAddress(resolveRecipientAddress(address, deliveryMethod));

        reminder.setStatus(PaymentReminderStatus.SENT);
        reminder.setSentAt(Instant.now());
        log.info("Payment reminder #{} sent for invoice {} to {}", reminder.getReminderNumber(),
            reminder.getInvoice().getInvoiceNumber(), reminder.getRecipientAddress());

        return reminderRepository.save(reminder);
    }

    @Transactional(readOnly = true)
    public List<PaymentReminder> getRemindersForInvoice(Long invoiceId) {
        return reminderRepository.findByInvoice_IdOrderByReminderNumberAsc(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<PaymentReminder> getRemindersForCustomer(Long customerId) {
        return reminderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @EventListener
    @Transactional
    public void onBillingAddressChanged(BillingAddressChangedEvent event) {
        Customer customer = customerRepository.findById(event.getCustomerId()).orElse(null);
        if (customer == null) return;

        List<PaymentReminder> pending = reminderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
            event.getCustomerId(), PaymentReminderStatus.PENDING);

        if (pending.isEmpty()) return;

        BillingAddress address = customer.getBillingProfile().getBillingAddress();
        for (PaymentReminder reminder : pending) {
            String newAddress = resolveRecipientAddress(address, reminder.getDeliveryMethod());
            reminder.setRecipientAddress(newAddress);
        }
        reminderRepository.saveAll(pending);
        log.info("Updated recipient address on {} pending reminder(s) for customerId={}", pending.size(), event.getCustomerId());
    }

    private String resolveRecipientAddress(BillingAddress address, String deliveryMethod) {
        if ("EMAIL".equalsIgnoreCase(deliveryMethod) && address != null && address.getEmailAddress() != null) {
            return address.getEmailAddress();
        }
        if ("E_INVOICE".equalsIgnoreCase(deliveryMethod) && address != null && address.getEInvoicingAddress() != null) {
            return address.getEInvoicingAddress();
        }
        if (address != null && address.getStreetAddress() != null) {
            return String.join(", ", List.of(
                nvl(address.getStreetAddress()),
                nvl(address.getPostalCode()),
                nvl(address.getCity()),
                nvl(address.getCountryCode())
            ).stream().filter(s -> !s.isEmpty()).toList());
        }
        return null;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String buildDefaultMessage(Invoice invoice, int reminderNumber) {
        return "Payment reminder #" + reminderNumber + " for invoice " + invoice.getInvoiceNumber()
            + " (due: " + invoice.getDueDate() + "). Please settle the outstanding amount.";
    }
}
