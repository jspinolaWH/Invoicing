package com.example.invoicing.service;
import com.example.invoicing.entity.invoice.dto.BillingEventForCorrectionDto;
import com.example.invoicing.entity.invoice.dto.CorrectionResult;
import com.example.invoicing.entity.invoice.dto.CorrectionRequest;
import com.example.invoicing.entity.invoice.dto.CopyRequest;

import com.example.invoicing.entity.invoice.CreditType;
import com.example.invoicing.service.CreditNoteService;
import com.example.invoicing.entity.invoice.dto.CreditNoteRequest;
import com.example.invoicing.entity.invoice.dto.CreditNoteResponse;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.invoice.Invoice;
import com.example.invoicing.repository.InvoiceRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BilledEventCorrectionService {

    private final CreditNoteService creditNoteService;
    private final InvoiceRepository invoiceRepository;
    private final BillingEventRepository billingEventRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final InvoiceService invoiceService;

    public CorrectionResult correct(Long originalInvoiceId, CorrectionRequest request) {
        // 1. Issue credit note
        CreditNoteRequest creditRequest = new CreditNoteRequest();
        creditRequest.setCreditType(request.getLineItemIds() != null ? CreditType.PARTIAL : CreditType.FULL);
        creditRequest.setLineItemIds(request.getLineItemIds());
        creditRequest.setCustomText(request.getCustomText());
        creditRequest.setInternalComment(request.getInternalComment());

        CreditNoteResponse creditNote = creditNoteService.credit(originalInvoiceId, creditRequest);

        // 2. Determine target customer number
        Invoice original = invoiceRepository.findById(originalInvoiceId).orElseThrow();
        Long targetCustomerId = request.getTargetCustomerId() != null
            ? request.getTargetCustomerId()
            : original.getCustomer().getId();

        String targetCustomerNumber = customerRepository.findById(targetCustomerId)
            .map(c -> c.getBillingProfile() != null ? c.getBillingProfile().getCustomerIdNumber() : null)
            .orElseThrow(() -> new EntityNotFoundException("Target customer not found: " + targetCustomerId));

        // 3. Copy billing events if IDs provided
        List<Long> copiedEventIds = new ArrayList<>();
        if (request.getBillingEventIds() != null && !request.getBillingEventIds().isEmpty()) {
            List<BillingEvent> originalEvents = billingEventRepository
                .findAllById(request.getBillingEventIds());

            List<BillingEvent> copies = originalEvents.stream()
                .map(ev -> copyEventForCorrection(ev, targetCustomerNumber, request.getTargetPropertyId()))
                .toList();
            List<BillingEvent> saved = billingEventRepository.saveAll(copies);
            saved.forEach(e -> copiedEventIds.add(e.getId()));
        }

        // 4. Create a new draft invoice from the copied events
        Long draftInvoiceId = invoiceService.createDraftFromEvents(copiedEventIds, targetCustomerId);

        String msg = "Credit note issued" +
            (copiedEventIds.isEmpty() ? "." : " and " + copiedEventIds.size() + " events copied to new draft invoice.");

        log.info("Correction applied to invoice {}: credit note={}, copied events={}, draftInvoice={}",
            originalInvoiceId, creditNote.getCreditNoteId(), copiedEventIds, draftInvoiceId);

        return new CorrectionResult(originalInvoiceId, creditNote.getCreditNoteId(),
            copiedEventIds, targetCustomerId, draftInvoiceId, msg);
    }

    public CorrectionResult copy(Long originalInvoiceId, CopyRequest request) {
        if (request.getInternalComment() == null || request.getInternalComment().isBlank()) {
            throw new IllegalArgumentException("internalComment is required for copy");
        }

        Invoice original = invoiceRepository.findById(originalInvoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + originalInvoiceId));

        Long targetCustomerId = request.getTargetCustomerId() != null
            ? request.getTargetCustomerId()
            : original.getCustomer().getId();

        String targetCustomerNumber = customerRepository.findById(targetCustomerId)
            .map(c -> c.getBillingProfile() != null ? c.getBillingProfile().getCustomerIdNumber() : null)
            .orElseThrow(() -> new EntityNotFoundException("Target customer not found: " + targetCustomerId));

        List<Long> copiedEventIds = new ArrayList<>();
        if (request.getBillingEventIds() != null && !request.getBillingEventIds().isEmpty()) {
            List<BillingEvent> originalEvents = billingEventRepository
                .findAllById(request.getBillingEventIds());

            List<BillingEvent> copies = originalEvents.stream()
                .map(ev -> copyEventForCorrection(ev, targetCustomerNumber, request.getTargetPropertyId()))
                .toList();
            List<BillingEvent> saved = billingEventRepository.saveAll(copies);
            saved.forEach(e -> copiedEventIds.add(e.getId()));
        }

        Long draftInvoiceId = invoiceService.createDraftFromEvents(copiedEventIds, targetCustomerId);

        log.info("Copy applied to invoice {}: copied events={}, draftInvoice={}",
            originalInvoiceId, copiedEventIds, draftInvoiceId);

        return new CorrectionResult(originalInvoiceId, null, copiedEventIds, targetCustomerId,
            draftInvoiceId, copiedEventIds.size() + " events copied to new draft invoice.");
    }

    public List<BillingEventForCorrectionDto> getBillingEventsForInvoice(Long invoiceId) {
        return billingEventRepository.findByInvoiceId(invoiceId).stream()
            .map(e -> BillingEventForCorrectionDto.builder()
                .id(e.getId())
                .eventDate(e.getEventDate())
                .productName(e.getProduct() != null ? e.getProduct().getCode() : null)
                .customerNumber(e.getCustomerNumber())
                .totalFees(e.getWasteFeePrice()
                    .add(e.getTransportFeePrice())
                    .add(e.getEcoFeePrice()))
                .build())
            .toList();
    }

    private BillingEvent copyEventForCorrection(BillingEvent original, String targetCustomerNumber,
                                                 String targetPropertyId) {
        BillingEvent copy = new BillingEvent();
        copy.setEventDate(original.getEventDate());
        copy.setProduct(original.getProduct());
        copy.setQuantity(original.getQuantity());
        copy.setWeight(original.getWeight());
        copy.setWasteFeePrice(original.getWasteFeePrice());
        copy.setTransportFeePrice(original.getTransportFeePrice());
        copy.setEcoFeePrice(original.getEcoFeePrice());
        copy.setVatRate0(original.getVatRate0());
        copy.setVatRate24(original.getVatRate24());
        copy.setLegalClassification(original.getLegalClassification());
        copy.setAccountingAccount(original.getAccountingAccount());
        copy.setCostCenter(original.getCostCenter());
        copy.setMunicipalityId(original.getMunicipalityId());
        copy.setLocationId(targetPropertyId != null ? targetPropertyId : original.getLocationId());
        copy.setCustomerNumber(targetCustomerNumber);
        copy.setStatus(BillingEventStatus.IN_PROGRESS);
        copy.setExcluded(false);
        copy.setOfficeReviewRequired(false);
        copy.setCorrectedFromEventId(original.getId());
        return copy;
    }
}
