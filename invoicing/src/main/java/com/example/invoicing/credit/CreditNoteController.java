package com.example.invoicing.credit;

import com.example.invoicing.credit.dto.CreditNoteRequest;
import com.example.invoicing.credit.dto.CreditNoteResponse;
import com.example.invoicing.invoice.InvoiceService;
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
    public Page<com.example.invoicing.invoice.dto.InvoiceResponse> getCreditHistory(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.findCreditNotesByCustomerId(customerId, PageRequest.of(page, size));
    }
}
