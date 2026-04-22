package com.example.invoicing.controller.reports;

import com.example.invoicing.entity.billingevent.retroactive.dto.BillingEventAuditLogResponse;
import com.example.invoicing.entity.billingevent.retroactive.dto.ResponsibilityChangeLogResponse;
import com.example.invoicing.repository.BillingEventAuditLogRepository;
import com.example.invoicing.repository.ServiceResponsibilityChangeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/responsibility-changes")
@RequiredArgsConstructor
public class ResponsibilityChangeReportController {

    private final ServiceResponsibilityChangeLogRepository repository;
    private final BillingEventAuditLogRepository auditLogRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping
    public List<ResponsibilityChangeLogResponse> list(
            @RequestParam(required = false) String customerNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return repository.findFiltered(customerNumber, from, to)
            .stream()
            .map(ResponsibilityChangeLogResponse::from)
            .toList();
    }

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/{changeRunId}/events")
    public ResponseEntity<List<BillingEventAuditLogResponse>> listRunEvents(
            @PathVariable String changeRunId) {
        return repository.findByChangeRunId(changeRunId)
            .map(log -> auditLogRepository
                .findByChangedByAndChangedAtOrderByBillingEventIdAsc(log.getAppliedBy(), log.getAppliedAt())
                .stream()
                .map(BillingEventAuditLogResponse::from)
                .toList())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
