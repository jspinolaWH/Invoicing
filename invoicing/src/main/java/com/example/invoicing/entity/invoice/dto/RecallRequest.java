package com.example.invoicing.entity.invoice.dto;

import lombok.Data;

@Data
public class RecallRequest {
    private String reason;
    private String internalComment;
}
