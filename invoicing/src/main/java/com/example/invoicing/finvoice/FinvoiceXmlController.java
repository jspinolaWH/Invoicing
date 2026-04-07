package com.example.invoicing.finvoice;

import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class FinvoiceXmlController {

    private final InvoiceRepository invoiceRepository;
    private final FinvoiceBuilderService finvoiceBuilderService;
    private final SellerDetails sellerDetails;

    @GetMapping(value = "/{id}/finvoice-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getFinvoiceXml(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));

        // Return stored XML if present; otherwise generate on-the-fly
        if (invoice.getFinvoiceXml() != null && !invoice.getFinvoiceXml().isBlank()) {
            return ResponseEntity.ok(invoice.getFinvoiceXml());
        }

        String xml = finvoiceBuilderService.build(invoice);
        return ResponseEntity.ok(xml);
    }
}
