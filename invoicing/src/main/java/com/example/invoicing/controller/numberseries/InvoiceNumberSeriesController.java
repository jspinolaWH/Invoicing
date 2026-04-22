package com.example.invoicing.controller.numberseries;

import com.example.invoicing.entity.numberseries.dto.AssignNumberResponse;
import com.example.invoicing.entity.numberseries.dto.InvoiceNumberSeriesRequest;
import com.example.invoicing.entity.numberseries.dto.InvoiceNumberSeriesResponse;
import com.example.invoicing.service.InvoiceNumberSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoice-number-series")
@RequiredArgsConstructor
public class InvoiceNumberSeriesController {

    private final InvoiceNumberSeriesService service;

    @GetMapping
    public List<InvoiceNumberSeriesResponse> getAll() {
        return service.findAll().stream().map(InvoiceNumberSeriesResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InvoiceNumberSeriesResponse getById(@PathVariable Long id) {
        return InvoiceNumberSeriesResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceNumberSeriesResponse create(@RequestBody InvoiceNumberSeriesRequest request) {
        return InvoiceNumberSeriesResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public InvoiceNumberSeriesResponse update(
            @PathVariable Long id,
            @RequestBody InvoiceNumberSeriesRequest request) {
        return InvoiceNumberSeriesResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/assign")
    public AssignNumberResponse assign(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean simulation) {
        String assigned = service.assignNextNumber(id, simulation);
        return new AssignNumberResponse(assigned, id, simulation);
    }
}
