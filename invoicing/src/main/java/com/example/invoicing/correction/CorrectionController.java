package com.example.invoicing.correction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
