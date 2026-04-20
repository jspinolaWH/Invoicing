package com.example.invoicing.entity.contract.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ContractSummaryDto {
    private Long id;
    private String name;
    private String customerNumber;
    private boolean active;
}
