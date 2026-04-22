package com.example.invoicing.service;

import com.example.invoicing.service.CostCenterCompositionService;
import com.example.invoicing.entity.weighbridge.dto.WeighbridgeConfigResponse;
import com.example.invoicing.entity.vat.VatCalculationResult;
import com.example.invoicing.service.VatCalculationService;
import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.BillingEventValidationStatus;
import com.example.invoicing.entity.billingevent.audit.BillingEventAuditLog;
import com.example.invoicing.entity.billingevent.dto.*;
import com.example.invoicing.entity.billingevent.dto.BulkExcludeFailure;
import com.example.invoicing.entity.billingevent.dto.BulkExcludeResult;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventService {

    private final BillingEventRepository billingEventRepository;
    private final BillingEventStatusService statusService;
    private final BillingEventAuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final VatRateRepository vatRateRepository;
    private final AccountingAllocationRuleRepository allocationRuleRepository;
    private final AccountingAccountRepository accountingAccountRepository;
    private final CostCenterRepository costCenterRepository;
    private final LegalClassificationService classificationService;
    private final CostCenterCompositionService costCenterCompositionService;
    private final VatCalculationService vatCalculationService;
    private final WeighbridgeIntegrationConfigService weighbridgeConfigService;

    // -----------------------------------------------------------------------
    // CREATE — external source (integration / weighbridge)
    // -----------------------------------------------------------------------
    public BillingEventResponse createFromExternalSource(BillingEventCreateRequest req) {
        Product product = loadProduct(req.getProductId());
        BillingEvent event = new BillingEvent();
        mapCommonFields(event, req, product);

        weighbridgeConfigService.findByCustomerNumber(req.getCustomerNumber())
            .filter(WeighbridgeConfigResponse::isActive)
            .ifPresent(config -> {
                if (event.getProjectId() == null && config.getSiteReference() != null) {
                    event.setProjectId(config.getSiteReference());
                }
            });

        if (req.getAccountingAccountId() != null && req.getCostCenterId() != null) {
            event.setAccountingAccount(accountingAccountRepository.getReferenceById(req.getAccountingAccountId()));
            CostCenter cc = costCenterRepository.findById(req.getCostCenterId())
                .orElseThrow(() -> new EntityNotFoundException("CostCenter not found: " + req.getCostCenterId()));
            event.setCostCenter(cc);
            snapshotCostCenterSegments(event, cc);
        } else {
            resolveAccountingDefaults(event, product, req.getLocationId());
        }

        if (req.getVatRate0() != null && req.getVatRate24() != null) {
            event.setVatRate0(req.getVatRate0());
            event.setVatRate24(req.getVatRate24());
        } else {
            resolveVatRates(event, req.getEventDate());
        }

        LegalClassification classification = req.getLegalClassification() != null
            ? req.getLegalClassification()
            : classificationService.classify(req.getCustomerNumber(), product, req.getMunicipalityId());
        event.setLegalClassification(classification);
        event.setStatus(BillingEventStatus.IN_PROGRESS);
        event.setOrigin("INTEGRATION");

        return toResponse(billingEventRepository.save(event));
    }

    // -----------------------------------------------------------------------
    // CREATE — manual (billing clerk)
    // -----------------------------------------------------------------------
    public BillingEventResponse createManual(BillingEventManualCreateRequest req) {
        BillingEvent event = buildManualEvent(req);
        event.setStatus(BillingEventStatus.IN_PROGRESS);
        return toResponse(billingEventRepository.save(event));
    }

    public BillingEventResponse saveDraft(BillingEventManualCreateRequest req) {
        BillingEvent event = buildManualEvent(req);
        event.setStatus(BillingEventStatus.DRAFT);
        return toResponse(billingEventRepository.save(event));
    }

    private BillingEvent buildManualEvent(BillingEventManualCreateRequest req) {
        Product product = loadProduct(req.getProductId());
        BillingEvent event = new BillingEvent();

        event.setEventDate(req.getEventDate());
        event.setProduct(product);
        event.setWasteFeePrice(req.getWasteFeePrice());
        event.setTransportFeePrice(req.getTransportFeePrice());
        event.setEcoFeePrice(req.getEcoFeePrice());
        event.setQuantity(req.getQuantity());
        event.setWeight(req.getWeight());
        event.setCustomerNumber(req.getCustomerNumber());
        event.setVehicleId(req.getVehicleId());
        event.setDriverId(req.getDriverId());
        event.setLocationId(req.getLocationId());
        event.setMunicipalityId(req.getMunicipalityId());
        event.setComments(req.getComments());
        event.setInternalComments(req.getInternalComments());
        event.setRegistrationNumber(req.getRegistrationNumber());
        event.setContractor(req.getContractor());
        event.setDirection(req.getDirection());
        event.setSharedServiceGroupPercentage(req.getSharedServiceGroupPercentage());
        event.setWasteType(req.getWasteType());
        event.setReceivingSite(req.getReceivingSite());
        event.setProductGroup(product.getProductGroupCode());

        BigDecimal defWaste     = product.getDefaultWasteFee();
        BigDecimal defTransport = product.getDefaultTransportFee();
        BigDecimal defEco       = product.getDefaultEcoFee();
        boolean overridden = (defWaste     != null && req.getWasteFeePrice().compareTo(defWaste)         != 0)
                          || (defTransport != null && req.getTransportFeePrice().compareTo(defTransport) != 0)
                          || (defEco       != null && req.getEcoFeePrice().compareTo(defEco)             != 0);
        if (overridden) {
            event.setPriceOverridden(true);
            event.setOriginalWasteFeePrice(defWaste);
            event.setOriginalTransportFeePrice(defTransport);
            event.setOriginalEcoFeePrice(defEco);
        }

        resolveAccountingDefaults(event, product, req.getLocationId());
        resolveVatRates(event, req.getEventDate());
        event.setLegalClassification(
            classificationService.classify(req.getCustomerNumber(), product, req.getMunicipalityId()));
        event.setOrigin("MANUAL");
        return event;
    }

    // -----------------------------------------------------------------------
    // UPDATE — partial edit with audit log
    // -----------------------------------------------------------------------
    public BillingEventResponse update(Long id, BillingEventUpdateRequest req, String currentUser, boolean pricingOnly) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));

        statusService.assertMutable(event);
        BillingEventStatus originalStatus = event.getStatus();

        if (pricingOnly) {
            boolean hasNonPricingChange =
                req.getEventDate() != null ||
                req.getProductId() != null ||
                req.getQuantity() != null ||
                req.getWeight() != null ||
                req.getVehicleId() != null ||
                req.getDriverId() != null ||
                req.getCustomerNumber() != null ||
                req.getContractor() != null ||
                req.getLocationId() != null ||
                req.getMunicipalityId() != null ||
                req.getSharedServiceGroupId() != null ||
                req.getSharedServiceGroupPercentage() != null ||
                req.getDirection() != null ||
                req.getComments() != null ||
                req.getInternalComments() != null ||
                req.getRegistrationNumber() != null ||
                req.getWasteType() != null ||
                req.getReceivingSite() != null;
            if (hasNonPricingChange) {
                throw new AccessDeniedException("INVOICING_PRICING role may only update pricing fields (wasteFeePrice, transportFeePrice, ecoFeePrice)");
            }
        }

        List<BillingEventAuditLog> auditEntries = new ArrayList<>();

        if (req.getEventDate() != null && !req.getEventDate().equals(event.getEventDate())) {
            auditEntries.add(buildAudit(id, "eventDate", event.getEventDate(), req.getEventDate(), currentUser, req.getReason()));
            event.setEventDate(req.getEventDate());
            resolveVatRates(event, req.getEventDate());
        }

        if (req.getProductId() != null && !req.getProductId().equals(event.getProduct().getId())) {
            Product newProduct = loadProduct(req.getProductId());
            auditEntries.add(buildAudit(id, "product", event.getProduct().getId(), newProduct.getId(), currentUser, req.getReason()));
            event.setProduct(newProduct);
            resolveAccountingDefaults(event, newProduct, event.getLocationId());
        }

        applyIfChanged(id, "wasteFeePrice",          req.getWasteFeePrice(),          event.getWasteFeePrice(),          event::setWasteFeePrice,          currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "transportFeePrice",      req.getTransportFeePrice(),      event.getTransportFeePrice(),      event::setTransportFeePrice,      currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "ecoFeePrice",            req.getEcoFeePrice(),            event.getEcoFeePrice(),            event::setEcoFeePrice,            currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "quantity",               req.getQuantity(),               event.getQuantity(),               event::setQuantity,               currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "weight",                 req.getWeight(),                 event.getWeight(),                 event::setWeight,                 currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "vehicleId",              req.getVehicleId(),              event.getVehicleId(),              event::setVehicleId,              currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "driverId",               req.getDriverId(),               event.getDriverId(),               event::setDriverId,               currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "customerNumber",         req.getCustomerNumber(),         event.getCustomerNumber(),         event::setCustomerNumber,         currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "contractor",             req.getContractor(),             event.getContractor(),             event::setContractor,             currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "locationId",             req.getLocationId(),             event.getLocationId(),             event::setLocationId,             currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "municipalityId",         req.getMunicipalityId(),         event.getMunicipalityId(),         event::setMunicipalityId,         currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "sharedServiceGroupId",   req.getSharedServiceGroupId(),   event.getSharedServiceGroupId(),   event::setSharedServiceGroupId,   currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "sharedServiceGroupPct",  req.getSharedServiceGroupPercentage(), event.getSharedServiceGroupPercentage(), event::setSharedServiceGroupPercentage, currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "direction",              req.getDirection(),              event.getDirection(),              event::setDirection,              currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "comments",               req.getComments(),               event.getComments(),               event::setComments,               currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "internalComments",       req.getInternalComments(),       event.getInternalComments(),       event::setInternalComments,       currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "registrationNumber",     req.getRegistrationNumber(),     event.getRegistrationNumber(),     event::setRegistrationNumber,     currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "wasteType",              req.getWasteType(),              event.getWasteType(),              event::setWasteType,              currentUser, req.getReason(), auditEntries);
        applyIfChanged(id, "receivingSite",          req.getReceivingSite(),          event.getReceivingSite(),          event::setReceivingSite,          currentUser, req.getReason(), auditEntries);

        if (!auditEntries.isEmpty() && originalStatus == BillingEventStatus.IN_PROGRESS) {
            event.setStatus(BillingEventStatus.FOR_CORRECTION);
            auditEntries.add(buildAudit(id, "status", originalStatus, BillingEventStatus.FOR_CORRECTION,
                currentUser, "Flagged for correction review after manual edit"));
        }

        billingEventRepository.save(event);
        if (!auditEntries.isEmpty()) {
            auditLogRepository.saveAll(auditEntries);
        }

        return toResponse(event);
    }

    // -----------------------------------------------------------------------
    // APPROVE CORRECTION
    // -----------------------------------------------------------------------
    public BillingEventResponse approveCorrection(Long id, String currentUser) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        if (event.getStatus() != BillingEventStatus.FOR_CORRECTION) {
            throw new IllegalStateException("BillingEvent " + id + " is not in FOR_CORRECTION status");
        }
        event.setStatus(BillingEventStatus.IN_PROGRESS);
        billingEventRepository.save(event);
        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(id).field("status")
            .oldValue("FOR_CORRECTION").newValue("IN_PROGRESS")
            .changedBy(currentUser).changedAt(Instant.now())
            .reason("Correction reviewed and approved").build());
        return toResponse(event);
    }

    // -----------------------------------------------------------------------
    // EXCLUSION
    // -----------------------------------------------------------------------
    public BillingEventResponse exclude(Long id, String exclusionReason, String currentUser) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        statusService.assertMutable(event);
        if (event.isExcluded()) {
            throw new IllegalStateException("BillingEvent " + id + " is already excluded.");
        }
        event.setExcluded(true);
        event.setExclusionReason(exclusionReason);
        event.setExcludedBy(currentUser);
        event.setExcludedAt(Instant.now());
        billingEventRepository.save(event);
        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(id).field("excluded")
            .oldValue("false").newValue("true")
            .changedBy(currentUser).changedAt(Instant.now())
            .reason(exclusionReason).build());
        return toResponse(event);
    }

    public BillingEventResponse reinstate(Long id, String reason, String currentUser) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        if (!event.isExcluded()) {
            throw new IllegalStateException("BillingEvent " + id + " is not excluded and cannot be reinstated.");
        }
        String previousReason = event.getExclusionReason();
        event.setExcluded(false);
        event.setExclusionReason(null);
        event.setExcludedBy(null);
        event.setExcludedAt(null);
        billingEventRepository.save(event);
        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(id).field("excluded")
            .oldValue("true (reason: " + previousReason + ")").newValue("false")
            .changedBy(currentUser).changedAt(Instant.now())
            .reason(reason).build());
        return toResponse(event);
    }

    public BulkExcludeResult bulkExclude(List<Long> eventIds, String exclusionReason, String currentUser) {
        List<BillingEvent> events = billingEventRepository.findAllById(eventIds);
        List<Long> succeeded = new ArrayList<>();
        List<BulkExcludeFailure> failed = new ArrayList<>();
        for (BillingEvent event : events) {
            try {
                statusService.assertMutable(event);
                if (event.isExcluded()) {
                    failed.add(new BulkExcludeFailure(event.getId(), "Already excluded"));
                    continue;
                }
                event.setExcluded(true);
                event.setExclusionReason(exclusionReason);
                event.setExcludedBy(currentUser);
                event.setExcludedAt(Instant.now());
                succeeded.add(event.getId());
            } catch (IllegalStateException ex) {
                failed.add(new BulkExcludeFailure(event.getId(), ex.getMessage()));
            }
        }
        billingEventRepository.saveAll(events.stream().filter(e -> succeeded.contains(e.getId())).toList());
        auditLogRepository.saveAll(succeeded.stream()
            .map(eid -> BillingEventAuditLog.builder()
                .billingEventId(eid).field("excluded")
                .oldValue("false").newValue("true")
                .changedBy(currentUser).changedAt(Instant.now())
                .reason(exclusionReason).build())
            .toList());
        return new BulkExcludeResult(succeeded, failed);
    }

    // -----------------------------------------------------------------------
    // PENDING REVIEW QUERY
    // -----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<BillingEventResponse> findPendingReview() {
        return billingEventRepository.findPendingOfficeReview()
            .stream().map(this::toResponse).toList();
    }

    // -----------------------------------------------------------------------
    // QUERIES
    // -----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Page<BillingEventResponse> findFiltered(String customerNumber, BillingEventStatus status,
            String municipalityId, LocalDate dateFrom, LocalDate dateTo,
            Long productId, Boolean excluded, Boolean requiresReview,
            String serviceResponsibility, BillingEventValidationStatus validationStatus,
            Pageable pageable) {
        return billingEventRepository.findFiltered(
            customerNumber, status, municipalityId, dateFrom, dateTo,
            productId, excluded, requiresReview, serviceResponsibility, validationStatus, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BillingEventDetailResponse findById(Long id) {
        BillingEvent e = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        return toDetailResponse(e);
    }

    public BillingEventDetailResponse overrideValidation(Long id, String reason, String currentUser) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        event.setValidationOverrideReason(reason);
        event.setValidationOverriddenBy(currentUser);
        event.setValidationOverriddenAt(java.time.Instant.now());
        event.setValidationStatus(BillingEventValidationStatus.OVERRIDDEN);
        billingEventRepository.save(event);
        return toDetailResponse(event);
    }

    // -----------------------------------------------------------------------
    // EXPORT
    // -----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<BillingEventExportRow> exportEvents(LocalDate dateFrom, LocalDate dateTo,
            BillingEventStatus status, String customerNumber, String municipalityId) {
        return billingEventRepository.findForExport(dateFrom, dateTo, status, customerNumber, municipalityId)
            .stream().map(e -> {
                VatCalculationResult vat = null;
                try { vat = vatCalculationService.calculate(e); } catch (Exception ignored) {}
                return BillingEventExportRow.builder()
                    .eventId(e.getId())
                    .eventDate(e.getEventDate())
                    .accountingAccount(e.getAccountingAccount() != null ? e.getAccountingAccount().getCode() : null)
                    .responsibilityArea(e.getResponsibilityArea())
                    .productGroup(e.getProductGroup())
                    .wasteType(e.getWasteType())
                    .serviceResponsibility(e.getServiceResponsibility())
                    .locationId(e.getLocationId())
                    .municipalityId(e.getMunicipalityId())
                    .projectCode(e.getProjectId())
                    .costCenter(e.getCostCenter() != null ? e.getCostCenter().getCompositeCode() : null)
                    .receivingSite(e.getReceivingSite())
                    .calculatedAmountNet(vat != null ? vat.getAmountNet() : null)
                    .calculatedAmountVat(vat != null ? vat.getAmountVat() : null)
                    .calculatedAmountGross(vat != null ? vat.getAmountGross() : null)
                    .customerNumber(e.getCustomerNumber())
                    .build();
            }).toList();
    }

    // -----------------------------------------------------------------------
    // SELECTIVE COMPONENT INVOICING (AC3)
    // -----------------------------------------------------------------------
    public BillingEventDetailResponse updateComponentInclusion(Long id, SelectiveComponentRequest req, String user) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        statusService.assertMutable(event);

        List<BillingEventAuditLog> auditEntries = new ArrayList<>();
        if (req.getIncludeWasteFee() != null && req.getIncludeWasteFee() != event.isIncludeWasteFee()) {
            auditEntries.add(buildAudit(id, "includeWasteFee", event.isIncludeWasteFee(), req.getIncludeWasteFee(), user, null));
            event.setIncludeWasteFee(req.getIncludeWasteFee());
        }
        if (req.getIncludeTransportFee() != null && req.getIncludeTransportFee() != event.isIncludeTransportFee()) {
            auditEntries.add(buildAudit(id, "includeTransportFee", event.isIncludeTransportFee(), req.getIncludeTransportFee(), user, null));
            event.setIncludeTransportFee(req.getIncludeTransportFee());
        }
        if (req.getIncludeEcoFee() != null && req.getIncludeEcoFee() != event.isIncludeEcoFee()) {
            auditEntries.add(buildAudit(id, "includeEcoFee", event.isIncludeEcoFee(), req.getIncludeEcoFee(), user, null));
            event.setIncludeEcoFee(req.getIncludeEcoFee());
        }
        billingEventRepository.save(event);
        if (!auditEntries.isEmpty()) auditLogRepository.saveAll(auditEntries);
        return toDetailResponse(event);
    }

    // -----------------------------------------------------------------------
    // CONTRACTOR PAYMENT (AC5)
    // -----------------------------------------------------------------------
    public BillingEventDetailResponse recordContractorPayment(Long id, ContractorPaymentRequest req, String user) {
        BillingEvent event = billingEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + id));
        if (event.getContractor() == null || event.getContractor().isBlank()) {
            throw new IllegalStateException("BillingEvent " + id + " has no contractor assigned.");
        }
        String oldStatus = event.getContractorPaymentStatus();
        event.setContractorPaymentStatus(req.getStatus());
        event.setContractorPaymentNotes(req.getNotes());
        event.setContractorPaymentRecordedBy(user);
        event.setContractorPaymentRecordedAt(Instant.now());
        billingEventRepository.save(event);
        auditLogRepository.save(BillingEventAuditLog.builder()
            .billingEventId(id).field("contractorPaymentStatus")
            .oldValue(oldStatus).newValue(req.getStatus())
            .changedBy(user).changedAt(Instant.now())
            .reason(req.getNotes()).build());
        return toDetailResponse(event);
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------
    private void snapshotCostCenterSegments(BillingEvent event, CostCenter cc) {
        event.setResponsibilityArea(cc.getResponsibilitySegment());
        event.setServiceResponsibility(cc.getReceptionSegment());
    }

    private void resolveVatRates(BillingEvent event, LocalDate eventDate) {
        List<VatRate> rates = vatRateRepository.findByEventDate(eventDate);
        rates.stream().filter(r -> r.getRate().compareTo(BigDecimal.ZERO) == 0)
            .findFirst().ifPresent(r -> event.setVatRate0(r.getRate()));
        rates.stream().filter(r -> r.getRate().compareTo(BigDecimal.ZERO) > 0)
            .findFirst().ifPresent(r -> event.setVatRate24(r.getRate()));
        if (event.getVatRate0() == null) event.setVatRate0(BigDecimal.ZERO);
        if (event.getVatRate24() == null) event.setVatRate24(BigDecimal.ZERO);
    }

    private void resolveAccountingDefaults(BillingEvent event, Product product, String locationId) {
        allocationRuleRepository.findBestMatchForProduct(product.getId(), locationId)
            .ifPresent(rule -> event.setAccountingAccount(rule.getAccountingAccount()));
    }

    private void mapCommonFields(BillingEvent event, BillingEventCreateRequest req, Product product) {
        event.setEventDate(req.getEventDate());
        event.setProduct(product);
        event.setWasteFeePrice(req.getWasteFeePrice());
        event.setTransportFeePrice(req.getTransportFeePrice());
        event.setEcoFeePrice(req.getEcoFeePrice());
        event.setQuantity(req.getQuantity());
        event.setWeight(req.getWeight());
        event.setCustomerNumber(req.getCustomerNumber());
        event.setVehicleId(req.getVehicleId());
        event.setDriverId(req.getDriverId());
        event.setContractor(req.getContractor());
        event.setLocationId(req.getLocationId());
        event.setMunicipalityId(req.getMunicipalityId());
        event.setSharedServiceGroupId(req.getSharedServiceGroupId());
        event.setSharedServiceGroupPercentage(req.getSharedServiceGroupPercentage());
        event.setComments(req.getComments());
        event.setInternalComments(req.getInternalComments());
        event.setRegistrationNumber(req.getRegistrationNumber());
        event.setProjectId(req.getProjectId());
        event.setWasteType(req.getWasteType());
        event.setReceivingSite(req.getReceivingSite());
        event.setProductGroup(product.getProductGroupCode());
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
    }

    private <T> BillingEventAuditLog buildAudit(Long eventId, String field, T oldVal, T newVal,
                                                  String changedBy, String reason) {
        return BillingEventAuditLog.builder()
            .billingEventId(eventId)
            .field(field)
            .oldValue(oldVal != null ? oldVal.toString() : null)
            .newValue(newVal != null ? newVal.toString() : null)
            .changedBy(changedBy)
            .changedAt(Instant.now())
            .reason(reason)
            .build();
    }

    private <T> void applyIfChanged(Long eventId, String fieldName, T newVal, T oldVal,
                                     Consumer<T> setter, String user, String reason,
                                     List<BillingEventAuditLog> entries) {
        if (newVal == null || newVal.equals(oldVal)) return;
        entries.add(buildAudit(eventId, fieldName, oldVal, newVal, user, reason));
        setter.accept(newVal);
    }

    public BillingEventResponse toResponse(BillingEvent e) {
        return BillingEventResponse.builder()
            .id(e.getId())
            .eventDate(e.getEventDate())
            .product(e.getProduct() != null ? ProductSummaryDto.builder()
                .id(e.getProduct().getId()).code(e.getProduct().getCode())
                .name(e.getProduct().getTranslations().stream()
                    .filter(t -> "en".equals(t.getLocale())).findFirst()
                    .map(t -> t.getName()).orElse(e.getProduct().getCode()))
                .build() : null)
            .wasteFeePrice(e.getWasteFeePrice())
            .transportFeePrice(e.getTransportFeePrice())
            .ecoFeePrice(e.getEcoFeePrice())
            .quantity(e.getQuantity())
            .weight(e.getWeight())
            .vatRate0(e.getVatRate0())
            .vatRate24(e.getVatRate24())
            .vehicleId(e.getVehicleId())
            .driverId(e.getDriverId())
            .customerNumber(e.getCustomerNumber())
            .contractor(e.getContractor())
            .locationId(e.getLocationId())
            .municipalityId(e.getMunicipalityId())
            .sharedServiceGroupId(e.getSharedServiceGroupId())
            .sharedServiceGroupPercentage(e.getSharedServiceGroupPercentage())
            .direction(e.getDirection())
            .comments(e.getComments())
            .status(e.getStatus())
            .excluded(e.isExcluded())
            .exclusionReason(e.getExclusionReason())
            .nonBillable(e.isNonBillable())
            .legalClassification(e.getLegalClassification())
            .officeReviewRequired(e.isOfficeReviewRequired())
            .projectId(e.getProjectId())
            .origin(e.getOrigin())
            .accountingAccount(e.getAccountingAccount() != null ? AccountSummaryDto.builder()
                .id(e.getAccountingAccount().getId())
                .code(e.getAccountingAccount().getCode())
                .name(e.getAccountingAccount().getName())
                .build() : null)
            .costCenter(e.getCostCenter() != null ? CostCenterSummaryDto.builder()
                .id(e.getCostCenter().getId())
                .compositeCode(e.getCostCenter().getCompositeCode())
                .build() : null)
            .createdAt(e.getCreatedAt())
            .createdBy(e.getCreatedBy())
            .wasteType(e.getWasteType())
            .receivingSite(e.getReceivingSite())
            .priceOverridden(e.isPriceOverridden())
            .pendingTransferCustomerNumber(e.getPendingTransferCustomerNumber())
            .includeWasteFee(e.isIncludeWasteFee())
            .includeTransportFee(e.isIncludeTransportFee())
            .includeEcoFee(e.isIncludeEcoFee())
            .contractorPaymentStatus(e.getContractorPaymentStatus())
            .validationStatus(e.getValidationStatus())
            .lastValidatedAt(e.getLastValidatedAt())
            .build();
    }

    private BillingEventDetailResponse toDetailResponse(BillingEvent e) {
        String resolvedCostCenter = null;
        try {
            resolvedCostCenter = costCenterCompositionService.compose(e);
        } catch (Exception ignored) {}

        VatCalculationResult vatResult = null;
        try {
            vatResult = vatCalculationService.calculate(e);
        } catch (Exception ignored) {}

        BillingEventDetailResponse.BillingEventDetailResponseBuilder builder = BillingEventDetailResponse.builder()
            .id(e.getId())
            .eventDate(e.getEventDate())
            .product(e.getProduct() != null ? ProductSummaryDto.builder()
                .id(e.getProduct().getId()).code(e.getProduct().getCode())
                .name(e.getProduct().getTranslations().stream()
                    .filter(t -> "en".equals(t.getLocale())).findFirst()
                    .map(t -> t.getName()).orElse(e.getProduct().getCode()))
                .build() : null)
            .wasteFeePrice(e.getWasteFeePrice())
            .transportFeePrice(e.getTransportFeePrice())
            .ecoFeePrice(e.getEcoFeePrice())
            .quantity(e.getQuantity())
            .weight(e.getWeight())
            .vatRate0(e.getVatRate0())
            .vatRate24(e.getVatRate24())
            .vehicleId(e.getVehicleId())
            .driverId(e.getDriverId())
            .customerNumber(e.getCustomerNumber())
            .contractor(e.getContractor())
            .locationId(e.getLocationId())
            .municipalityId(e.getMunicipalityId())
            .sharedServiceGroupId(e.getSharedServiceGroupId())
            .sharedServiceGroupPercentage(e.getSharedServiceGroupPercentage())
            .direction(e.getDirection())
            .comments(e.getComments())
            .internalComments(e.getInternalComments())
            .registrationNumber(e.getRegistrationNumber())
            .status(e.getStatus())
            .excluded(e.isExcluded())
            .exclusionReason(e.getExclusionReason())
            .excludedBy(e.getExcludedBy())
            .excludedAt(e.getExcludedAt())
            .nonBillable(e.isNonBillable())
            .legalClassification(e.getLegalClassification())
            .officeReviewRequired(e.isOfficeReviewRequired())
            .reviewedBy(e.getReviewedBy())
            .reviewedAt(e.getReviewedAt())
            .rejectionReason(e.getRejectionReason())
            .projectId(e.getProjectId())
            .origin(e.getOrigin())
            .accountingAccount(e.getAccountingAccount() != null ? AccountSummaryDto.builder()
                .id(e.getAccountingAccount().getId())
                .code(e.getAccountingAccount().getCode())
                .name(e.getAccountingAccount().getName())
                .build() : null)
            .costCenter(e.getCostCenter() != null ? CostCenterSummaryDto.builder()
                .id(e.getCostCenter().getId())
                .compositeCode(e.getCostCenter().getCompositeCode())
                .build() : null)
            .createdAt(e.getCreatedAt())
            .createdBy(e.getCreatedBy())
            .lastModifiedAt(e.getLastModifiedAt())
            .lastModifiedBy(e.getLastModifiedBy())
            .resolvedCostCenterCode(resolvedCostCenter)
            .wasteType(e.getWasteType())
            .receivingSite(e.getReceivingSite())
            .responsibilityArea(e.getResponsibilityArea())
            .serviceResponsibility(e.getServiceResponsibility())
            .productGroup(e.getProductGroup())
            .transmissionErrorReason(e.getTransmissionErrorReason())
            .priceOverridden(e.isPriceOverridden())
            .originalWasteFeePrice(e.getOriginalWasteFeePrice())
            .originalTransportFeePrice(e.getOriginalTransportFeePrice())
            .originalEcoFeePrice(e.getOriginalEcoFeePrice())
            .pendingTransferCustomerNumber(e.getPendingTransferCustomerNumber())
            .pendingTransferLocationId(e.getPendingTransferLocationId())
            .priorCustomerNumber(e.getPriorCustomerNumber())
            .priorLocationId(e.getPriorLocationId())
            .includeWasteFee(e.isIncludeWasteFee())
            .includeTransportFee(e.isIncludeTransportFee())
            .includeEcoFee(e.isIncludeEcoFee())
            .contractorPaymentStatus(e.getContractorPaymentStatus())
            .contractorPaymentNotes(e.getContractorPaymentNotes())
            .contractorPaymentRecordedBy(e.getContractorPaymentRecordedBy())
            .contractorPaymentRecordedAt(e.getContractorPaymentRecordedAt())
            .validationStatus(e.getValidationStatus())
            .lastValidatedAt(e.getLastValidatedAt())
            .validationOverrideReason(e.getValidationOverrideReason())
            .validationOverriddenBy(e.getValidationOverriddenBy())
            .validationOverriddenAt(e.getValidationOverriddenAt());

        if (vatResult != null) {
            builder
                .resolvedVatRateCode(vatResult.getVatRateCode())
                .resolvedVatRatePercent(vatResult.getEffectiveRatePercent())
                .reverseCharge(vatResult.isReverseCharge())
                .calculatedAmountNet(vatResult.getAmountNet())
                .calculatedAmountVat(vatResult.getAmountVat())
                .calculatedAmountGross(vatResult.getAmountGross())
                .buyerVatNumber(vatResult.getBuyerVatNumber());
        }

        return builder.build();
    }
}
