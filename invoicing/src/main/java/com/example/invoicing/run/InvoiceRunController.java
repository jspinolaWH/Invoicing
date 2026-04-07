package com.example.invoicing.run;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.run.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoice-runs")
@RequiredArgsConstructor
public class InvoiceRunController {

    private final InvoiceRunService invoiceRunService;
    private final InvoiceRunOrchestratorService orchestratorService;
    private final com.example.invoicing.cancellation.InvoiceCancellationService cancellationService;

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
    public com.example.invoicing.cancellation.CancellationResult cancelRun(
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
}
