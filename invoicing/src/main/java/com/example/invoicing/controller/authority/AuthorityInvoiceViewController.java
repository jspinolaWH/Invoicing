package com.example.invoicing.controller.authority;
import com.example.invoicing.entity.invoice.dto.AuthorityLineItemDto;
import com.example.invoicing.entity.invoice.dto.AuthorityInvoiceResponse;
import com.example.invoicing.integration.ExternalAttachmentDto;

import com.example.invoicing.service.InvoiceTransmissionService;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only invoice view for municipal authorities.
 * Requires AUTHORITY_VIEWER role, enforced via @PreAuthorize on each endpoint.
 * Exposes only SENT/COMPLETED invoices with no internal fields.
 */
@RestController
@RequestMapping("/api/v1/authority/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AUTHORITY_VIEWER')")
public class AuthorityInvoiceViewController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTransmissionService transmissionService;

    @GetMapping
    public Page<AuthorityInvoiceResponse> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 25, sort = "invoiceDate") Pageable pageable) {
        return invoiceRepository
            .findForAuthority(customerId, dateFrom, dateTo, pageable)
            .map(this::toAuthorityResponse);
    }

    @GetMapping("/{id}")
    public AuthorityInvoiceResponse getById(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        if (invoice.getStatus() == InvoiceStatus.DRAFT
                || invoice.getStatus() == InvoiceStatus.READY
                || invoice.getStatus() == InvoiceStatus.ERROR) {
            throw new EntityNotFoundException("Invoice not accessible: " + id);
        }
        return toAuthorityResponse(invoice);
    }

    /** STEP-56 — Invoice image retrieval for authority */
    @GetMapping(value = "/{id}/image", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        if (invoice.getExternalReference() == null) {
            byte[] content = invoice.getFinvoiceXml() != null
                ? invoice.getFinvoiceXml().getBytes()
                : ("Invoice " + invoice.getInvoiceNumber()).getBytes();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
        }
        try {
            byte[] imageBytes = transmissionService.fetchImage(id);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(imageBytes);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getMessage().getBytes());
        }
    }

    @GetMapping("/{id}/external-attachments")
    public List<ExternalAttachmentDto> getExternalAttachments(@PathVariable Long id) {
        return transmissionService.fetchExternalAttachments(id);
    }

    private AuthorityInvoiceResponse toAuthorityResponse(Invoice invoice) {
        return AuthorityInvoiceResponse.builder()
            .id(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .invoiceDate(invoice.getInvoiceDate())
            .dueDate(invoice.getDueDate())
            .netAmount(invoice.getNetAmount())
            .grossAmount(invoice.getGrossAmount())
            .vatAmount(invoice.getVatAmount())
            .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
            .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
            .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
            .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
            .lineItems(invoice.getLineItems().stream().map(li ->
                AuthorityLineItemDto.builder()
                    .id(li.getId())
                    .description(li.getDescription())
                    .quantity(li.getQuantity())
                    .unitPrice(li.getUnitPrice())
                    .vatRate(li.getVatRate())
                    .netAmount(li.getNetAmount())
                    .grossAmount(li.getGrossAmount())
                    .legalClassification(li.getLegalClassification() != null
                        ? li.getLegalClassification().name() : null)
                    .build()
            ).collect(Collectors.toList()))
            .build();
    }
}
