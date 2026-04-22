package com.example.invoicing.service;

import com.example.invoicing.entity.pricelist.PriceList;
import com.example.invoicing.entity.pricelist.dto.PriceListRequest;
import com.example.invoicing.repository.PriceListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceListService {

    private final PriceListRepository repository;

    public List<PriceList> findAll() {
        return repository.findAll();
    }

    public List<PriceList> findActive() {
        return repository.findByActiveTrue();
    }

    public PriceList findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Price list not found: " + id));
    }

    @Transactional
    public PriceList create(PriceListRequest request) {
        PriceList p = new PriceList();
        apply(p, request);
        return repository.save(p);
    }

    @Transactional
    public PriceList update(Long id, PriceListRequest request) {
        PriceList p = findById(id);
        apply(p, request);
        return repository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private void apply(PriceList p, PriceListRequest req) {
        p.setCode(req.getCode().toUpperCase().trim());
        p.setName(req.getName().trim());
        p.setTariffVariant(req.getTariffVariant());
        p.setValidFrom(req.getValidFrom());
        p.setValidTo(req.getValidTo());
        p.setDescription(req.getDescription());
        p.setActive(req.isActive());
    }
}
