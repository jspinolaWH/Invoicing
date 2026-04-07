package com.example.invoicing.controller.retroactive;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeResult;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangePreview;
import com.example.invoicing.entity.billingevent.dto.ResponsibilityChangeRequest;
import com.example.invoicing.service.ServiceResponsibilityChangeService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/retroactive/responsibility-change")
@RequiredArgsConstructor
public class ResponsibilityChangeController {

    private final ServiceResponsibilityChangeService service;

    @PostMapping("/preview")
    public ResponsibilityChangePreview preview(@RequestBody ResponsibilityChangeRequest request) {
        return service.preview(request);
    }

    @PostMapping("/apply")
    public ResponseEntity<ResponsibilityChangeResult> apply(@RequestBody ResponsibilityChangeRequest request) {
        return ResponseEntity.ok(service.apply(request));
    }
}
