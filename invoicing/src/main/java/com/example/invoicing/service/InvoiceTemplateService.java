package com.example.invoicing.service;

import com.example.invoicing.entity.invoicetemplate.InvoiceTemplate;
import com.example.invoicing.entity.invoicetemplate.dto.InvoiceTemplateRequest;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.repository.InvoiceTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository repository;
    private final InvoiceNumberSeriesRepository seriesRepository;

    public List<InvoiceTemplate> findAll() {
        return repository.findAll();
    }

    public InvoiceTemplate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("InvoiceTemplate not found: " + id));
    }

    @Transactional
    public InvoiceTemplate create(InvoiceTemplateRequest request) {
        InvoiceTemplate t = new InvoiceTemplate();
        applyRequest(t, request);
        return repository.save(t);
    }

    @Transactional
    public InvoiceTemplate update(Long id, InvoiceTemplateRequest request) {
        InvoiceTemplate t = findById(id);
        applyRequest(t, request);
        return repository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        InvoiceTemplate t = findById(id);
        repository.delete(t);
    }

    private void applyRequest(InvoiceTemplate t, InvoiceTemplateRequest request) {
        t.setName(request.getName().trim());
        t.setCode(request.getCode().trim().toUpperCase());
        if (request.getNumberSeriesId() != null) {
            InvoiceNumberSeries series = seriesRepository.findById(request.getNumberSeriesId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "InvoiceNumberSeries not found: " + request.getNumberSeriesId()));
            t.setNumberSeries(series);
        } else {
            t.setNumberSeries(null);
        }
    }
}
