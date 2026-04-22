package com.example.invoicing.controller;

import com.example.invoicing.entity.ticket.BillingThresholdTicket;
import com.example.invoicing.entity.trigger.BillingThresholdTrigger;
import com.example.invoicing.repository.BillingThresholdTicketRepository;
import com.example.invoicing.repository.BillingThresholdTriggerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-threshold/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INVOICING')")
public class BillingThresholdTicketController {

    private final BillingThresholdTicketRepository ticketRepository;
    private final BillingThresholdTriggerRepository triggerRepository;

    @GetMapping
    public List<BillingThresholdTicket> list() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public BillingThresholdTicket get(@PathVariable Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingThresholdTicket not found: " + id));
    }

    @PostMapping("/convert/{triggerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingThresholdTicket convertTrigger(@PathVariable Long triggerId) {
        BillingThresholdTrigger trigger = triggerRepository.findById(triggerId)
            .orElseThrow(() -> new EntityNotFoundException("BillingThresholdTrigger not found: " + triggerId));

        ticketRepository.findByTriggerId(triggerId).ifPresent(t -> {
            throw new IllegalStateException("Ticket already exists for trigger " + triggerId);
        });

        trigger.setStatus(BillingThresholdTrigger.TriggerStatus.CONVERTED_TO_TICKET);
        triggerRepository.save(trigger);

        BillingThresholdTicket ticket = BillingThresholdTicket.builder()
            .triggerId(triggerId)
            .customerNumber(trigger.getCustomerNumber())
            .serviceResponsibility(trigger.getServiceResponsibility())
            .status(BillingThresholdTicket.TicketStatus.OPEN)
            .build();
        return ticketRepository.save(ticket);
    }
}
