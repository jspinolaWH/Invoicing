package com.example.invoicing.controller.vat;

import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.entity.vat.dto.VatRateRequest;
import com.example.invoicing.entity.vat.dto.VatRateResponse;
import com.example.invoicing.service.VatRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vat-rates")
@RequiredArgsConstructor
public class VatRateController {

    private final VatRateService service;

    @GetMapping
    public List<VatRateResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) Boolean active) {

        List<VatRate> rates;
        if (eventDate != null) {
            rates = service.findByEventDate(eventDate);
        } else if (Boolean.TRUE.equals(active)) {
            rates = service.findActive();
        } else {
            rates = service.findAll();
        }
        return rates.stream().map(VatRateResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VatRateResponse getById(@PathVariable Long id) {
        return VatRateResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VatRateResponse create(@RequestBody VatRateRequest request) {
        return VatRateResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public VatRateResponse update(@PathVariable Long id, @RequestBody VatRateRequest request) {
        return VatRateResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
