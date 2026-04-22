package com.example.invoicing.service;

import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.entity.numberseries.dto.InvoiceNumberSeriesRequest;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceNumberSeriesService {

    private final InvoiceNumberSeriesRepository repository;
    private final InvoiceRepository invoiceRepository;

    public List<InvoiceNumberSeries> findAll() {
        return repository.findAll();
    }

    public InvoiceNumberSeries findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("InvoiceNumberSeries not found: " + id));
    }

    @Transactional
    public InvoiceNumberSeries create(InvoiceNumberSeriesRequest request) {
        InvoiceNumberSeries s = new InvoiceNumberSeries();
        applyRequest(s, request);
        s.setCurrentCounter(0L);
        return repository.save(s);
    }

    @Transactional
    public InvoiceNumberSeries update(Long id, InvoiceNumberSeriesRequest request) {
        InvoiceNumberSeries s = findById(id);
        applyRequest(s, request);
        return repository.save(s);
    }

    @Transactional
    public void delete(Long id) {
        InvoiceNumberSeries series = findById(id);
        if (invoiceRepository.existsByInvoiceNumberSeriesId(id)) {
            throw new IllegalStateException(
                "Cannot delete series '" + series.getName() + "' because it has been used on one or more invoices");
        }
        repository.delete(series);
    }

    @Transactional
    public String assignNextNumber(Long seriesId, boolean simulationMode) {
        if (simulationMode) {
            return "SIMULATION-PREVIEW";
        }

        InvoiceNumberSeries series = repository.findByIdForUpdate(seriesId)
                .orElseThrow(() -> new EntityNotFoundException("InvoiceNumberSeries not found: " + seriesId));

        if (!series.getReleasedNumbers().isEmpty()) {
            String reused = series.getReleasedNumbers().remove(series.getReleasedNumbers().size() - 1);
            repository.save(series);
            return reused;
        }

        long next = series.getCurrentCounter() + 1;
        series.setCurrentCounter(next);
        repository.save(series);

        return formatNumber(series, next);
    }

    @Transactional
    public void releaseNumber(Long seriesId, String number) {
        InvoiceNumberSeries series = findById(seriesId);
        series.getReleasedNumbers().add(number);
        repository.save(series);
    }

    private void applyRequest(InvoiceNumberSeries s, InvoiceNumberSeriesRequest request) {
        validateFormatPattern(request.getFormatPattern());
        s.setName(request.getName().trim());
        s.setPrefix(request.getPrefix().trim().toUpperCase());
        s.setFormatPattern(request.getFormatPattern().trim());
        s.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
    }

    private void validateFormatPattern(String pattern) {
        if (pattern == null || !pattern.contains("{COUNTER:06d}")) {
            throw new IllegalArgumentException(
                "Format pattern must contain the token {COUNTER:06d} to ensure unique counter-based numbering");
        }
    }

    private String formatNumber(InvoiceNumberSeries series, long counter) {
        return series.getFormatPattern()
                .replace("{PREFIX}", series.getPrefix())
                .replace("{YEAR}", String.valueOf(LocalDate.now().getYear()))
                .replace("{COUNTER:06d}", String.format("%06d", counter));
    }
}
