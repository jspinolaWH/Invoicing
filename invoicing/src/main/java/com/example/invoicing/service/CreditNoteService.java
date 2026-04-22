package com.example.invoicing.service;
import com.example.invoicing.common.exception.CreditNoteValidationException;
import com.example.invoicing.entity.invoice.dto.BatchCreditResult;
import com.example.invoicing.entity.invoice.dto.BatchCreditRequest;
import com.example.invoicing.entity.invoice.CreditType;
import com.example.invoicing.entity.invoice.InvoiceLanguage;
import com.example.invoicing.entity.invoice.InvoiceType;
import com.example.invoicing.entity.invoice.InvoiceStatus;
import com.example.invoicing.entity.invoice.InvoiceLineItem;
import com.example.invoicing.entity.invoice.Invoice;

import com.example.invoicing.entity.invoice.dto.CreditNoteRequest;
import com.example.invoicing.entity.invoice.dto.CreditNoteResponse;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.service.FinvoiceBuilderService;
import com.example.invoicing.entity.invoice.*;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreditNoteService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberSeriesRepository numberSeriesRepository;
    private final FinvoiceBuilderService finvoiceBuilderService;
    private final InvoiceTransmissionService invoiceTransmissionService;

    public CreditNoteResponse credit(Long originalInvoiceId, CreditNoteRequest request) {
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new CreditNoteValidationException("internalComment is mandatory for credit notes");
        }

        Invoice original = invoiceRepository.findById(originalInvoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + originalInvoiceId));

        if (original.getStatus() != InvoiceStatus.SENT &&
            original.getStatus() != InvoiceStatus.COMPLETED) {
            throw new CreditNoteValidationException(
                "Credit notes can only be issued for SENT or COMPLETED invoices. Current status: " + original.getStatus());
        }

        List<InvoiceLineItem> linesToCredit;
        CreditType creditType = request.getCreditType() != null ? request.getCreditType() : CreditType.FULL;
        if (creditType == CreditType.FULL) {
            linesToCredit = original.getLineItems();
        } else {
            Set<Long> ids = new HashSet<>(request.getLineItemIds() != null ? request.getLineItemIds() : List.of());
            linesToCredit = original.getLineItems().stream()
                .filter(l -> ids.contains(l.getId()))
                .toList();
            if (linesToCredit.isEmpty()) {
                throw new CreditNoteValidationException("No valid line items selected for partial credit");
            }
        }

        Invoice creditNote = new Invoice();
        creditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);
        creditNote.setOriginalInvoice(original);
        creditNote.setCustomer(original.getCustomer());
        creditNote.setLanguage(original.getLanguage());
        creditNote.setInvoicingMode(original.getInvoicingMode());
        creditNote.setReverseChargeVat(original.isReverseChargeVat());
        creditNote.setCustomText(request.getCustomText());
        creditNote.setInternalComment(request.getInternalComment());
        creditNote.setTemplateCode(original.getTemplateCode());
        creditNote.setStatus(InvoiceStatus.DRAFT);
        creditNote.setInvoiceDate(LocalDate.now());

        for (InvoiceLineItem originalLine : linesToCredit) {
            InvoiceLineItem creditLine = new InvoiceLineItem();
            creditLine.setInvoice(creditNote);
            creditLine.setProduct(originalLine.getProduct());
            creditLine.setDescription(originalLine.getDescription());
            creditLine.setQuantity(originalLine.getQuantity().negate());
            creditLine.setUnitPrice(originalLine.getUnitPrice());
            creditLine.setVatRate(originalLine.getVatRate());
            creditLine.setNetAmount(originalLine.getNetAmount().negate());
            creditLine.setGrossAmount(originalLine.getGrossAmount().negate());
            creditLine.setLegalClassification(originalLine.getLegalClassification());
            creditLine.setAccountingAccount(originalLine.getAccountingAccount());
            creditLine.setCostCenter(originalLine.getCostCenter());
            creditNote.getLineItems().add(creditLine);
        }

        creditNote.setNetAmount(creditNote.getLineItems().stream()
            .map(InvoiceLineItem::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        creditNote.setGrossAmount(creditNote.getLineItems().stream()
            .map(InvoiceLineItem::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        creditNote.setVatAmount(creditNote.getGrossAmount().subtract(creditNote.getNetAmount()));

        creditNote.setInvoiceNumber(original.getInvoiceNumber());

        try {
            String xml = finvoiceBuilderService.build(creditNote);
            creditNote.setFinvoiceXml(xml);
        } catch (Exception e) {
            log.warn("Failed to build FINVOICE XML for credit note: {}", e.getMessage());
        }

        creditNote.setStatus(InvoiceStatus.READY);
        Invoice saved = invoiceRepository.save(creditNote);

        log.info("Credit note {} issued for original invoice {} (type={}, reason={})",
            saved.getId(), originalInvoiceId, creditType, request.getInternalComment());

        try {
            invoiceTransmissionService.transmit(saved.getId());
        } catch (Exception e) {
            log.error("Failed to transmit credit note {}: {}", saved.getId(), e.getMessage());
            saved.setStatus(InvoiceStatus.ERROR);
            invoiceRepository.save(saved);
        }

        return CreditNoteResponse.from(saved, original, creditType);
    }

    public BatchCreditResult batchCredit(BatchCreditRequest request) {
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new CreditNoteValidationException("internalComment is mandatory for batch credit");
        }
        if (request.getInvoiceIds() == null || request.getInvoiceIds().isEmpty()) {
            throw new CreditNoteValidationException("At least one invoice ID required");
        }

        List<CreditNoteResponse> results = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Long invoiceId : request.getInvoiceIds()) {
            try {
                CreditNoteRequest singleRequest = new CreditNoteRequest();
                singleRequest.setCreditType(CreditType.FULL);
                singleRequest.setCustomText(request.getCustomText());
                singleRequest.setInternalComment(request.getInternalComment());
                results.add(credit(invoiceId, singleRequest));
            } catch (CreditNoteValidationException | EntityNotFoundException ex) {
                skipped.add("Invoice " + invoiceId + ": " + ex.getMessage());
            }
        }

        return new BatchCreditResult(results.size(), skipped.size(), results, skipped);
    }

    private String assignNextCreditNumber() {
        return numberSeriesRepository.findAll().stream()
            .findFirst()
            .map(series -> {
                long next = series.getCurrentCounter() + 1;
                series.setCurrentCounter(next);
                numberSeriesRepository.save(series);
                return series.getFormatPattern()
                    .replace("{PREFIX}", series.getPrefix())
                    .replace("{YEAR}", String.valueOf(LocalDate.now().getYear()))
                    .replace("{COUNTER:06d}", String.format("%06d", next));
            })
            .orElse("CR-" + System.currentTimeMillis());
    }
}
