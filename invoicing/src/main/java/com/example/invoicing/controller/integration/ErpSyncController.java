package com.example.invoicing.controller.integration;

import com.example.invoicing.service.ErpSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/integration/erp")
@RequiredArgsConstructor
public class ErpSyncController {

    private final ErpSyncService erpSyncService;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/export")
    public List<ErpSyncService.ErpLineItem> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return erpSyncService.exportLineItems(from, to);
    }
}
