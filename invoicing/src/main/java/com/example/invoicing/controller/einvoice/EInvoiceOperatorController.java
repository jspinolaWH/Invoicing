package com.example.invoicing.controller.einvoice;
import com.example.invoicing.service.dto.OperatorBatchResult;
import com.example.invoicing.service.EInvoiceOperatorService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/einvoice-operator")
@RequiredArgsConstructor
public class EInvoiceOperatorController {

    private final EInvoiceOperatorService operatorService;

    @PostMapping("/trigger")
    public OperatorBatchResult triggerBatch() {
        return operatorService.triggerManually();
    }
}
