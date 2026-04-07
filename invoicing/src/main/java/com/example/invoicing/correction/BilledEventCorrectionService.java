package com.example.invoicing.correction;

import com.example.invoicing.credit.CreditType;
import com.example.invoicing.credit.CreditNoteService;
import com.example.invoicing.credit.dto.CreditNoteRequest;
import com.example.invoicing.credit.dto.CreditNoteResponse;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(ev -> copyEventForCorrection(ev, targetCustomerNumber))
                .toList();
            List<BillingEvent> saved = billingEventRepository.saveAll(copies);
            saved.forEach(e -> copiedEventIds.add(e.getId()));
        }

        String msg = "Credit note issued" +
            (copiedEventIds.isEmpty() ? "." : " and " + copiedEventIds.size() + " events copied for re-invoicing.");

        log.info("Correction applied to invoice {}: credit note={}, copied events={}",
            originalInvoiceId, creditNote.getCreditNoteId(), copiedEventIds);

        return new CorrectionResult(originalInvoiceId, creditNote.getCreditNoteId(),
            copiedEventIds, targetCustomerId, msg);
    }

    private BillingEvent copyEventForCorrection(BillingEvent original, String targetCustomerNumber) {
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
        copy.setLocationId(original.getLocationId());
        copy.setCustomerNumber(targetCustomerNumber);
        copy.setStatus(BillingEventStatus.IN_PROGRESS);
        copy.setExcluded(false);
        copy.setOfficeReviewRequired(false);
        copy.setCorrectedFromEventId(original.getId());
        return copy;
    }
}
