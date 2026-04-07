package com.example.invoicing.invoice;

import com.example.invoicing.invoice.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository repository;

    public InvoiceResponse findById(Long id) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        return toResponse(invoice);
    }

    public List<InvoiceResponse> findByRunId(Long runId) {
        return repository.findByInvoiceRunId(runId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse updateCustomText(Long id, String customText) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        invoice.setCustomText(customText);
        return toResponse(repository.save(invoice));
    }

    @Transactional
    public void bulkUpdateCustomText(List<Long> ids, String customText) {
        for (Long id : ids) {
            repository.findById(id).ifPresent(inv -> {
                inv.setCustomText(customText);
                repository.save(inv);
            });
        }
    }

    @Transactional
    public InvoiceResponse removeSurcharge(Long id) {
        Invoice invoice = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        invoice.getLineItems().removeIf(li -> li.getDescription() != null
            && li.getDescription().startsWith("Invoicing surcharge"));
        return toResponse(repository.save(invoice));
    }

    public List<Invoice> findOpenByCustomerId(Long customerId) {
        return repository.findOpenByCustomerId(customerId);
    }

    public Page<InvoiceResponse> findSentOrCompletedByCustomerId(Long customerId, Pageable pageable) {
        return repository.findSentOrCompletedByCustomerId(customerId, pageable).map(this::toResponse);
    }

    public Page<InvoiceResponse> findCreditNotesByCustomerId(Long customerId, Pageable pageable) {
        return repository.findCreditNotesByCustomerId(customerId, pageable).map(this::toResponse);
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineItemResponse> lineItems = invoice.getLineItems().stream()
            .map(li -> InvoiceLineItemResponse.builder()
                .id(li.getId())
                .description(li.getDescription())
                .quantity(li.getQuantity())
                .unitPrice(li.getUnitPrice())
                .vatRate(li.getVatRate())
                .netAmount(li.getNetAmount())
                .grossAmount(li.getGrossAmount())
                .legalClassification(li.getLegalClassification() != null ? li.getLegalClassification().name() : null)
                .accountingAccountCode(li.getAccountingAccount() != null ? li.getAccountingAccount().getCode() : null)
                .costCenterCode(li.getCostCenter() != null ? li.getCostCenter().getCompositeCode() : null)
                .bundled(li.isBundled())
                .lineOrder(li.getLineOrder())
                .build())
            .collect(Collectors.toList());

        List<InvoiceAttachmentResponse> attachments = invoice.getAttachments().stream()
            .map(a -> InvoiceAttachmentResponse.builder()
                .id(a.getId())
                .attachmentIdentifier(a.getAttachmentIdentifier())
                .filename(a.getFilename())
                .mimeType(a.getMimeType())
                .sizeBytes(a.getSizeBytes())
                .securityClass(a.getSecurityClass())
                .build())
            .collect(Collectors.toList());

        return InvoiceResponse.builder()
            .id(invoice.getId())
            .invoiceNumber(invoice.getInvoiceNumber())
            .templateCode(invoice.getTemplateCode())
            .language(invoice.getLanguage() != null ? invoice.getLanguage().name() : null)
            .invoicingMode(invoice.getInvoicingMode() != null ? invoice.getInvoicingMode().name() : null)
            .reverseChargeVat(invoice.isReverseChargeVat())
            .customText(invoice.getCustomText())
            .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
            .invoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null)
            .originalInvoiceId(invoice.getOriginalInvoice() != null ? invoice.getOriginalInvoice().getId() : null)
            .invoiceDate(invoice.getInvoiceDate())
            .dueDate(invoice.getDueDate())
            .netAmount(invoice.getNetAmount())
            .grossAmount(invoice.getGrossAmount())
            .vatAmount(invoice.getVatAmount())
            .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
            .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
            .lineItems(lineItems)
            .attachments(attachments)
            .build();
    }
}
