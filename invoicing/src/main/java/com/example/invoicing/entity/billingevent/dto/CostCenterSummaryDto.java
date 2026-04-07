package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CostCenterSummaryDto {
    private Long id;
    private String compositeCode;
}
