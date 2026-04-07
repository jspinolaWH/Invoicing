package com.example.invoicing.controller.invoice;
import com.example.invoicing.entity.invoice.dto.InvoiceResponse;
import com.example.invoicing.entity.invoice.dto.BatchCreditResult;
import com.example.invoicing.entity.invoice.dto.BatchCreditRequest;
import com.example.invoicing.service.CreditNoteService;

import com.example.invoicing.entity.invoice.dto.CreditNoteRequest;
import com.example.invoicing.entity.invoice.dto.CreditNoteResponse;
import com.example.invoicing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    private final InvoiceService invoiceService;

    @PostMapping("/api/v1/invoices/{id}/credit")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditNoteResponse issueCreditNote(@PathVariable Long id,
                                               @RequestBody CreditNoteRequest request) {
        return creditNoteService.credit(id, request);
    }

    @PostMapping("/api/v1/invoices/batch-credit")
    public BatchCreditResult batchCredit(@RequestBody BatchCreditRequest request) {
        return creditNoteService.batchCredit(request);
    }

    @GetMapping("/api/v1/customers/{customerId}/credit-history")
    public Page<com.example.invoicing.entity.invoice.dto.InvoiceResponse> getCreditHistory(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.findCreditNotesByCustomerId(customerId, PageRequest.of(page, size));
    }
}
