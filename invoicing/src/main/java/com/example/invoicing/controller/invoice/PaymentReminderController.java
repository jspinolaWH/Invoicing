package com.example.invoicing.controller.invoice;

import com.example.invoicing.entity.paymentreminder.PaymentReminder;
import com.example.invoicing.service.PaymentReminderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentReminderController {

    private final PaymentReminderService reminderService;

    @PostMapping("/invoices/{invoiceId}/payment-reminders")
    public ResponseEntity<PaymentReminder> create(
            @PathVariable Long invoiceId,
            @RequestBody(required = false) CreateReminderRequest body) {
        String message = body != null ? body.getMessage() : null;
        return ResponseEntity.ok(reminderService.createReminder(invoiceId, message));
    }

    @GetMapping("/invoices/{invoiceId}/payment-reminders")
    public List<PaymentReminder> listForInvoice(@PathVariable Long invoiceId) {
        return reminderService.getRemindersForInvoice(invoiceId);
    }

    @PostMapping("/payment-reminders/{id}/send")
    public ResponseEntity<PaymentReminder> send(@PathVariable Long id) {
        return ResponseEntity.ok(reminderService.sendReminder(id));
    }

    @GetMapping("/customers/{customerId}/payment-reminders")
    public List<PaymentReminder> listForCustomer(@PathVariable Long customerId) {
        return reminderService.getRemindersForCustomer(customerId);
    }

    @Data
    public static class CreateReminderRequest {
        private String message;
    }
}
