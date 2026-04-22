package com.example.invoicing.integration;

import lombok.Data;

@Data
public class OperatorStatusResponse {
    private String reference;
    private String status;
    private String message;
}
