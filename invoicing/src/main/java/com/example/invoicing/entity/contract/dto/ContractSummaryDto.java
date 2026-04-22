package com.example.invoicing.entity.contract.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class ContractSummaryDto {
    private Long id;
    private String name;
    private String customerNumber;
    private boolean active;
    private Long invoiceTemplateId;
    private String workSite;
    private LocalDate startDate;
    private LocalDate endDate;
}
