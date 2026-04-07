package com.example.invoicing.controller.run;
import com.example.invoicing.service.InvoiceSimulationService;

import com.example.invoicing.entity.invoice.dto.SimulationReport;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoice-runs")
@RequiredArgsConstructor
public class SimulationController {

    private final InvoiceSimulationService simulationService;

    @PostMapping("/simulate")
    public SimulationReport simulate(@RequestBody InvoiceRunRequest request) {
        return simulationService.simulate(request);
    }
}
