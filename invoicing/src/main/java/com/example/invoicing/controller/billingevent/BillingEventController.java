package com.example.invoicing.controller.billingevent;

import com.example.invoicing.service.DriverEventService;
import com.example.invoicing.entity.driver.dto.RejectEventRequest;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.BillingEventValidationStatus;
import com.example.invoicing.entity.billingevent.audit.AuditLogQueryService;
import com.example.invoicing.entity.billingevent.audit.dto.AuditLogEntryResponse;
import com.example.invoicing.entity.billingevent.dto.*;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferLinkResponse;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferRequest;
import com.example.invoicing.entity.billingevent.credit.dto.CreditTransferResult;
import com.example.invoicing.entity.billingevent.transfer.dto.*;
import com.example.invoicing.entity.validation.BillingEventValidationLog;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.repository.InvoiceLineItemRepository;
import com.example.invoicing.service.BillingEventCreditTransferService;
import com.example.invoicing.service.BillingEventService;
import com.example.invoicing.service.BillingEventStatusService;
import com.example.invoicing.service.BillingEventTransferService;
import com.example.invoicing.service.BillingEventValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing-events")
@RequiredArgsConstructor
public class BillingEventController {

    private final BillingEventService billingEventService;
    private final BillingEventStatusService statusService;
    private final BillingEventValidationService validationService;
    private final BillingEventTransferService transferService;
    private final BillingEventCreditTransferService creditTransferService;
    private final DriverEventService driverEventService;
    private final AuditLogQueryService auditLogQueryService;
    private final InvoiceLineItemRepository invoiceLineItemRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse create(@Valid @RequestBody BillingEventCreateRequest request) {
        return billingEventService.createFromExternalSource(request);
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse createManual(
        @Valid @RequestBody BillingEventManualCreateRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        return billingEventService.createManual(request);
    }

    @PostMapping("/draft")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse createDraft(
        @Valid @RequestBody BillingEventDraftRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        return billingEventService.saveDraft(request);
    }

    @GetMapping("/export")
    public List<BillingEventExportRow> export(
        @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo,
        @RequestParam(required = false) BillingEventStatus status,
        @RequestParam(required = false) String customerNumber,
        @RequestParam(required = false) String municipalityId
    ) {
        return billingEventService.exportEvents(dateFrom, dateTo, status, customerNumber, municipalityId);
    }

    @GetMapping
    public Page<BillingEventResponse> list(
        @RequestParam(required = false) String customerNumber,
        @RequestParam(required = false) BillingEventStatus status,
        @RequestParam(required = false) String municipalityId,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Boolean excluded,
        @RequestParam(required = false) Boolean requiresReview,
        @RequestParam(required = false) String serviceResponsibility,
        @RequestParam(required = false) BillingEventValidationStatus validationStatus,
        @RequestParam(required = false) String origin,
        Pageable pageable
    ) {
        return billingEventService.findFiltered(
            customerNumber, status, municipalityId, dateFrom, dateTo,
            productId, excluded, requiresReview, serviceResponsibility, validationStatus, origin, pageable);
    }

    @GetMapping("/{id}")
    public BillingEventDetailResponse getById(@PathVariable Long id) {
        return billingEventService.findById(id);
    }

    @PreAuthorize("hasAnyRole('INVOICING', 'INVOICING_PRICING')")
    @PatchMapping("/{id}")
    public BillingEventResponse update(
        @PathVariable Long id,
        @Valid @RequestBody BillingEventUpdateRequest request,
        @AuthenticationPrincipal String currentUser,
        Authentication authentication
    ) {
        String user = currentUser != null ? currentUser : "system";
        boolean pricingOnly = !hasRole(authentication, "INVOICING");
        return billingEventService.update(id, request, user, pricingOnly);
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/{id}/approve-correction")
    public BillingEventResponse approveCorrection(
        @PathVariable Long id,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.approveCorrection(id, user);
    }

    @PostMapping("/{id}/transition")
    public BillingEventDetailResponse transition(
        @PathVariable Long id,
        @Valid @RequestBody TransitionRequest request
    ) {
        statusService.transitionTo(id, request.getTargetStatus());
        return billingEventService.findById(id);
    }

    // -----------------------------------------------------------------------
    // SELECTIVE COMPONENT INVOICING (AC3)
    // -----------------------------------------------------------------------
    @PatchMapping("/{id}/components")
    public BillingEventDetailResponse updateComponents(
        @PathVariable Long id,
        @RequestBody SelectiveComponentRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.updateComponentInclusion(id, request, user);
    }

    // -----------------------------------------------------------------------
    // CONTRACTOR PAYMENT (AC5)
    // -----------------------------------------------------------------------
    @PostMapping("/{id}/contractor-payment")
    public BillingEventDetailResponse recordContractorPayment(
        @PathVariable Long id,
        @Valid @RequestBody ContractorPaymentRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.recordContractorPayment(id, request, user);
    }

    // -----------------------------------------------------------------------
    // EXCLUSION
    // -----------------------------------------------------------------------
    @PostMapping("/{id}/exclude")
    public BillingEventResponse exclude(
        @PathVariable Long id,
        @Valid @RequestBody ExcludeEventRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.exclude(id, request.getExclusionReason(), user);
    }

    @PostMapping("/{id}/reinstate")
    public BillingEventResponse reinstate(
        @PathVariable Long id,
        @Valid @RequestBody ReinstateEventRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.reinstate(id, request.getReason(), user);
    }

    @PostMapping("/bulk-exclude")
    public BulkExcludeResult bulkExclude(
        @Valid @RequestBody BulkExcludeRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.bulkExclude(request.getEventIds(), request.getExclusionReason(), user);
    }

    // -----------------------------------------------------------------------
    // TRANSFER
    // -----------------------------------------------------------------------
    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/{id}/transfer")
    public TransferResult transfer(
        @PathVariable Long id,
        @Valid @RequestBody TransferEventRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.transfer(id, request, user);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/bulk-transfer")
    public BulkTransferResult bulkTransfer(
        @Valid @RequestBody BulkTransferRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.bulkTransfer(request, user);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/{id}/transfer/confirm")
    public TransferResult confirmTransfer(
        @PathVariable Long id,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.confirmTransfer(id, user);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/{id}/transfer/cancel")
    public TransferResult cancelTransfer(
        @PathVariable Long id,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.cancelTransfer(id, user);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/bulk-transfer/confirm")
    public BulkTransferResult bulkConfirmTransfer(
        @Valid @RequestBody BulkActionRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.bulkConfirmTransfer(request.getEventIds(), user);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/bulk-transfer/cancel")
    public BulkTransferResult bulkCancelTransfer(
        @Valid @RequestBody BulkActionRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.bulkCancelTransfer(request.getEventIds(), user);
    }

    // -----------------------------------------------------------------------
    // CREDIT & TRANSFER (SENT / COMPLETED events)
    // -----------------------------------------------------------------------
    @PostMapping("/{id}/credit-transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditTransferResult creditTransfer(
        @PathVariable Long id,
        @Valid @RequestBody CreditTransferRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return creditTransferService.creditAndTransfer(id, request, user);
    }

    @GetMapping("/{id}/credit-transfer")
    public CreditTransferLinkResponse getCreditTransferLink(@PathVariable Long id) {
        return creditTransferService.findLink(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "No credit-transfer link found for event " + id));
    }

    // -----------------------------------------------------------------------
    // OFFICE REVIEW
    // -----------------------------------------------------------------------
    @GetMapping("/pending-review")
    public List<BillingEventResponse> getPendingReview() {
        return billingEventService.findPendingReview();
    }

    @PostMapping("/{id}/approve")
    public BillingEventResponse approve(
        @PathVariable Long id,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return driverEventService.approveReview(id, user);
    }

    @PostMapping("/{id}/reject")
    public BillingEventResponse reject(
        @PathVariable Long id,
        @Valid @RequestBody RejectEventRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return driverEventService.rejectReview(id, request.getRejectionReason(), user);
    }

    // -----------------------------------------------------------------------
    // AUDIT LOG
    // -----------------------------------------------------------------------
    @GetMapping("/{id}/audit-log")
    public List<AuditLogEntryResponse> getAuditLog(@PathVariable Long id) {
        return auditLogQueryService.getChangeHistoryForEvent(id);
    }

    @PostMapping("/validate")
    public ValidationReport validate(@Valid @RequestBody ValidateEventsRequest request) {
        return validationService.validate(request.getEventIds());
    }

    @GetMapping("/{id}/validation-failures")
    public List<BillingEventValidationLog> getValidationFailures(@PathVariable Long id) {
        return validationService.getValidationFailures(id);
    }

    // -----------------------------------------------------------------------
    // VALIDATION OVERRIDE (AC 18 — PD-278)
    // -----------------------------------------------------------------------
    @PostMapping("/{id}/validation-override")
    public BillingEventDetailResponse overrideValidation(
        @PathVariable Long id,
        @Valid @RequestBody ValidationOverrideRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.overrideValidation(id, request.getReason(), user);
    }

    // -----------------------------------------------------------------------
    // PARENT INVOICE LOOKUP (PD-299 AC5)
    // -----------------------------------------------------------------------
    @GetMapping("/{id}/invoice")
    public ResponseEntity<Map<String, Long>> getParentInvoice(@PathVariable Long id) {
        return invoiceLineItemRepository.findInvoiceIdBySourceEventId(id)
            .map(invoiceId -> ResponseEntity.ok(Map.of("invoiceId", invoiceId)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
