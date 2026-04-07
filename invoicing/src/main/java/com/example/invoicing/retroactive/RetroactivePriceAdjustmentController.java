package com.example.invoicing.retroactive;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/retroactive/price-adjustment")
@RequiredArgsConstructor
public class RetroactivePriceAdjustmentController {

    private final RetroactivePriceAdjustmentService service;

    @PostMapping("/preview")
    public PriceAdjustmentPreview preview(@RequestBody PriceAdjustmentRequest request) {
        return service.preview(request);
    }

    @PostMapping("/apply")
    public ResponseEntity<PriceAdjustmentResult> apply(@RequestBody PriceAdjustmentRequest request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
