package com.example.invoicing.controller.dev;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.repository.BillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevSimulationController {

    private final BillingEventRepository billingEventRepository;

    @PostMapping("/billing-events/{id}/simulate-transmission")
    public ResponseEntity<Void> simulateTransmission(
            @PathVariable Long id,
            @RequestBody SimulateRequest request) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        event.setStatus(request.getOutcome());
        if (request.getOutcome() == BillingEventStatus.ERROR) {
            event.setTransmissionErrorReason(request.getErrorReason());
        } else {
            event.setTransmissionErrorReason(null);
        }
        billingEventRepository.save(event);
        return ResponseEntity.ok().build();
    }

    @Data
    static class SimulateRequest {
        private BillingEventStatus outcome;
        private String errorReason;
    }
}
