package com.example.invoicing.controller.retroactive;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeResult;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangePreview;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeRequest;
import com.example.invoicing.service.ServiceResponsibilityChangeService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/retroactive/responsibility-change")
@RequiredArgsConstructor
public class ResponsibilityChangeController {

    private final ServiceResponsibilityChangeService service;

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/preview")
    public ResponsibilityChangePreview preview(@RequestBody ResponsibilityChangeRequest request) {
        return service.preview(request);
    }

    @PreAuthorize("hasRole('INVOICING')")
    @PostMapping("/apply")
    public ResponseEntity<ResponsibilityChangeResult> apply(
            @RequestBody ResponsibilityChangeRequest request,
            @AuthenticationPrincipal String currentUser) {
        String actor = currentUser != null ? currentUser : "system";
        return ResponseEntity.ok(service.apply(request, actor));
    }
}
