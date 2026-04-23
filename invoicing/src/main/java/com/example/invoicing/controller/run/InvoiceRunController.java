package com.example.invoicing.controller.run;
import com.example.invoicing.entity.invoice.dto.BatchAttachmentRequest;
import com.example.invoicing.entity.invoicerun.dto.CancellationResult;
import com.example.invoicing.service.InvoiceCancellationService;
import com.example.invoicing.service.InvoiceRunOrchestratorService;
import com.example.invoicing.service.InvoiceRunService;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.invoicerun.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoice-runs")
@RequiredArgsConstructor
public class InvoiceRunController {

    private final InvoiceRunService invoiceRunService;
    private final InvoiceRunOrchestratorService orchestratorService;
    private final com.example.invoicing.service.InvoiceCancellationService cancellationService;

    @GetMapping
    public Page<InvoiceRunResponse> listRuns(
            @RequestParam(required = false) InvoiceRunStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceRunService.findAll(status, PageRequest.of(page, size));
    }

    @PostMapping
    public ResponseEntity<InvoiceRunResponse> createRun(@RequestBody InvoiceRunRequest request) {
        InvoiceRun run = invoiceRunService.create(request);
        orchestratorService.executeRun(run.getId());
        return ResponseEntity.accepted().body(InvoiceRunResponse.from(run));
    }

    @GetMapping("/{id}")
    public InvoiceRunResponse getRun(@PathVariable Long id) {
        return invoiceRunService.findById(id);
    }

    @PostMapping("/{id}/cancel")
    public com.example.invoicing.entity.invoicerun.dto.CancellationResult cancelRun(
            @PathVariable Long id,
            @RequestBody CancelRunRequest request) {
        return cancellationService.cancelRun(id, request.getReason(), "SYSTEM");
    }

    @PostMapping("/{id}/schedule-send")
    public InvoiceRunResponse scheduleSend(@PathVariable Long id,
                                            @RequestBody ScheduleSendRequest request) {
        return invoiceRunService.scheduleSend(id, request.getSendAt());
    }

    @PostMapping("/{id}/send")
    public InvoiceRunResponse send(@PathVariable Long id) {
        return invoiceRunService.triggerSend(id);
    }

    @PostMapping("/{id}/batch-attachment")
    public InvoiceRunResponse setBatchAttachment(@PathVariable Long id,
                                                  @RequestBody BatchAttachmentRequest request) {
        return invoiceRunService.setBatchAttachment(id, request);
    }

    @GetMapping("/verify-attachment")
    public ResponseEntity<java.util.Map<String, Object>> verifyAttachmentIdentifier(
            @RequestParam String identifier) {
        boolean valid = invoiceRunService.verifyAttachmentIdentifier(identifier);
        return ResponseEntity.ok(java.util.Map.of("identifier", identifier, "valid", valid));
    }
}
