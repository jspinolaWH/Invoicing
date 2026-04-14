package com.example.invoicing.service;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.audit.BillingEventAuditLog;
import com.example.invoicing.entity.billingevent.credit.BillingEventCreditLog;
import com.example.invoicing.entity.billingevent.credit.BillingEventCreditLogRepository;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferLinkResponse;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferRequest;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferResult;
import com.example.invoicing.repository.BillingEventAuditLogRepository;
import com.example.invoicing.repository.BillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventCreditTransferService {

    private final BillingEventRepository billingEventRepository;
    private final BillingEventCreditLogRepository creditLogRepository;
    private final BillingEventAuditLogRepository auditLogRepository;

    public CreditTransferResult creditAndTransfer(Long originalEventId,
                                                  CreditTransferRequest request,
                                                  String currentUser) {
        BillingEvent original = billingEventRepository.findById(originalEventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + originalEventId));

        if (original.getStatus() != BillingEventStatus.SENT
                && original.getStatus() != BillingEventStatus.COMPLETED) {
            throw new IllegalStateException(
                "Credit & Transfer is only available for SENT or COMPLETED events. " +
                "Use the regular Transfer for IN_PROGRESS events.");
        }

        if (original.isExcluded()) {
            throw new IllegalStateException(
                "BillingEvent " + originalEventId + " is excluded and cannot be credited.");
        }

        Instant now = Instant.now();

        // --- Credit event (reversal of the original) ---
        BillingEvent creditEvent = new BillingEvent();
        copyFields(original, creditEvent);
        creditEvent.setWasteFeePrice(original.getWasteFeePrice().negate());
        creditEvent.setTransportFeePrice(original.getTransportFeePrice().negate());
        creditEvent.setEcoFeePrice(original.getEcoFeePrice().negate());
        creditEvent.setOrigin("CREDIT");
        creditEvent.setCorrectedFromEventId(originalEventId);
        creditEvent.setStatus(BillingEventStatus.COMPLETED);
        BillingEvent savedCredit = billingEventRepository.save(creditEvent);

        // --- New transfer event (same data, new customer) ---
        BillingEvent newEvent = new BillingEvent();
        copyFields(original, newEvent);
        newEvent.setCustomerNumber(request.getTargetCustomerNumber());
        if (request.getTargetPropertyId() != null && !request.getTargetPropertyId().isBlank()) {
            newEvent.setLocationId(request.getTargetPropertyId());
        }
        newEvent.setOrigin("TRANSFER");
        newEvent.setCorrectedFromEventId(originalEventId);
        newEvent.setStatus(BillingEventStatus.IN_PROGRESS);
        BillingEvent savedNew = billingEventRepository.save(newEvent);

        // --- Credit log (links all three) ---
        creditLogRepository.save(BillingEventCreditLog.builder()
            .originalEventId(originalEventId)
            .creditEventId(savedCredit.getId())
            .newEventId(savedNew.getId())
            .performedBy(currentUser)
            .performedAt(now)
            .reason(request.getReason())
            .build());

        // --- Audit entries ---
        List<BillingEventAuditLog> auditEntries = new ArrayList<>();

        // On the credit event: record what it credits and why
        auditEntries.add(BillingEventAuditLog.builder()
            .billingEventId(savedCredit.getId())
            .field("creditOf")
            .oldValue(null)
            .newValue(String.valueOf(originalEventId))
            .changedBy(currentUser).changedAt(now)
            .reason(request.getReason()).build());

        // On the new transfer event: record the customer change
        auditEntries.add(BillingEventAuditLog.builder()
            .billingEventId(savedNew.getId())
            .field("customerNumber")
            .oldValue(original.getCustomerNumber())
            .newValue(request.getTargetCustomerNumber())
            .changedBy(currentUser).changedAt(now)
            .reason(request.getReason()).build());

        if (request.getTargetPropertyId() != null && !request.getTargetPropertyId().isBlank()) {
            auditEntries.add(BillingEventAuditLog.builder()
                .billingEventId(savedNew.getId())
                .field("locationId")
                .oldValue(original.getLocationId())
                .newValue(request.getTargetPropertyId())
                .changedBy(currentUser).changedAt(now)
                .reason(request.getReason()).build());
        }

        // On the original event: record that it was credited (read-only trace, no field mutation)
        auditEntries.add(BillingEventAuditLog.builder()
            .billingEventId(originalEventId)
            .field("creditedBy")
            .oldValue(null)
            .newValue(String.valueOf(savedCredit.getId()))
            .changedBy(currentUser).changedAt(now)
            .reason(request.getReason()).build());

        auditLogRepository.saveAll(auditEntries);

        return new CreditTransferResult(originalEventId, savedCredit.getId(), savedNew.getId(), request.getReason());
    }

    @Transactional(readOnly = true)
    public Optional<CreditTransferLinkResponse> findLink(Long eventId) {
        Optional<BillingEventCreditLog> log =
            creditLogRepository.findByOriginalEventId(eventId)
                .or(() -> creditLogRepository.findByCreditEventId(eventId))
                .or(() -> creditLogRepository.findByNewEventId(eventId));

        return log.map(l -> new CreditTransferLinkResponse(
            l.getOriginalEventId(),
            l.getCreditEventId(),
            l.getNewEventId(),
            l.getPerformedBy(),
            l.getPerformedAt(),
            l.getReason()
        ));
    }

    // Copy all business fields from source to target (excluding id, status, origin, correctedFromEventId)
    private void copyFields(BillingEvent source, BillingEvent target) {
        target.setEventDate(source.getEventDate());
        target.setProduct(source.getProduct());
        target.setWasteFeePrice(source.getWasteFeePrice());
        target.setTransportFeePrice(source.getTransportFeePrice());
        target.setEcoFeePrice(source.getEcoFeePrice());
        target.setQuantity(source.getQuantity());
        target.setWeight(source.getWeight());
        target.setVatRate0(source.getVatRate0());
        target.setVatRate24(source.getVatRate24());
        target.setVehicleId(source.getVehicleId());
        target.setDriverId(source.getDriverId());
        target.setCostCenter(source.getCostCenter());
        target.setAccountingAccount(source.getAccountingAccount());
        target.setCustomerNumber(source.getCustomerNumber());
        target.setContractor(source.getContractor());
        target.setLocationId(source.getLocationId());
        target.setMunicipalityId(source.getMunicipalityId());
        target.setSharedServiceGroupPercentage(source.getSharedServiceGroupPercentage());
        target.setDirection(source.getDirection());
        target.setSharedServiceGroupId(source.getSharedServiceGroupId());
        target.setComments(source.getComments());
        target.setNonBillable(source.isNonBillable());
        target.setLegalClassification(source.getLegalClassification());
        target.setProjectId(source.getProjectId());
    }
}
