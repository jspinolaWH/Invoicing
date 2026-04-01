package com.example.invoicing.service;

import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.costcenter.dto.CostCenterRequest;
import com.example.invoicing.repository.CostCenterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostCenterService {

    private final CostCenterRepository repository;

    public List<CostCenter> findAll() {
        return repository.findAll();
    }

    public List<CostCenter> findByProductSegment(String productSegment) {
        return repository.findByProductSegment(productSegment);
    }

    public List<CostCenter> findByResponsibilitySegment(String responsibilitySegment) {
        return repository.findByResponsibilitySegment(responsibilitySegment);
    }

    public CostCenter findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + id));
    }

    @Transactional
    public CostCenter create(CostCenterRequest request) {
        CostCenter cc = new CostCenter();
        applyRequest(cc, request);
        return repository.save(cc);
    }

    @Transactional
    public CostCenter update(Long id, CostCenterRequest request) {
        CostCenter cc = findById(id);
        applyRequest(cc, request);
        return repository.save(cc);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private void applyRequest(CostCenter cc, CostCenterRequest request) {
        cc.setProductSegment(request.getProductSegment().toUpperCase().trim());
        cc.setReceptionSegment(request.getReceptionSegment().toUpperCase().trim());
        cc.setResponsibilitySegment(request.getResponsibilitySegment().toUpperCase().trim());
        cc.setCompositeCode(deriveCompositeCode(request));
        cc.setDescription(request.getDescription());
    }

    private String deriveCompositeCode(CostCenterRequest r) {
        return r.getProductSegment().toUpperCase().trim()
                + "-" + r.getReceptionSegment().toUpperCase().trim()
                + "-" + r.getResponsibilitySegment().toUpperCase().trim();
    }
}
