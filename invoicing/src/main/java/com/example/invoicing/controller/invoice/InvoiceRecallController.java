package com.example.invoicing.controller.invoice;
import com.example.invoicing.entity.invoice.dto.RecallResult;
import com.example.invoicing.entity.invoice.dto.RecallRequest;
import com.example.invoicing.service.InvoiceRecallService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceRecallController {

    private final InvoiceRecallService recallService;

    @PostMapping("/{id}/recall")
    public ResponseEntity<RecallResult> recall(
            @PathVariable Long id,
            @RequestBody RecallRequest request) {
        return ResponseEntity.ok(recallService.recall(id, request));
    }
}
