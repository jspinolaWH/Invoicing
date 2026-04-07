package com.example.invoicing.common.exception;

import lombok.*;

import java.util.List;

public class FinvoiceValidationException extends RuntimeException {
    @Getter
    private final List<String> schemaViolations;

    public FinvoiceValidationException(String message) {
        super(message);
        this.schemaViolations = List.of(message);
    }

    public FinvoiceValidationException(List<String> violations) {
        super("FINVOICE validation failed: " + violations);
        this.schemaViolations = violations;
    }
}
