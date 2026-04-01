package com.example.invoicing.service;

import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.entity.vat.dto.VatRateRequest;
import com.example.invoicing.repository.VatRateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VatRateService {

    private final VatRateRepository repository;

    public List<VatRate> findAll() {
        return repository.findAll();
    }

    public List<VatRate> findByEventDate(LocalDate eventDate) {
        return repository.findByEventDate(eventDate);
    }

    public List<VatRate> findActive() {
        return repository.findActive(LocalDate.now());
    }

    public VatRate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VatRate not found: " + id));
    }

    @Transactional
    public VatRate create(VatRateRequest request) {
        VatRate vatRate = new VatRate();
        vatRate.setCode(request.getCode());
        vatRate.setRate(request.getRate());
        vatRate.setValidFrom(request.getValidFrom());
        vatRate.setValidTo(request.getValidTo());
        return repository.save(vatRate);
    }

    @Transactional
    public VatRate update(Long id, VatRateRequest request) {
        VatRate vatRate = findById(id);
        vatRate.setCode(request.getCode());
        vatRate.setRate(request.getRate());
        vatRate.setValidFrom(request.getValidFrom());
        vatRate.setValidTo(request.getValidTo());
        return repository.save(vatRate);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
