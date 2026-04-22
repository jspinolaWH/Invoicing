package com.example.invoicing.controller.invoicetemplate;

import com.example.invoicing.entity.invoicetemplate.dto.InvoiceTemplateRequest;
import com.example.invoicing.entity.invoicetemplate.dto.InvoiceTemplateResponse;
import com.example.invoicing.service.InvoiceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoice-templates")
@RequiredArgsConstructor
public class InvoiceTemplateController {

    private final InvoiceTemplateService service;

    @GetMapping
    public List<InvoiceTemplateResponse> getAll() {
        return service.findAll().stream().map(InvoiceTemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InvoiceTemplateResponse getById(@PathVariable Long id) {
        return InvoiceTemplateResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceTemplateResponse create(@RequestBody InvoiceTemplateRequest request) {
        return InvoiceTemplateResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public InvoiceTemplateResponse update(@PathVariable Long id,
                                          @RequestBody InvoiceTemplateRequest request) {
        return InvoiceTemplateResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
