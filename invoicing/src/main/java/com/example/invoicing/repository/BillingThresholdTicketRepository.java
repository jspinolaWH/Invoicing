package com.example.invoicing.repository;

import com.example.invoicing.entity.ticket.BillingThresholdTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingThresholdTicketRepository extends JpaRepository<BillingThresholdTicket, Long> {
    Optional<BillingThresholdTicket> findByTriggerId(Long triggerId);
    List<BillingThresholdTicket> findAllByOrderByCreatedAtDesc();
}
