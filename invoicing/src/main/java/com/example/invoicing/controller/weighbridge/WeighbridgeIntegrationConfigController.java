package com.example.invoicing.controller.weighbridge;

import com.example.invoicing.entity.weighbridge.dto.WeighbridgeConfigRequest;
import com.example.invoicing.entity.weighbridge.dto.WeighbridgeConfigResponse;
import com.example.invoicing.service.WeighbridgeIntegrationConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weighbridge-configs")
@RequiredArgsConstructor
public class WeighbridgeIntegrationConfigController {

    private final WeighbridgeIntegrationConfigService service;

    @GetMapping
    public ResponseEntity<List<WeighbridgeConfigResponse>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/by-customer/{customerNumber}")
    public ResponseEntity<WeighbridgeConfigResponse> getByCustomer(@PathVariable String customerNumber) {
        return service.findByCustomerNumber(customerNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WeighbridgeConfigResponse> upsert(@RequestBody @Valid WeighbridgeConfigRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
