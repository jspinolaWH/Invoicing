package com.example.invoicing.controller.billingevent;

import com.example.invoicing.entity.billingevent.dto.BillingEventTemplateRequest;
import com.example.invoicing.entity.billingevent.dto.BillingEventTemplateResponse;
import com.example.invoicing.service.BillingEventTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-event-templates")
@RequiredArgsConstructor
public class BillingEventTemplateController {

    private final BillingEventTemplateService service;

    @GetMapping
    public List<BillingEventTemplateResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public BillingEventTemplateResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<BillingEventTemplateResponse> create(
            @Valid @RequestBody BillingEventTemplateRequest req) {
        return ResponseEntity.status(201).body(service.create(req));
    }

    @PutMapping("/{id}")
    public BillingEventTemplateResponse update(
            @PathVariable Long id, @Valid @RequestBody BillingEventTemplateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
