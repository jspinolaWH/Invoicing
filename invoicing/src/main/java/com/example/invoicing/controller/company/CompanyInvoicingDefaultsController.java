package com.example.invoicing.controller.company;

import com.example.invoicing.entity.company.CompanyInvoicingDefaults;
import com.example.invoicing.entity.customer.InvoicingMode;
import com.example.invoicing.repository.CompanyInvoicingDefaultsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/company/invoicing-defaults")
@RequiredArgsConstructor
public class CompanyInvoicingDefaultsController {

    private static final Long SINGLETON_ID = 1L;

    private final CompanyInvoicingDefaultsRepository repository;

    @GetMapping
    public CompanyInvoicingDefaultsResponse get() {
        CompanyInvoicingDefaults defaults = repository.findById(SINGLETON_ID)
            .orElseGet(() -> CompanyInvoicingDefaults.builder()
                .id(SINGLETON_ID)
                .defaultInvoicingMode(InvoicingMode.NET)
                .build());
        return new CompanyInvoicingDefaultsResponse(
            defaults.getDefaultInvoicingMode().name(),
            defaults.getUpdatedBy(),
            defaults.getUpdatedAt() != null ? defaults.getUpdatedAt().toString() : null
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('INVOICING')")
    public CompanyInvoicingDefaultsResponse update(
        @RequestBody Map<String, String> body,
        Authentication authentication
    ) {
        String modeStr = body.get("defaultInvoicingMode");
        InvoicingMode mode = InvoicingMode.valueOf(modeStr);
        String user = authentication != null ? authentication.getName() : "system";

        CompanyInvoicingDefaults defaults = repository.findById(SINGLETON_ID)
            .orElseGet(() -> CompanyInvoicingDefaults.builder().id(SINGLETON_ID).build());
        defaults.setDefaultInvoicingMode(mode);
        defaults.setUpdatedBy(user);
        CompanyInvoicingDefaults saved = repository.save(defaults);
        return new CompanyInvoicingDefaultsResponse(
            saved.getDefaultInvoicingMode().name(),
            saved.getUpdatedBy(),
            saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : null
        );
    }

    public record CompanyInvoicingDefaultsResponse(
        String defaultInvoicingMode,
        String updatedBy,
        String updatedAt
    ) {}
}
