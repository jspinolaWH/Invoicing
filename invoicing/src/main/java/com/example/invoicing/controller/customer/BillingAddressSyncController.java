package com.example.invoicing.controller.customer;
import com.example.invoicing.entity.customer.dto.BillingAddressSyncResult;
import com.example.invoicing.entity.customer.dto.BillingAddressSyncRequest;
import com.example.invoicing.service.BillingAddressSyncService;

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
