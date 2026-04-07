package com.example.invoicing.invoice;

import com.example.invoicing.invoice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public List<InvoiceResponse> getByRunId(@RequestParam Long runId) {
        return service.findByRunId(runId);
    }

    @PatchMapping("/{id}/text")
    public InvoiceResponse updateText(@PathVariable Long id,
                                      @RequestBody UpdateCustomTextRequest request) {
        return service.updateCustomText(id, request.getCustomText());
    }

    @PostMapping("/bulk-text")
    public ResponseEntity<Void> bulkText(@RequestBody BulkCustomTextRequest request) {
        service.bulkUpdateCustomText(request.getInvoiceIds(), request.getCustomText());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/remove-surcharge")
    public InvoiceResponse removeSurcharge(@PathVariable Long id) {
        return service.removeSurcharge(id);
    }
}
