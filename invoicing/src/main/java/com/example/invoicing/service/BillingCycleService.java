package com.example.invoicing.service;
import com.example.invoicing.repository.BillingCycleRepository;
import com.example.invoicing.entity.billingcycle.BillingFrequency;
import com.example.invoicing.entity.billingcycle.BillingCycle;

import com.example.invoicing.entity.billingcycle.dto.BillingCycleRequest;
import com.example.invoicing.entity.billingcycle.dto.BillingCycleResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingCycleService {

    private final BillingCycleRepository repository;

    @Transactional(readOnly = true)
    public List<BillingCycleResponse> findAll() {
        return repository.findAllByActiveTrueOrderByNextBillingDateAsc()
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BillingCycleResponse> findByCustomer(String customerNumber) {
        return repository.findByCustomerNumberAndActiveTrue(customerNumber)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BillingCycleResponse findById(Long id) {
        return toResponse(load(id));
    }

    public BillingCycleResponse create(BillingCycleRequest req) {
        BillingCycle cycle = BillingCycle.builder()
            .customerNumber(req.getCustomerNumber())
            .frequency(req.getFrequency())
            .nextBillingDate(req.getNextBillingDate())
            .description(req.getDescription())
            .contractReference(req.getContractReference())
            .propertyReference(req.getPropertyReference())
            .serviceType(req.getServiceType())
            .active(req.isActive())
            .build();
        return toResponse(repository.save(cycle));
    }

    public BillingCycleResponse update(Long id, BillingCycleRequest req) {
        BillingCycle cycle = load(id);
        cycle.setFrequency(req.getFrequency());
        cycle.setNextBillingDate(req.getNextBillingDate());
        cycle.setDescription(req.getDescription());
        cycle.setContractReference(req.getContractReference());
        cycle.setPropertyReference(req.getPropertyReference());
        cycle.setServiceType(req.getServiceType());
        cycle.setActive(req.isActive());
        return toResponse(repository.save(cycle));
    }

    public List<BillingCycle> findDueCycles(LocalDate runDate) {
        return repository.findCyclesDueInRunWindow(runDate);
    }

    public void advanceNextBillingDate(BillingCycle cycle) {
        LocalDate next = switch (cycle.getFrequency()) {
            case MONTHLY     -> cycle.getNextBillingDate().plusMonths(1);
            case QUARTERLY   -> cycle.getNextBillingDate().plusMonths(3);
            case SEMI_ANNUAL -> cycle.getNextBillingDate().plusMonths(6);
            case ANNUAL      -> cycle.getNextBillingDate().plusYears(1);
        };
        cycle.setNextBillingDate(next);
        repository.save(cycle);
    }

    public LocalDate computePeriodStart(BillingCycle cycle) {
        return switch (cycle.getFrequency()) {
            case MONTHLY     -> cycle.getNextBillingDate().minusMonths(1);
            case QUARTERLY   -> cycle.getNextBillingDate().minusMonths(3);
            case SEMI_ANNUAL -> cycle.getNextBillingDate().minusMonths(6);
            case ANNUAL      -> cycle.getNextBillingDate().minusYears(1);
        };
    }

    private BillingCycle load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingCycle not found: " + id));
    }

    private BillingCycleResponse toResponse(BillingCycle c) {
        return BillingCycleResponse.builder()
            .id(c.getId())
            .customerNumber(c.getCustomerNumber())
            .frequency(c.getFrequency())
            .nextBillingDate(c.getNextBillingDate())
            .description(c.getDescription())
            .contractReference(c.getContractReference())
            .propertyReference(c.getPropertyReference())
            .serviceType(c.getServiceType())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .createdBy(c.getCreatedBy())
            .build();
    }
}
