package com.example.invoicing.entity.billingevent.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProductSummaryDto {
    private Long id;
    private String code;
    private String name;
}
