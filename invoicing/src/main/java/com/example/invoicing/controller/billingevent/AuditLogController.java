package com.example.invoicing.controller.billingevent;

import com.example.invoicing.entity.billingevent.audit.AuditLogQueryService;
import com.example.invoicing.entity.billingevent.audit.dto.AuditLogEntryResponse;
import com.example.invoicing.entity.reportingaudit.ReportingDataAuditLog;
import com.example.invoicing.repository.ReportingDataAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final ReportingDataAuditLogRepository reportingAuditLogRepository;

    @GetMapping("/user-activity")
    public List<AuditLogEntryResponse> getUserActivity(
        @RequestParam String userId,
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        return auditLogQueryService.getUserActivity(userId, from, to);
    }

    @GetMapping("/by-field")
    public List<AuditLogEntryResponse> getByField(@RequestParam String fieldName) {
        return auditLogQueryService.getChangesByField(fieldName);
    }

    @GetMapping("/reporting-data/{invoiceId}")
    public List<ReportingDataAuditLog> getReportingAuditForInvoice(@PathVariable Long invoiceId) {
        return reportingAuditLogRepository.findByInvoiceIdOrderByLoggedAtDesc(invoiceId);
    }

    @GetMapping("/reporting-data/export")
    public List<ReportingDataAuditLog> exportReportingAudit(
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        return reportingAuditLogRepository.findByPeriod(from, to);
    }
}
