package com.example.invoicing.accounting.costcenter;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CostCenterCompositionService {

    private final CostCenterCompositionConfigRepository configRepository;

    public String compose(BillingEvent event) {
        CostCenterCompositionConfig config = getConfig();
        String[] order = config.getSegmentOrder().split(",");

        List<String> segments = new ArrayList<>();
        for (String segmentName : order) {
            String value = switch (segmentName.trim()) {
                case "PRODUCT"               -> resolveProductSegment(event, config);
                case "RECEPTION_POINT"       -> resolveReceptionPointSegment(event, config);
                case "SERVICE_RESPONSIBILITY" -> resolveServiceResponsibilitySegment(event, config);
                default -> "";
            };
            if (value != null && !value.isBlank()) {
                segments.add(value);
            }
        }
        return String.join(config.getSeparator(), segments);
    }

    public CostCenterCompositionConfig getConfig() {
        return configRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "CostCenterCompositionConfig not found. Configure at /api/v1/cost-center-composition-config."));
    }

    private String resolveProductSegment(BillingEvent event, CostCenterCompositionConfig config) {
        if (!config.isProductSegmentEnabled()) return "";
        CostCenter cc = event.getCostCenter();
        if (cc == null) return "";
        return cc.getProductSegment() != null ? cc.getProductSegment() : "";
    }

    private String resolveReceptionPointSegment(BillingEvent event, CostCenterCompositionConfig config) {
        if (!config.isReceptionPointSegmentEnabled()) return "";
        CostCenter cc = event.getCostCenter();
        if (cc == null) return "";
        return cc.getReceptionSegment() != null ? cc.getReceptionSegment() : "";
    }

    private String resolveServiceResponsibilitySegment(BillingEvent event, CostCenterCompositionConfig config) {
        if (!config.isServiceResponsibilitySegmentEnabled()) return "";
        if (event.getLegalClassification() == LegalClassification.PUBLIC_LAW) {
            return config.getPublicLawCode() != null ? config.getPublicLawCode() : "PL";
        }
        return config.getPrivateLawCode() != null ? config.getPrivateLawCode() : "PR";
    }
}
