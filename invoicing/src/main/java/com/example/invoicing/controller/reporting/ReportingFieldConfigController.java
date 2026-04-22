package com.example.invoicing.controller.reporting;

import com.example.invoicing.entity.reporting.dto.ReportingFieldConfigDto;
import com.example.invoicing.service.ReportingFieldConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reporting-field-configs")
@RequiredArgsConstructor
public class ReportingFieldConfigController {

    private final ReportingFieldConfigService service;

    @GetMapping
    public List<ReportingFieldConfigDto> list(@RequestParam Long companyId) {
        return service.findAll(companyId);
    }

    @PutMapping
    public ReportingFieldConfigDto upsert(@RequestParam Long companyId,
                                          @RequestBody ReportingFieldConfigDto req) {
        return service.upsert(companyId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
