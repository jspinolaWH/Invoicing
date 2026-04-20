package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEventTemplate;
import com.example.invoicing.entity.billingevent.dto.BillingEventTemplateRequest;
import com.example.invoicing.entity.billingevent.dto.BillingEventTemplateResponse;
import com.example.invoicing.repository.BillingEventTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventTemplateService {

    private final BillingEventTemplateRepository repository;

    public List<BillingEventTemplateResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public BillingEventTemplateResponse getById(Long id) {
        return toResponse(load(id));
    }

    public BillingEventTemplateResponse create(BillingEventTemplateRequest req) {
        BillingEventTemplate t = new BillingEventTemplate();
        applyRequest(t, req);
        return toResponse(repository.save(t));
    }

    public BillingEventTemplateResponse update(Long id, BillingEventTemplateRequest req) {
        BillingEventTemplate t = load(id);
        applyRequest(t, req);
        return toResponse(repository.save(t));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new EntityNotFoundException("Template not found: " + id);
        repository.deleteById(id);
    }

    private BillingEventTemplate load(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));
    }

    private void applyRequest(BillingEventTemplate t, BillingEventTemplateRequest req) {
        t.setName(req.getName());
        t.setCustomerNumber(req.getCustomerNumber());
        t.setContractId(req.getContractId());
        t.setProductId(req.getProductId());
        t.setWasteFeePrice(req.getWasteFeePrice());
        t.setTransportFeePrice(req.getTransportFeePrice());
        t.setEcoFeePrice(req.getEcoFeePrice());
        t.setQuantity(req.getQuantity());
        t.setWeight(req.getWeight());
        t.setVehicleId(req.getVehicleId());
        t.setDriverId(req.getDriverId());
        t.setLocationId(req.getLocationId());
        t.setMunicipalityId(req.getMunicipalityId());
        t.setComments(req.getComments());
        t.setContractor(req.getContractor());
        t.setDirection(req.getDirection());
        t.setSharedServiceGroupPercentage(req.getSharedServiceGroupPercentage());
        t.setWasteType(req.getWasteType());
        t.setReceivingSite(req.getReceivingSite());
    }

    private BillingEventTemplateResponse toResponse(BillingEventTemplate t) {
        return BillingEventTemplateResponse.builder()
            .id(t.getId())
            .name(t.getName())
            .customerNumber(t.getCustomerNumber())
            .contractId(t.getContractId())
            .productId(t.getProductId())
            .wasteFeePrice(t.getWasteFeePrice())
            .transportFeePrice(t.getTransportFeePrice())
            .ecoFeePrice(t.getEcoFeePrice())
            .quantity(t.getQuantity())
            .weight(t.getWeight())
            .vehicleId(t.getVehicleId())
            .driverId(t.getDriverId())
            .locationId(t.getLocationId())
            .municipalityId(t.getMunicipalityId())
            .comments(t.getComments())
            .contractor(t.getContractor())
            .direction(t.getDirection())
            .sharedServiceGroupPercentage(t.getSharedServiceGroupPercentage())
            .wasteType(t.getWasteType())
            .receivingSite(t.getReceivingSite())
            .createdBy(t.getCreatedBy())
            .createdAt(t.getCreatedAt())
            .build();
    }
}
