package com.example.invoicing.controller.billingevent;

import com.example.invoicing.service.DriverEventService;
import com.example.invoicing.entity.driver.dto.RejectEventRequest;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.billingevent.audit.AuditLogQueryService;
import com.example.invoicing.entity.billingevent.audit.dto.AuditLogEntryResponse;
import com.example.invoicing.entity.billingevent.dto.*;
import com.example.invoicing.entity.billingevent.transfer.dto.*;
import com.example.invoicing.entity.validation.ValidationReport;
import com.example.invoicing.service.BillingEventService;
import com.example.invoicing.service.BillingEventStatusService;
import com.example.invoicing.service.BillingEventTransferService;
import com.example.invoicing.service.BillingEventValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-events")
@RequiredArgsConstructor
public class BillingEventController {

    private final BillingEventService billingEventService;
    private final BillingEventStatusService statusService;
    private final BillingEventValidationService validationService;
    private final BillingEventTransferService transferService;
    private final DriverEventService driverEventService;
    private final AuditLogQueryService auditLogQueryService;

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
        Pageable pageable
    ) {
        return billingEventService.findFiltered(
            customerNumber, status, municipalityId, dateFrom, dateTo,
            productId, excluded, requiresReview, pageable);
    }

    @GetMapping("/{id}")
    public BillingEventDetailResponse getById(@PathVariable Long id) {
        return billingEventService.findById(id);
    }

    @PatchMapping("/{id}")
    public BillingEventResponse update(
        @PathVariable Long id,
        @Valid @RequestBody BillingEventUpdateRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return billingEventService.update(id, request, user);
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
    @PostMapping("/{id}/transfer")
    public TransferResult transfer(
        @PathVariable Long id,
        @Valid @RequestBody TransferEventRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.transfer(id, request, user);
    }

    @PostMapping("/bulk-transfer")
    public BulkTransferResult bulkTransfer(
        @Valid @RequestBody BulkTransferRequest request,
        @AuthenticationPrincipal String currentUser
    ) {
        String user = currentUser != null ? currentUser : "system";
        return transferService.bulkTransfer(request, user);
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
}
