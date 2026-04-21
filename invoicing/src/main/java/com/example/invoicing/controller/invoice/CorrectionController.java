package com.example.invoicing.controller.invoice;
import com.example.invoicing.entity.invoice.dto.BillingEventForCorrectionDto;
import com.example.invoicing.entity.invoice.dto.CorrectionResult;
import com.example.invoicing.entity.invoice.dto.CorrectionRequest;
import com.example.invoicing.entity.invoice.dto.CopyRequest;
import com.example.invoicing.service.BilledEventCorrectionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CorrectionController {

    private final BilledEventCorrectionService correctionService;

    @PostMapping("/api/v1/invoices/{id}/correct")
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectionResult correct(@PathVariable Long id,
                                     @RequestBody CorrectionRequest request) {
        return correctionService.correct(id, request);
    }

    @PostMapping("/api/v1/invoices/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectionResult copy(@PathVariable Long id,
                                  @RequestBody CopyRequest request) {
        return correctionService.copy(id, request);
    }

    @GetMapping("/api/v1/invoices/{id}/billing-events")
    public List<BillingEventForCorrectionDto> getBillingEvents(@PathVariable Long id) {
        return correctionService.getBillingEventsForInvoice(id);
    }
}
