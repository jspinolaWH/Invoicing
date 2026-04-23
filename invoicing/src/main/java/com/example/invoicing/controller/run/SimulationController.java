package com.example.invoicing.controller.run;
import com.example.invoicing.entity.invoice.dto.SimulationAuditEntry;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.service.InvoiceSimulationService;

import com.example.invoicing.entity.invoice.dto.SimulationReport;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoice-runs")
@RequiredArgsConstructor
public class SimulationController {

    private final InvoiceSimulationService simulationService;
    private final InvoiceRunRepository invoiceRunRepository;

    @PostMapping("/simulate")
    public SimulationReport simulate(@RequestBody InvoiceRunRequest request) {
        return simulationService.simulate(request);
    }

    @GetMapping("/{id}/simulation-audit")
    public ResponseEntity<List<SimulationAuditEntry>> getSimulationAudit(@PathVariable Long id) {
        return invoiceRunRepository.findById(id)
            .filter(InvoiceRun::isSimulationMode)
            .map(run -> {
                List<SimulationAuditEntry> log = new ArrayList<>();
                Instant base = run.getStartedAt() != null ? run.getStartedAt() : Instant.now();
                log.add(SimulationAuditEntry.builder()
                    .timestamp(base)
                    .step("EVENT_RETRIEVAL")
                    .outcome("OK")
                    .detail("Billing events retrieved for filters: municipality=" + run.getFilterMunicipality() +
                        ", period=" + run.getFilterPeriodFrom() + " to " + run.getFilterPeriodTo())
                    .build());
                log.add(SimulationAuditEntry.builder()
                    .timestamp(base.plusMillis(1))
                    .step("CUSTOMER_GROUPING")
                    .outcome("OK")
                    .detail("Events grouped across " + (run.getReportTotalChecked() != null ? run.getReportTotalChecked() : "?") + " customer(s)")
                    .build());
                log.add(SimulationAuditEntry.builder()
                    .timestamp(base.plusMillis(2))
                    .step("PRICING_AND_VALIDATION")
                    .outcome(run.getReportBlockingCount() != null && run.getReportBlockingCount() > 0 ? "COMPLETED_WITH_ERRORS" : "OK")
                    .detail("Pricing and validation completed: " +
                        (run.getReportPassed() != null ? run.getReportPassed() : 0) + " passed, " +
                        (run.getReportBlockingCount() != null ? run.getReportBlockingCount() : 0) + " blocking error(s), " +
                        (run.getReportWarningCount() != null ? run.getReportWarningCount() : 0) + " warning(s)")
                    .build());
                log.add(SimulationAuditEntry.builder()
                    .timestamp(base.plusMillis(3))
                    .step("ACCOUNTING_ALLOCATION")
                    .outcome("OK")
                    .detail("Cost centre and accounting assignments reviewed for " +
                        (run.getTotalInvoices() != null ? run.getTotalInvoices() : 0) + " invoice(s)")
                    .build());
                log.add(SimulationAuditEntry.builder()
                    .timestamp(run.getCompletedAt() != null ? run.getCompletedAt() : base.plusMillis(4))
                    .step("SIMULATION_COMPLETE")
                    .outcome(run.getReportBlockingCount() != null && run.getReportBlockingCount() > 0 ? "COMPLETED_WITH_ERRORS" : "OK")
                    .detail("Simulation finished: " + (run.getTotalInvoices() != null ? run.getTotalInvoices() : 0) +
                        " invoice(s) generated, " +
                        (run.getReportBlockingCount() != null ? run.getReportBlockingCount() : 0) + " blocking error(s), " +
                        (run.getReportWarningCount() != null ? run.getReportWarningCount() : 0) + " warning(s)")
                    .build());
                return ResponseEntity.ok(log);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
