package com.example.invoicing.controller.billingevent;

import com.example.invoicing.entity.billingevent.audit.AuditLogQueryService;
import com.example.invoicing.entity.billingevent.audit.dto.AuditLogEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

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
}
