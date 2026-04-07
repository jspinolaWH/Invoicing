package com.example.invoicing.simulation;

import com.example.invoicing.generation.SimulationReport;
import com.example.invoicing.run.dto.InvoiceRunRequest;
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
