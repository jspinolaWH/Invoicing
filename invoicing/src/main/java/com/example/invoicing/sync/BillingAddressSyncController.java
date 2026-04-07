package com.example.invoicing.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class BillingAddressSyncController {

    private final BillingAddressSyncService syncService;

    @PostMapping("/billing-address")
    public BillingAddressSyncResult syncBillingAddress(@RequestBody BillingAddressSyncRequest request) {
        return syncService.syncFromWasteHero(request);
    }
}
