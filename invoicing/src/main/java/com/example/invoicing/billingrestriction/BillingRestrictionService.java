package com.example.invoicing.billingrestriction;

import com.example.invoicing.billingrestriction.dto.BillingRestrictionRequest;
import com.example.invoicing.billingrestriction.dto.BillingRestrictionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingRestrictionService {

    private final BillingRestrictionRepository repository;

    @Transactional(readOnly = true)
    public List<BillingRestrictionResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BillingRestrictionResponse findById(Long id) {
        return toResponse(load(id));
    }

    public BillingRestrictionResponse create(BillingRestrictionRequest req) {
        BillingRestriction r = BillingRestriction.builder()
            .municipality(req.getMunicipality())
            .customerType(req.getCustomerType())
            .serviceType(req.getServiceType())
            .locationId(req.getLocationId())
            .minAmount(req.getMinAmount())
            .period(req.getPeriod())
            .serviceResponsibility(req.getServiceResponsibility())
            .billingType(req.getBillingType())
            .description(req.getDescription())
            .active(req.isActive())
            .build();
        return toResponse(repository.save(r));
    }

    public BillingRestrictionResponse update(Long id, BillingRestrictionRequest req) {
        BillingRestriction r = load(id);
        r.setMunicipality(req.getMunicipality());
        r.setCustomerType(req.getCustomerType());
        r.setServiceType(req.getServiceType());
        r.setLocationId(req.getLocationId());
        r.setMinAmount(req.getMinAmount());
        r.setPeriod(req.getPeriod());
        r.setServiceResponsibility(req.getServiceResponsibility());
        r.setBillingType(req.getBillingType());
        r.setDescription(req.getDescription());
        r.setActive(req.isActive());
        return toResponse(repository.save(r));
    }

    public void delete(Long id) {
        BillingRestriction r = load(id);
        r.setActive(false);
        repository.save(r);
    }

    public boolean isImmediateService(String serviceType) {
        return !repository.findByServiceTypeAndActiveTrue(serviceType).stream()
            .filter(r -> r.getBillingType() == BillingType.IMMEDIATE)
            .toList().isEmpty();
    }

    public List<BillingRestriction> getActiveFilterRestrictions() {
        return repository.findByActiveTrue();
    }

    private BillingRestriction load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingRestriction not found: " + id));
    }

    private BillingRestrictionResponse toResponse(BillingRestriction r) {
        return BillingRestrictionResponse.builder()
            .id(r.getId())
            .municipality(r.getMunicipality())
            .customerType(r.getCustomerType())
            .serviceType(r.getServiceType())
            .locationId(r.getLocationId())
            .minAmount(r.getMinAmount())
            .period(r.getPeriod())
            .serviceResponsibility(r.getServiceResponsibility())
            .billingType(r.getBillingType())
            .description(r.getDescription())
            .active(r.isActive())
            .createdAt(r.getCreatedAt())
            .createdBy(r.getCreatedBy())
            .build();
    }
}
