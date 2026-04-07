package com.example.invoicing.entity.costcenter;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cost_center_composition_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterCompositionConfig extends BaseAuditEntity {

    @Column(nullable = false, length = 5)
    private String separator = "-";

    @Column(nullable = false, length = 50)
    private String segmentOrder = "PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY";

    @Column(nullable = false)
    private boolean productSegmentEnabled = true;

    @Column(nullable = false)
    private boolean receptionPointSegmentEnabled = true;

    @Column(nullable = false)
    private boolean serviceResponsibilitySegmentEnabled = true;

    @Column(length = 10)
    private String publicLawCode = "PL";

    @Column(length = 10)
    private String privateLawCode = "PR";
}
