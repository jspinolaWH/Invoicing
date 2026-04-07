package com.example.invoicing.service;

import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.entity.numberseries.dto.InvoiceNumberSeriesRequest;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
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
        // Counter is never updated via the API — only assignNextNumber increments it
        s.setName(request.getName().trim());
        s.setPrefix(request.getPrefix().trim().toUpperCase());
        s.setFormatPattern(request.getFormatPattern().trim());
        return repository.save(s);
    }

    /**
     * Atomically assigns the next invoice number.
     * Uses a pessimistic write lock so concurrent calls cannot get the same counter value.
     * In simulation mode the counter is never incremented — returns a preview placeholder.
     */
    @Transactional
    public String assignNextNumber(Long seriesId, boolean simulationMode) {
        if (simulationMode) {
            // Per 06-cross-cutting.md: never increment real counter in simulation
            return "SIMULATION-PREVIEW";
        }

        InvoiceNumberSeries series = repository.findByIdForUpdate(seriesId)
                .orElseThrow(() -> new EntityNotFoundException("InvoiceNumberSeries not found: " + seriesId));

        long next = series.getCurrentCounter() + 1;
        series.setCurrentCounter(next);
        repository.save(series);

        return formatNumber(series, next);
    }

    /**
     * Records a released number (e.g. cancelled invoice).
     * Released numbers are never re-assigned — kept for auditing only.
     */
    @Transactional
    public void releaseNumber(Long seriesId, String number) {
        InvoiceNumberSeries series = findById(seriesId);
        series.getReleasedNumbers().add(number);
        repository.save(series);
    }

    private void applyRequest(InvoiceNumberSeries s, InvoiceNumberSeriesRequest request) {
        s.setName(request.getName().trim());
        s.setPrefix(request.getPrefix().trim().toUpperCase());
        s.setFormatPattern(request.getFormatPattern().trim());
    }

    private String formatNumber(InvoiceNumberSeries series, long counter) {
        return series.getFormatPattern()
                .replace("{PREFIX}", series.getPrefix())
                .replace("{YEAR}", String.valueOf(LocalDate.now().getYear()))
                .replace("{COUNTER:06d}", String.format("%06d", counter));
    }
}
