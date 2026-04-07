package com.example.invoicing.accounting.costcenter;

import com.example.invoicing.accounting.costcenter.dto.CostCenterCompositionConfigRequest;
import com.example.invoicing.accounting.costcenter.dto.CostCenterCompositionConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cost-center-composition-config")
@RequiredArgsConstructor
public class CostCenterCompositionConfigController {

    private final CostCenterCompositionConfigRepository configRepository;

    @GetMapping
    public CostCenterCompositionConfigResponse get() {
        CostCenterCompositionConfig config = configRepository.findAll().stream().findFirst()
            .orElse(defaultConfig());
        return toResponse(config);
    }

    @PutMapping
    public CostCenterCompositionConfigResponse update(
        @Valid @RequestBody CostCenterCompositionConfigRequest request
    ) {
        CostCenterCompositionConfig config = configRepository.findAll().stream().findFirst()
            .orElse(new CostCenterCompositionConfig());

        config.setSeparator(request.getSeparator());
        config.setSegmentOrder(request.getSegmentOrder());
        config.setProductSegmentEnabled(request.isProductSegmentEnabled());
        config.setReceptionPointSegmentEnabled(request.isReceptionPointSegmentEnabled());
        config.setServiceResponsibilitySegmentEnabled(request.isServiceResponsibilitySegmentEnabled());
        config.setPublicLawCode(request.getPublicLawCode());
        config.setPrivateLawCode(request.getPrivateLawCode());

        return toResponse(configRepository.save(config));
    }

    private CostCenterCompositionConfig defaultConfig() {
        CostCenterCompositionConfig c = new CostCenterCompositionConfig();
        c.setSeparator("-");
        c.setSegmentOrder("PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY");
        c.setProductSegmentEnabled(true);
        c.setReceptionPointSegmentEnabled(true);
        c.setServiceResponsibilitySegmentEnabled(true);
        c.setPublicLawCode("PL");
        c.setPrivateLawCode("PR");
        return c;
    }

    private CostCenterCompositionConfigResponse toResponse(CostCenterCompositionConfig c) {
        return CostCenterCompositionConfigResponse.builder()
            .id(c.getId())
            .separator(c.getSeparator())
            .segmentOrder(c.getSegmentOrder())
            .productSegmentEnabled(c.isProductSegmentEnabled())
            .receptionPointSegmentEnabled(c.isReceptionPointSegmentEnabled())
            .serviceResponsibilitySegmentEnabled(c.isServiceResponsibilitySegmentEnabled())
            .publicLawCode(c.getPublicLawCode())
            .privateLawCode(c.getPrivateLawCode())
            .build();
    }
}
