package com.example.invoicing.entity.invoice.dto;

import lombok.*;

import java.time.Instant;

@Data @Builder
public class SimulationAuditEntry {
    private Instant timestamp;
    private String step;
    private String outcome;
    private String detail;
}
